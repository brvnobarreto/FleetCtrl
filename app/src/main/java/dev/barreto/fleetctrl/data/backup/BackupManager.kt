package dev.barreto.fleetctrl.data.backup

import android.content.Context
import android.util.Log
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gerenciador de backup automático com versionamento
 */
@Singleton
class BackupManager @Inject constructor(
    private val context: Context
) {
    private val backupDir = File(context.filesDir, "backups")
    private val maxBackups = 10 // Manter apenas os últimos 10 backups
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    init {
        createBackupDirectory()
    }

    /**
     * Cria o diretório de backup se não existir
     */
    private fun createBackupDirectory() {
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
    }

    /**
     * Cria um backup completo dos dados
     */
    suspend fun createBackup(vehicles: List<Vehicle>): BackupResult = withContext(Dispatchers.IO) {
        try {
            val timestamp = dateFormat.format(Date())
            val backupFile = File(backupDir, "fleet_backup_$timestamp.json")
            
            val backupData = BackupData(
                version = 1,
                timestamp = timestamp,
                vehicles = vehicles,
                checksum = calculateChecksum(vehicles)
            )

            FileWriter(backupFile).use { writer ->
                writer.write(backupData.toJson())
            }

            // Limpa backups antigos
            cleanupOldBackups()

            Log.d("BackupManager", "Backup criado: ${backupFile.name}")
            BackupResult.Success(backupFile.absolutePath)
        } catch (e: Exception) {
            Log.e("BackupManager", "Erro ao criar backup", e)
            BackupResult.Error(e.message ?: "Erro desconhecido")
        }
    }

    /**
     * Restaura dados de um backup
     */
    suspend fun restoreBackup(backupPath: String): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val backupFile = File(backupPath)
            if (!backupFile.exists()) {
                return@withContext RestoreResult.Error("Arquivo de backup não encontrado")
            }

            val backupJson = backupFile.readText()
            val backupData = BackupData.fromJson(backupJson)

            // Valida checksum
            val currentChecksum = calculateChecksum(backupData.vehicles)
            if (currentChecksum != backupData.checksum) {
                return@withContext RestoreResult.Error("Backup corrompido - checksum inválido")
            }

            Log.d("BackupManager", "Backup restaurado: ${backupFile.name}")
            RestoreResult.Success(backupData.vehicles)
        } catch (e: Exception) {
            Log.e("BackupManager", "Erro ao restaurar backup", e)
            RestoreResult.Error(e.message ?: "Erro desconhecido")
        }
    }

    /**
     * Lista todos os backups disponíveis
     */
    suspend fun listBackups(): List<BackupInfo> = withContext(Dispatchers.IO) {
        backupDir.listFiles()
            ?.filter { it.name.startsWith("fleet_backup_") && it.name.endsWith(".json") }
            ?.map { file ->
                BackupInfo(
                    path = file.absolutePath,
                    name = file.name,
                    size = file.length(),
                    created = Date(file.lastModified())
                )
            }
            ?.sortedByDescending { it.created }
            ?: emptyList()
    }

    /**
     * Calcula checksum dos dados para verificação de integridade
     */
    private fun calculateChecksum(vehicles: List<Vehicle>): String {
        val data = vehicles.joinToString("|") { "${it.id}-${it.vehicleNumber}-${it.plate}" }
        return data.hashCode().toString()
    }

    /**
     * Remove backups antigos mantendo apenas os mais recentes
     */
    private fun cleanupOldBackups() {
        val backups = backupDir.listFiles()
            ?.filter { it.name.startsWith("fleet_backup_") && it.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?: return

        if (backups.size > maxBackups) {
            val toDelete = backups.drop(maxBackups)
            toDelete.forEach { file ->
                try {
                    file.delete()
                    Log.d("BackupManager", "Backup antigo removido: ${file.name}")
                } catch (e: Exception) {
                    Log.e("BackupManager", "Erro ao remover backup antigo", e)
                }
            }
        }
    }

    /**
     * Verifica integridade de um backup
     */
    suspend fun verifyBackup(backupPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val backupFile = File(backupPath)
            val backupJson = backupFile.readText()
            val backupData = BackupData.fromJson(backupJson)
            
            val currentChecksum = calculateChecksum(backupData.vehicles)
            currentChecksum == backupData.checksum
        } catch (e: Exception) {
            Log.e("BackupManager", "Erro ao verificar backup", e)
            false
        }
    }
}

/**
 * Dados de backup com versionamento
 */
data class BackupData(
    val version: Int,
    val timestamp: String,
    val vehicles: List<Vehicle>,
    val checksum: String
) {
    fun toJson(): String {
        return """
        {
            "version": $version,
            "timestamp": "$timestamp",
            "vehicles": [${vehicles.joinToString(",") { vehicleToJson(it) }}],
            "checksum": "$checksum"
        }
        """.trimIndent()
    }

    companion object {
        fun fromJson(json: String): BackupData {
            // Implementação simplificada - em produção usar Gson ou similar
            val vehicles = mutableListOf<Vehicle>()
            // Parse JSON aqui (simplificado para exemplo)
            return BackupData(1, "", vehicles, "")
        }
    }
}

/**
 * Converte Vehicle para JSON (implementação simplificada)
 */
private fun vehicleToJson(vehicle: Vehicle): String {
    return """
    {
        "id": ${vehicle.id},
        "vehicleNumber": "${vehicle.vehicleNumber}",
        "plate": "${vehicle.plate}",
        "model": "${vehicle.model}",
        "brand": "${vehicle.brand}",
        "year": ${vehicle.year},
        "color": "${vehicle.color}",
        "driver": "${vehicle.driver}",
        "showInDiary": ${vehicle.showInDiary},
        "photoPath": "${vehicle.photoPath ?: ""}",
        "engineType": "${vehicle.engineType}",
        "fuelCapacity": ${vehicle.fuelCapacity},
        "averageConsumption": ${vehicle.averageConsumption},
        "isActive": ${vehicle.isActive},
        "currentMileage": ${vehicle.currentMileage},
        "lastMaintenanceMileage": ${vehicle.lastMaintenanceMileage},
        "createdAt": "${vehicle.createdAt}",
        "updatedAt": "${vehicle.updatedAt}",
        "notes": "${vehicle.notes ?: ""}"
    }
    """.trimIndent()
}

/**
 * Informações de um backup
 */
data class BackupInfo(
    val path: String,
    val name: String,
    val size: Long,
    val created: Date
)

/**
 * Resultado de operação de backup
 */
sealed class BackupResult {
    data class Success(val path: String) : BackupResult()
    data class Error(val message: String) : BackupResult()
}

/**
 * Resultado de operação de restauração
 */
sealed class RestoreResult {
    data class Success(val vehicles: List<Vehicle>) : RestoreResult()
    data class Error(val message: String) : RestoreResult()
}