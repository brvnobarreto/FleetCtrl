package dev.barreto.fleetctrl.data.database.daos

import androidx.room.*
import dev.barreto.fleetctrl.data.database.entities.ActivityRecord
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * DAO para operações com registros de atividade
 */
@Dao
interface ActivityRecordDao {
    
    // ===== OPERAÇÕES BÁSICAS =====
    
    @Query("SELECT * FROM activity_records ORDER BY date DESC")
    fun getAllActivityRecords(): Flow<List<ActivityRecord>>
    
    @Query("SELECT * FROM activity_records WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun getActivityRecordsByVehicle(vehicleId: Long): Flow<List<ActivityRecord>>
    
    @Query("SELECT * FROM activity_records WHERE plate = :plate ORDER BY date DESC")
    fun getActivityRecordsByPlate(plate: String): Flow<List<ActivityRecord>>
    
    @Query("SELECT * FROM activity_records WHERE id = :id")
    suspend fun getActivityRecordById(id: Long): ActivityRecord?
    
    @Query("SELECT * FROM activity_records WHERE driver = :driver ORDER BY date DESC")
    fun getActivityRecordsByDriver(driver: String): Flow<List<ActivityRecord>>
    
    // ===== INSERÇÃO E ATUALIZAÇÃO =====
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityRecord(activityRecord: ActivityRecord): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityRecords(activityRecords: List<ActivityRecord>)
    
    @Update
    suspend fun updateActivityRecord(activityRecord: ActivityRecord)
    
    // ===== EXCLUSÃO =====
    
    @Delete
    suspend fun deleteActivityRecord(activityRecord: ActivityRecord)
    
    @Query("DELETE FROM activity_records WHERE id = :id")
    suspend fun deleteActivityRecordById(id: Long)
    
    @Query("DELETE FROM activity_records WHERE vehicleId = :vehicleId")
    suspend fun deleteActivityRecordsByVehicle(vehicleId: Long)

    @Query("DELETE FROM activity_records WHERE plate = :plate")
    suspend fun deleteActivityRecordsByPlate(plate: String)
    
    // ===== CONSULTAS ESPECÍFICAS =====
    
    @Query("""
        SELECT * FROM activity_records 
        WHERE plate = :plate 
        AND date BETWEEN :startDate AND :endDate 
        ORDER BY date DESC
    """)
    fun getActivityRecordsByPlateAndDateRange(
        plate: String, 
        startDate: LocalDateTime, 
        endDate: LocalDateTime
    ): Flow<List<ActivityRecord>>
    
    @Query("""
        SELECT * FROM activity_records 
        WHERE vehicleId = :vehicleId 
        AND date BETWEEN :startDate AND :endDate 
        ORDER BY date DESC
    """)
    fun getActivityRecordsByVehicleAndDateRange(
        vehicleId: Long, 
        startDate: LocalDateTime, 
        endDate: LocalDateTime
    ): Flow<List<ActivityRecord>>
    
    @Query("""
        SELECT * FROM activity_records 
        WHERE plate = :plate 
        ORDER BY date DESC 
        LIMIT 1
    """)
    suspend fun getLastActivityRecordByPlate(plate: String): ActivityRecord?
    
    @Query("""
        SELECT * FROM activity_records 
        WHERE vehicleId = :vehicleId 
        ORDER BY date DESC 
        LIMIT 1
    """)
    suspend fun getLastActivityRecordByVehicle(vehicleId: Long): ActivityRecord?
    
    @Query("""
        SELECT * FROM activity_records 
        WHERE plate = :plate 
        AND endMileage > startMileage 
        ORDER BY date DESC 
        LIMIT 1
    """)
    suspend fun getLastCompletedTripByPlate(plate: String): ActivityRecord?
    
    // ===== ESTATÍSTICAS =====
    
    @Query("SELECT COUNT(*) FROM activity_records WHERE plate = :plate")
    suspend fun getActivityCountByPlate(plate: String): Int
    
    @Query("SELECT COUNT(*) FROM activity_records WHERE vehicleId = :vehicleId")
    suspend fun getActivityCountByVehicle(vehicleId: Long): Int
    
    @Query("""
        SELECT SUM(endMileage - startMileage) 
        FROM activity_records 
        WHERE plate = :plate 
        AND endMileage > startMileage
    """)
    suspend fun getTotalDistanceByPlate(plate: String): Long?
    
    @Query("""
        SELECT SUM(endMileage - startMileage) 
        FROM activity_records 
        WHERE vehicleId = :vehicleId 
        AND endMileage > startMileage
    """)
    suspend fun getTotalDistanceByVehicle(vehicleId: Long): Long?
    
    // ===== FILTROS POR ORGANIZAÇÃO =====
    
    @Query("SELECT * FROM activity_records WHERE organizationId = :organizationId ORDER BY date DESC")
    suspend fun getActivityRecordsByOrganization(organizationId: String): List<ActivityRecord>
    
    @Query("SELECT * FROM activity_records WHERE organizationId = :organizationId AND vehicleId = :vehicleId ORDER BY date DESC")
    fun getActivityRecordsByOrganizationAndVehicle(organizationId: String, vehicleId: Long): Flow<List<ActivityRecord>>
    
    // ===== MÉTODOS DE LIMPEZA =====
    
    @Query("SELECT * FROM activity_records")
    suspend fun getAllActivityRecordsList(): List<ActivityRecord>
    
    @Query("DELETE FROM activity_records")
    suspend fun deleteAllActivityRecords()
}
