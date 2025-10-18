package dev.barreto.fleetctrl.data.database.daos

import androidx.room.*
import dev.barreto.fleetctrl.data.database.entities.MaintenanceRecord
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operações com registros de manutenção
 */
@Dao
interface MaintenanceRecordDao {
    
    @Query("SELECT * FROM maintenance_records WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun getMaintenanceRecordsByVehicle(vehicleId: Long): Flow<List<MaintenanceRecord>>
    
    @Query("SELECT * FROM maintenance_records WHERE vehicleId = :vehicleId ORDER BY date DESC LIMIT 1")
    suspend fun getLatestMaintenanceRecord(vehicleId: Long): MaintenanceRecord?
    
    @Query("SELECT * FROM maintenance_records WHERE id = :id")
    suspend fun getMaintenanceRecordById(id: Long): MaintenanceRecord?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaintenanceRecord(maintenanceRecord: MaintenanceRecord): Long
    
    @Update
    suspend fun updateMaintenanceRecord(maintenanceRecord: MaintenanceRecord)
    
    @Delete
    suspend fun deleteMaintenanceRecord(maintenanceRecord: MaintenanceRecord)
    
    @Query("DELETE FROM maintenance_records WHERE id = :id")
    suspend fun deleteMaintenanceRecordById(id: Long)
    
    @Query("DELETE FROM maintenance_records WHERE vehicleId = :vehicleId")
    suspend fun deleteMaintenanceRecordsByVehicle(vehicleId: Long)
    
    // ===== FILTROS POR ORGANIZAÇÃO =====
    
    @Query("SELECT * FROM maintenance_records WHERE organizationId = :organizationId ORDER BY date DESC")
    suspend fun getMaintenanceRecordsByOrganization(organizationId: String): List<MaintenanceRecord>
    
    @Query("SELECT * FROM maintenance_records WHERE organizationId = :organizationId AND vehicleId = :vehicleId ORDER BY date DESC")
    fun getMaintenanceRecordsByOrganizationAndVehicle(organizationId: String, vehicleId: Long): Flow<List<MaintenanceRecord>>
    
    // ===== MÉTODOS DE LIMPEZA =====
    
    @Query("SELECT * FROM maintenance_records")
    suspend fun getAllMaintenanceRecordsList(): List<MaintenanceRecord>
    
    @Query("DELETE FROM maintenance_records")
    suspend fun deleteAllMaintenanceRecords()
}
