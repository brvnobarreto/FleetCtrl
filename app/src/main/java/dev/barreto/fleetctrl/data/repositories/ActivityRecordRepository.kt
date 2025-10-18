package dev.barreto.fleetctrl.data.repositories

import dev.barreto.fleetctrl.data.database.daos.ActivityRecordDao
import dev.barreto.fleetctrl.data.database.entities.ActivityRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository para operações com registros de atividade
 * Implementa o padrão Repository para abstrair o acesso aos dados
 */
@Singleton
class ActivityRecordRepository @Inject constructor(
    private val activityRecordDao: ActivityRecordDao,
    private val syncRepository: SyncRepository
) {
    
    // ===== OPERAÇÕES BÁSICAS =====
    
    fun getAllActivityRecords(): Flow<List<ActivityRecord>> = 
        activityRecordDao.getAllActivityRecords()
            .catch { exception ->
                println("Erro ao carregar registros de atividade: ${exception.message}")
                emit(emptyList())
            }
    
    fun getActivityRecordsByVehicle(vehicleId: Long): Flow<List<ActivityRecord>> = 
        activityRecordDao.getActivityRecordsByVehicle(vehicleId)
            .catch { exception ->
                println("Erro ao carregar registros do veículo $vehicleId: ${exception.message}")
                emit(emptyList())
            }
    
    fun getActivityRecordsByPlate(plate: String): Flow<List<ActivityRecord>> = 
        activityRecordDao.getActivityRecordsByPlate(plate)
            .catch { exception ->
                println("Erro ao carregar registros da placa $plate: ${exception.message}")
                emit(emptyList())
            }
    
    suspend fun getActivityRecordById(id: Long): ActivityRecord? = try {
        activityRecordDao.getActivityRecordById(id)
    } catch (e: Exception) {
        println("Erro ao buscar registro por ID $id: ${e.message}")
        null
    }
    
    fun getActivityRecordsByDriver(driver: String): Flow<List<ActivityRecord>> = 
        activityRecordDao.getActivityRecordsByDriver(driver)
            .catch { exception ->
                println("Erro ao carregar registros do condutor $driver: ${exception.message}")
                emit(emptyList())
            }
    
    // ===== INSERÇÃO E ATUALIZAÇÃO =====
    
    suspend fun insertActivityRecord(activityRecord: ActivityRecord): Result<Long> = try {
        val id = activityRecordDao.insertActivityRecord(activityRecord)
        val saved = activityRecordDao.getActivityRecordById(id)
        if (saved?.organizationId?.isNotBlank() == true) {
            syncRepository.uploadActivityRecord(saved)
        }
        Result.success(id)
    } catch (e: Exception) {
        println("Erro ao inserir registro de atividade: ${e.message}")
        Result.failure(e)
    }
    
    suspend fun insertActivityRecords(activityRecords: List<ActivityRecord>): Result<Unit> = try {
        activityRecordDao.insertActivityRecords(activityRecords)
        activityRecords.filter { !it.organizationId.isNullOrBlank() }.forEach { syncRepository.uploadActivityRecord(it) }
        Result.success(Unit)
    } catch (e: Exception) {
        println("Erro ao inserir registros de atividade: ${e.message}")
        Result.failure(e)
    }
    
    suspend fun updateActivityRecord(activityRecord: ActivityRecord): Result<Unit> = try {
        activityRecordDao.updateActivityRecord(activityRecord)
        if (activityRecord.organizationId?.isNotBlank() == true) {
            syncRepository.uploadActivityRecord(activityRecord)
        }
        Result.success(Unit)
    } catch (e: Exception) {
        println("Erro ao atualizar registro de atividade: ${e.message}")
        Result.failure(e)
    }
    
    // ===== EXCLUSÃO =====
    
    suspend fun deleteActivityRecord(activityRecord: ActivityRecord): Result<Unit> = try {
        if (activityRecord.organizationId?.isNotBlank() == true) {
            syncRepository.deleteActivityRecord(activityRecord)
        }
        activityRecordDao.deleteActivityRecord(activityRecord)
        Result.success(Unit)
    } catch (e: Exception) {
        println("Erro ao excluir registro de atividade: ${e.message}")
        Result.failure(e)
    }
    
    suspend fun deleteActivityRecordById(id: Long): Result<Unit> = try {
        val existing = activityRecordDao.getActivityRecordById(id)
        if (existing?.organizationId?.isNotBlank() == true) {
            syncRepository.deleteActivityRecord(existing)
        }
        activityRecordDao.deleteActivityRecordById(id)
        Result.success(Unit)
    } catch (e: Exception) {
        println("Erro ao excluir registro por ID $id: ${e.message}")
        Result.failure(e)
    }
    
    suspend fun deleteActivityRecordsByVehicle(vehicleId: Long): Result<Unit> = try {
        activityRecordDao.deleteActivityRecordsByVehicle(vehicleId)
        Result.success(Unit)
    } catch (e: Exception) {
        println("Erro ao excluir registros do veículo $vehicleId: ${e.message}")
        Result.failure(e)
    }
    
    // ===== CONSULTAS ESPECÍFICAS =====
    
    fun getActivityRecordsByPlateAndDateRange(
        plate: String, 
        startDate: LocalDateTime, 
        endDate: LocalDateTime
    ): Flow<List<ActivityRecord>> = 
        activityRecordDao.getActivityRecordsByPlateAndDateRange(plate, startDate, endDate)
            .catch { exception ->
                println("Erro ao carregar registros por placa e período: ${exception.message}")
                emit(emptyList())
            }
    
    fun getActivityRecordsByVehicleAndDateRange(
        vehicleId: Long, 
        startDate: LocalDateTime, 
        endDate: LocalDateTime
    ): Flow<List<ActivityRecord>> = 
        activityRecordDao.getActivityRecordsByVehicleAndDateRange(vehicleId, startDate, endDate)
            .catch { exception ->
                println("Erro ao carregar registros por veículo e período: ${exception.message}")
                emit(emptyList())
            }
    
    suspend fun getLastActivityRecordByPlate(plate: String): ActivityRecord? = try {
        activityRecordDao.getLastActivityRecordByPlate(plate)
    } catch (e: Exception) {
        println("Erro ao buscar último registro da placa $plate: ${e.message}")
        null
    }
    
    suspend fun getLastActivityRecordByVehicle(vehicleId: Long): ActivityRecord? = try {
        activityRecordDao.getLastActivityRecordByVehicle(vehicleId)
    } catch (e: Exception) {
        println("Erro ao buscar último registro do veículo $vehicleId: ${e.message}")
        null
    }
    
    suspend fun getLastCompletedTripByPlate(plate: String): ActivityRecord? = try {
        activityRecordDao.getLastCompletedTripByPlate(plate)
    } catch (e: Exception) {
        println("Erro ao buscar última viagem completa da placa $plate: ${e.message}")
        null
    }
    
    // ===== ESTATÍSTICAS =====
    
    suspend fun getActivityCountByPlate(plate: String): Int = try {
        activityRecordDao.getActivityCountByPlate(plate)
    } catch (e: Exception) {
        println("Erro ao contar atividades da placa $plate: ${e.message}")
        0
    }
    
    suspend fun getActivityCountByVehicle(vehicleId: Long): Int = try {
        activityRecordDao.getActivityCountByVehicle(vehicleId)
    } catch (e: Exception) {
        println("Erro ao contar atividades do veículo $vehicleId: ${e.message}")
        0
    }
    
    suspend fun getTotalDistanceByPlate(plate: String): Long = try {
        activityRecordDao.getTotalDistanceByPlate(plate) ?: 0L
    } catch (e: Exception) {
        println("Erro ao calcular distância total da placa $plate: ${e.message}")
        0L
    }
    
    suspend fun getTotalDistanceByVehicle(vehicleId: Long): Long = try {
        activityRecordDao.getTotalDistanceByVehicle(vehicleId) ?: 0L
    } catch (e: Exception) {
        println("Erro ao calcular distância total do veículo $vehicleId: ${e.message}")
        0L
    }
    
    // ===== VALIDAÇÃO =====
    
    suspend fun validateActivityRecord(activityRecord: ActivityRecord): Boolean {
        // Validação básica
        if (activityRecord.plate.isBlank()) return false
        if (activityRecord.driver.isBlank()) return false
        if (activityRecord.startMileage < 0) return false
        if (activityRecord.endMileage < activityRecord.startMileage) return false
        
        return true
    }
}