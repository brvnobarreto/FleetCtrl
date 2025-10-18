package dev.barreto.fleetctrl.data.repositories

import dev.barreto.fleetctrl.data.database.daos.MaintenanceRecordDao
import dev.barreto.fleetctrl.data.database.entities.MaintenanceRecord
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositório para operações com registros de manutenção
 */
@Singleton
class MaintenanceRecordRepository @Inject constructor(
    private val maintenanceRecordDao: MaintenanceRecordDao,
    private val syncRepository: SyncRepository
) {
    
    fun getMaintenanceRecordsByVehicle(vehicleId: Long): Flow<List<MaintenanceRecord>> {
        return maintenanceRecordDao.getMaintenanceRecordsByVehicle(vehicleId)
    }
    
    suspend fun getLatestMaintenanceRecord(vehicleId: Long): MaintenanceRecord? {
        return maintenanceRecordDao.getLatestMaintenanceRecord(vehicleId)
    }
    
    suspend fun getMaintenanceRecordById(id: Long): MaintenanceRecord? {
        return maintenanceRecordDao.getMaintenanceRecordById(id)
    }
    
    suspend fun insertMaintenanceRecord(maintenanceRecord: MaintenanceRecord): Long {
        val id = maintenanceRecordDao.insertMaintenanceRecord(maintenanceRecord)
        val saved = maintenanceRecordDao.getMaintenanceRecordById(id)
        if (saved?.organizationId?.isNotBlank() == true) {
            syncRepository.uploadMaintenanceRecord(saved)
        }
        return id
    }
    
    suspend fun updateMaintenanceRecord(maintenanceRecord: MaintenanceRecord) {
        maintenanceRecordDao.updateMaintenanceRecord(maintenanceRecord)
        if (maintenanceRecord.organizationId?.isNotBlank() == true) {
            syncRepository.uploadMaintenanceRecord(maintenanceRecord)
        }
    }
    
    suspend fun deleteMaintenanceRecord(maintenanceRecord: MaintenanceRecord) {
        if (maintenanceRecord.organizationId?.isNotBlank() == true) {
            syncRepository.deleteMaintenanceRecord(maintenanceRecord)
        }
        maintenanceRecordDao.deleteMaintenanceRecord(maintenanceRecord)
    }
    
    suspend fun deleteMaintenanceRecordById(id: Long) {
        val existing = maintenanceRecordDao.getMaintenanceRecordById(id)
        if (existing?.organizationId?.isNotBlank() == true) {
            syncRepository.deleteMaintenanceRecord(existing)
        }
        maintenanceRecordDao.deleteMaintenanceRecordById(id)
    }
    
    suspend fun deleteMaintenanceRecordsByVehicle(vehicleId: Long) {
        maintenanceRecordDao.deleteMaintenanceRecordsByVehicle(vehicleId)
    }
}