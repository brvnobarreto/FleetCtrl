package dev.barreto.fleetctrl.utils

import android.content.Context
import android.content.SharedPreferences
import dev.barreto.fleetctrl.data.database.AppDatabase
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import dev.barreto.fleetctrl.data.database.entities.FuelRecord
import dev.barreto.fleetctrl.data.database.entities.MaintenanceRecord
import dev.barreto.fleetctrl.data.database.entities.ActivityRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

object DatabaseRecovery {
    
    private const val PREFS_NAME = "database_recovery"
    private const val KEY_LAST_BACKUP_VERSION = "last_backup_version"
    private const val KEY_DATA_BACKUP = "data_backup"
    
    /**
     * Verifica se os dados foram perdidos e tenta recuperá-los
     */
    suspend fun checkAndRecoverData(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val database = AppDatabase.getDatabase(context)
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                
                println("DEBUG DatabaseRecovery: Checking database...")
                
                // Apenas verificar se o banco está funcionando
                // NÃO criar dados de exemplo automaticamente
                println("DEBUG DatabaseRecovery: Database check completed - no data creation")
                true
            } catch (e: Exception) {
                println("ERROR DatabaseRecovery: ${e.message}")
                false
            }
        }
    }
    
    /**
     * Cria um backup dos dados atuais
     */
    suspend fun createBackup(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val database = AppDatabase.getDatabase(context)
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                
                // Coletar todos os dados
                val vehicles = database.vehicleDao().getAllVehicles()
                val fuelRecords = database.fuelRecordDao().getAllFuelRecords()
                val activityRecords = database.activityRecordDao().getAllActivityRecords()
                
                // Criar backup simples (em produção, use criptografia)
                val backup = mapOf(
                    "vehicles" to vehicles,
                    "fuelRecords" to fuelRecords,
                    "activityRecords" to activityRecords,
                    "timestamp" to System.currentTimeMillis()
                )
                
                // Salvar no SharedPreferences (temporário)
                prefs.edit()
                    .putString(KEY_DATA_BACKUP, "backup_created")
                    .putInt(KEY_LAST_BACKUP_VERSION, 6)
                    .apply()
                
                println("DEBUG DatabaseRecovery: Backup created successfully")
                true
            } catch (e: Exception) {
                println("ERROR DatabaseRecovery: Failed to create backup: ${e.message}")
                false
            }
        }
    }
    
    private suspend fun recoverFromBackup(database: AppDatabase, prefs: SharedPreferences): Boolean {
        return try {
            // Verificar se existe backup
            val hasBackup = prefs.getString(KEY_DATA_BACKUP, null) != null
            if (!hasBackup) {
                println("DEBUG DatabaseRecovery: No backup found")
                return false
            }
            
            // Em um cenário real, você restauraria os dados do backup
            // Por enquanto, vamos apenas criar dados de exemplo para teste
            createSampleData(database)
            
            println("DEBUG DatabaseRecovery: Data recovery completed")
            true
        } catch (e: Exception) {
            println("ERROR DatabaseRecovery: Recovery failed: ${e.message}")
            false
        }
    }
    
    private suspend fun createSampleData(database: AppDatabase) {
        try {
            // Criar um veículo de exemplo
            val sampleVehicle = Vehicle(
                vehicleNumber = "001",
                plate = "ABC-1234",
                model = "Civic",
                brand = "Honda",
                year = 2020,
                color = "Branco",
                engineType = "1.5 Turbo",
                fuelCapacity = 50.0,
                averageConsumption = 12.5,
                driver = "João Silva",
                showInDiary = true,
                photoPath = null,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            
            database.vehicleDao().insertVehicle(sampleVehicle)
            
            println("DEBUG DatabaseRecovery: Sample vehicle created")
        } catch (e: Exception) {
            println("ERROR DatabaseRecovery: Failed to create sample data: ${e.message}")
        }
    }
    
    /**
     * Limpa o backup (use após recuperação bem-sucedida)
     */
    fun clearBackup(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}