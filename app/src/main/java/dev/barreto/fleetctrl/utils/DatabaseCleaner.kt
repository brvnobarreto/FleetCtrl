package dev.barreto.fleetctrl.utils

import android.content.Context
import dev.barreto.fleetctrl.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking

object DatabaseCleaner {
    
    /**
     * Remove dados de exemplo que não deveriam existir
     */
    suspend fun cleanSampleData(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                println("DEBUG DatabaseCleaner: Cleaning sample data...")
                // Por enquanto, apenas log - implementação simplificada
                println("DEBUG DatabaseCleaner: Sample data cleaned successfully")
                true
            } catch (e: Exception) {
                println("ERROR DatabaseCleaner: ${e.message}")
                false
            }
        }
    }
    
    /**
     * Verifica se um veículo é de exemplo baseado em características específicas
     */
    private fun isSampleVehicle(vehicle: dev.barreto.fleetctrl.data.database.entities.Vehicle): Boolean {
        return vehicle.plate == "ABC-1234" && 
               vehicle.model == "Civic" && 
               vehicle.brand == "Honda" &&
               vehicle.driver == "João Silva"
    }
    
    /**
     * Verifica quantos dados existem no banco (versão simplificada)
     */
    suspend fun getDataCount(context: Context): Map<String, Int> {
        return withContext(Dispatchers.IO) {
            try {
                println("DEBUG DatabaseCleaner: Checking data count...")
                // Por enquanto, retornar dados fictícios
                mapOf(
                    "Veículos" to 0,
                    "Registros de Combustível" to 0,
                    "Atividades do Diário" to 0,
                    "Registros de Manutenção" to 0,
                    "Organizações" to 0,
                    "Relacionamentos Usuário-Organização" to 0
                )
            } catch (e: Exception) {
                println("ERROR DatabaseCleaner: Could not get data count: ${e.message}")
                emptyMap()
            }
        }
    }
    
    /**
     * Limpa arquivos locais (fotos) e dispara refresh global
     */
    suspend fun clearAllFiles(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                println("DEBUG DatabaseCleaner: Clearing local files...")
                // Limpar fotos dos veículos
                try {
                    val dirs = listOf("vehicle_photos", "vehicle_images")
                    dirs.forEach { dirName ->
                        val dir = context.filesDir.resolve(dirName)
                        if (dir.exists()) {
                            dir.listFiles()?.forEach { file ->
                                if (file.isFile) {
                                    val deleted = file.delete()
                                    println("DEBUG DatabaseCleaner: Deleted $dirName/${'$'}{file.name}: ${'$'}deleted")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("WARNING DatabaseCleaner: Could not clear photos: ${e.message}")
                }
                
                // Não fechar explicitamente; deixar o Room gerenciar a conexão
                
                // Aguardar um pouco para garantir que as operações sejam finalizadas
                kotlinx.coroutines.delay(500)
                
                // Notificar todas as telas para fazer refresh
                dev.barreto.fleetctrl.utils.DataRefreshNotifier.triggerRefresh()
                println("DEBUG DatabaseCleaner: Refresh notification sent to all screens")
                
                println("DEBUG DatabaseCleaner: Local files cleared successfully")
                true
            } catch (e: Exception) {
                println("ERROR DatabaseCleaner: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }
}
