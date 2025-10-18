package dev.barreto.fleetctrl.data.migration

import android.content.Context
import android.util.Log
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import dev.barreto.fleetctrl.data.preferences.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gerenciador de migração de dados para futuras versões
 */
@Singleton
class DataMigrationManager @Inject constructor(
    private val context: Context,
    private val appPreferences: AppPreferences
) {
    private val currentDataVersion = 1
    private val migrationKey = "data_version"

    /**
     * Executa migrações necessárias baseadas na versão atual dos dados
     */
    suspend fun migrateIfNeeded(): MigrationResult = withContext(Dispatchers.IO) {
        try {
            val currentVersion = getCurrentDataVersion()
            Log.d("DataMigration", "Versão atual dos dados: $currentVersion")
            Log.d("DataMigration", "Versão esperada: $currentDataVersion")

            if (currentVersion < currentDataVersion) {
                Log.i("DataMigration", "Iniciando migração de dados...")
                val result = performMigration(currentVersion, currentDataVersion)
                if (result is MigrationResult.Success) {
                    setCurrentDataVersion(currentDataVersion)
                    Log.i("DataMigration", "Migração concluída com sucesso")
                }
                result
            } else {
                Log.d("DataMigration", "Dados já estão na versão mais recente")
                MigrationResult.Success
            }
        } catch (e: Exception) {
            Log.e("DataMigration", "Erro durante migração", e)
            MigrationResult.Error("Erro durante migração: ${e.message}")
        }
    }

    /**
     * Obtém a versão atual dos dados
     */
    private suspend fun getCurrentDataVersion(): Int {
        return try {
            // Em uma implementação real, isso viria do banco de dados ou SharedPreferences
            appPreferences.getDataVersion() ?: 0
        } catch (e: Exception) {
            Log.w("DataMigration", "Erro ao obter versão dos dados, assumindo versão 0", e)
            0
        }
    }

    /**
     * Define a versão atual dos dados
     */
    private suspend fun setCurrentDataVersion(version: Int) {
        try {
            appPreferences.setDataVersion(version)
        } catch (e: Exception) {
            Log.e("DataMigration", "Erro ao definir versão dos dados", e)
        }
    }

    /**
     * Executa as migrações necessárias
     */
    private suspend fun performMigration(fromVersion: Int, toVersion: Int): MigrationResult {
        val migrations = getMigrations()
        var currentVersion = fromVersion

        for (migration in migrations) {
            if (migration.fromVersion == currentVersion && migration.toVersion <= toVersion) {
                Log.d("DataMigration", "Executando migração: ${migration.fromVersion} → ${migration.toVersion}")
                
                when (val result = migration.migrate()) {
                    is MigrationResult.Success -> {
                        currentVersion = migration.toVersion
                        Log.d("DataMigration", "Migração ${migration.fromVersion} → ${migration.toVersion} concluída")
                    }
                    is MigrationResult.Error -> {
                        Log.e("DataMigration", "Erro na migração ${migration.fromVersion} → ${migration.toVersion}: ${result.message}")
                        return result
                    }
                }
            }
        }

        return MigrationResult.Success
    }

    /**
     * Obtém todas as migrações disponíveis
     */
    private fun getMigrations(): List<DataMigration> {
        return listOf(
            // Migração da versão 0 para 1
            DataMigration(
                fromVersion = 0,
                toVersion = 1,
                description = "Migração inicial - adiciona campos de segurança e validação",
                migrate = { migrateToVersion1() }
            ),
            // Futuras migrações podem ser adicionadas aqui
            // DataMigration(
            //     fromVersion = 1,
            //     toVersion = 2,
            //     description = "Adiciona novos campos de manutenção",
            //     migrate = { migrateToVersion2() }
            // )
        )
    }

    /**
     * Migração para a versão 1
     */
    private suspend fun migrateToVersion1(): MigrationResult = withContext(Dispatchers.IO) {
        try {
            Log.d("DataMigration", "Executando migração para versão 1...")
            
            // Aqui você implementaria as mudanças específicas da versão 1
            // Por exemplo:
            // - Adicionar novos campos ao banco de dados
            // - Converter dados existentes para novo formato
            // - Atualizar configurações
            // - Validar integridade dos dados existentes
            
            // Exemplo de validação de dados existentes
            validateExistingData()
            
            Log.d("DataMigration", "Migração para versão 1 concluída")
            MigrationResult.Success
        } catch (e: Exception) {
            Log.e("DataMigration", "Erro na migração para versão 1", e)
            MigrationResult.Error("Erro na migração para versão 1: ${e.message}")
        }
    }

    /**
     * Valida dados existentes após migração
     */
    private suspend fun validateExistingData() {
        try {
            // Aqui você implementaria validação dos dados existentes
            // Por exemplo, verificar se todos os veículos têm campos obrigatórios
            Log.d("DataMigration", "Validando dados existentes...")
            
            // Implementar validação específica aqui
            
            Log.d("DataMigration", "Validação de dados concluída")
        } catch (e: Exception) {
            Log.e("DataMigration", "Erro na validação de dados", e)
            throw e
        }
    }

    /**
     * Força uma migração completa (útil para desenvolvimento)
     */
    suspend fun forceMigration(): MigrationResult = withContext(Dispatchers.IO) {
        try {
            Log.i("DataMigration", "Forçando migração completa...")
            setCurrentDataVersion(0)
            migrateIfNeeded()
        } catch (e: Exception) {
            Log.e("DataMigration", "Erro na migração forçada", e)
            MigrationResult.Error("Erro na migração forçada: ${e.message}")
        }
    }

    /**
     * Verifica se há migrações pendentes
     */
    suspend fun hasPendingMigrations(): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentVersion = getCurrentDataVersion()
            currentVersion < currentDataVersion
        } catch (e: Exception) {
            Log.e("DataMigration", "Erro ao verificar migrações pendentes", e)
            false
        }
    }

    /**
     * Obtém informações sobre migrações disponíveis
     */
    suspend fun getMigrationInfo(): MigrationInfo = withContext(Dispatchers.IO) {
        try {
            val currentVersion = getCurrentDataVersion()
            val migrations = getMigrations()
            val pendingMigrations = migrations.filter { it.fromVersion >= currentVersion }
            
            MigrationInfo(
                currentVersion = currentVersion,
                targetVersion = currentDataVersion,
                pendingMigrations = pendingMigrations.size,
                migrations = migrations
            )
        } catch (e: Exception) {
            Log.e("DataMigration", "Erro ao obter informações de migração", e)
            MigrationInfo(0, 0, 0, emptyList())
        }
    }
}

/**
 * Representa uma migração de dados
 */
data class DataMigration(
    val fromVersion: Int,
    val toVersion: Int,
    val description: String,
    val migrate: suspend () -> MigrationResult
)

/**
 * Informações sobre migrações
 */
data class MigrationInfo(
    val currentVersion: Int,
    val targetVersion: Int,
    val pendingMigrations: Int,
    val migrations: List<DataMigration>
)

/**
 * Resultado de uma operação de migração
 */
sealed class MigrationResult {
    object Success : MigrationResult()
    data class Error(val message: String) : MigrationResult()
}
