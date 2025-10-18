package dev.barreto.fleetctrl.data.database.daos

import androidx.room.*
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operações com veículos
 */
@Dao
interface VehicleDao {
    
    // ===== OPERAÇÕES BÁSICAS =====
    
    @Query("SELECT * FROM vehicles ORDER BY createdAt DESC")
    fun getAllVehicles(): Flow<List<Vehicle>>
    
    @Query("SELECT * FROM vehicles WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveVehicles(): Flow<List<Vehicle>>
    
    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getVehicleById(id: Long): Vehicle?
    
    @Query("SELECT * FROM vehicles WHERE plate = :plate")
    suspend fun getVehicleByPlate(plate: String): Vehicle?
    
    @Query("SELECT * FROM vehicles WHERE vehicleNumber = :vehicleNumber")
    suspend fun getVehicleByNumber(vehicleNumber: String): Vehicle?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: Vehicle): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicles(vehicles: List<Vehicle>)
    
    @Update
    suspend fun updateVehicle(vehicle: Vehicle)
    
    @Delete
    suspend fun deleteVehicle(vehicle: Vehicle)
    
    @Query("DELETE FROM vehicles WHERE id = :id")
    suspend fun deleteVehicleById(id: Long)

    // Deleção em lote para otimizações de listener
    @Query("DELETE FROM vehicles WHERE id IN (:ids)")
    suspend fun deleteVehiclesByIds(ids: List<Long>)
    
    // ===== OPERAÇÕES DE BUSCA =====
    
    @Query("SELECT * FROM vehicles WHERE brand LIKE :brand ORDER BY model ASC")
    fun getVehiclesByBrand(brand: String): Flow<List<Vehicle>>
    
    @Query("SELECT * FROM vehicles WHERE model LIKE :model ORDER BY brand ASC")
    fun getVehiclesByModel(model: String): Flow<List<Vehicle>>
    
    @Query("SELECT * FROM vehicles WHERE year = :year ORDER BY brand ASC, model ASC")
    fun getVehiclesByYear(year: Int): Flow<List<Vehicle>>
    
    @Query("SELECT * FROM vehicles WHERE year BETWEEN :startYear AND :endYear ORDER BY year DESC, brand ASC")
    fun getVehiclesByYearRange(startYear: Int, endYear: Int): Flow<List<Vehicle>>
    
    @Query("SELECT * FROM vehicles WHERE engineType = :engineType ORDER BY brand ASC, model ASC")
    fun getVehiclesByEngineType(engineType: String): Flow<List<Vehicle>>
    
    // ===== OPERAÇÕES DE QUILOMETRAGEM =====
    
    @Query("UPDATE vehicles SET currentMileage = :mileage, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateMileage(id: Long, mileage: Long, updatedAt: java.time.LocalDateTime)
    
    @Query("SELECT * FROM vehicles WHERE currentMileage >= :minMileage ORDER BY currentMileage DESC")
    fun getVehiclesWithMinMileage(minMileage: Long): Flow<List<Vehicle>>
    
    @Query("SELECT * FROM vehicles WHERE currentMileage - lastMaintenanceMileage >= :maintenanceThreshold ORDER BY currentMileage - lastMaintenanceMileage DESC")
    fun getVehiclesNeedingMaintenance(maintenanceThreshold: Long): Flow<List<Vehicle>>
    
    // ===== OPERAÇÕES DE ESTATÍSTICAS =====
    
    @Query("SELECT COUNT(*) FROM vehicles WHERE isActive = 1")
    suspend fun getActiveVehicleCount(): Int
    
    @Query("SELECT COUNT(*) FROM vehicles")
    suspend fun getTotalVehicleCount(): Int
    
    @Query("SELECT AVG(currentMileage) FROM vehicles WHERE isActive = 1")
    suspend fun getAverageMileage(): Double?
    
    @Query("SELECT MAX(currentMileage) FROM vehicles WHERE isActive = 1")
    suspend fun getMaxMileage(): Long?
    
    @Query("SELECT MIN(currentMileage) FROM vehicles WHERE isActive = 1")
    suspend fun getMinMileage(): Long?
    
    // ===== OPERAÇÕES DE BUSCA AVANÇADA =====
    
    @Query("""
        SELECT * FROM vehicles 
        WHERE (vehicleNumber LIKE :query OR brand LIKE :query OR model LIKE :query OR plate LIKE :query OR color LIKE :query OR driver LIKE :query)
        AND isActive = 1
        ORDER BY 
            CASE 
                WHEN vehicleNumber LIKE :query THEN 1
                WHEN plate LIKE :query THEN 2
                WHEN brand LIKE :query THEN 3
                WHEN model LIKE :query THEN 4
                ELSE 5
            END,
            brand ASC, model ASC
    """)
    fun searchVehicles(query: String): Flow<List<Vehicle>>
    
    // ===== FILTROS POR ORGANIZAÇÃO =====
    
    @Query("SELECT * FROM vehicles WHERE organizationId = :organizationId ORDER BY createdAt DESC")
    suspend fun getVehiclesByOrganization(organizationId: String): List<Vehicle>
    
    @Query("SELECT * FROM vehicles WHERE organizationId = :organizationId AND isActive = 1 ORDER BY createdAt DESC")
    fun getActiveVehiclesByOrganization(organizationId: String): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles WHERE (organizationId = :organizationId OR organizationId IS NULL) AND isActive = 1 ORDER BY createdAt DESC")
    fun getActiveVehiclesByOrgOrLocal(organizationId: String): Flow<List<Vehicle>>
    
    // ===== MÉTODOS DE LIMPEZA =====
    
    @Query("SELECT * FROM vehicles")
    suspend fun getAllVehiclesList(): List<Vehicle>
    
    @Query("DELETE FROM vehicles")
    suspend fun deleteAllVehicles()
}
