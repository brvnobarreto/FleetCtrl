package dev.barreto.fleetctrl.data.database.daos

import androidx.room.*
import dev.barreto.fleetctrl.data.database.entities.FuelRecord
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * DAO para operações com registros de abastecimento
 */
@Dao
interface FuelRecordDao {
    
    // ===== OPERAÇÕES BÁSICAS =====
    
    @Query("SELECT * FROM fuel_records ORDER BY date DESC")
    fun getAllFuelRecords(): Flow<List<FuelRecord>>
    
    @Query("SELECT * FROM fuel_records WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun getFuelRecordsByVehicle(vehicleId: Long): Flow<List<FuelRecord>>
    
    @Query("SELECT * FROM fuel_records WHERE id = :id")
    suspend fun getFuelRecordById(id: Long): FuelRecord?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelRecord(fuelRecord: FuelRecord): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelRecords(fuelRecords: List<FuelRecord>)
    
    @Update
    suspend fun updateFuelRecord(fuelRecord: FuelRecord)
    
    @Delete
    suspend fun deleteFuelRecord(fuelRecord: FuelRecord)
    
    @Query("DELETE FROM fuel_records WHERE id = :id")
    suspend fun deleteFuelRecordById(id: Long)

    @Query("DELETE FROM fuel_records WHERE vehicleId = :vehicleId")
    suspend fun deleteFuelRecordsByVehicle(vehicleId: Long)
    
    // ===== OPERAÇÕES POR PERÍODO =====
    
    @Query("SELECT * FROM fuel_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getFuelRecordsByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<FuelRecord>>
    
    @Query("SELECT * FROM fuel_records WHERE vehicleId = :vehicleId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getFuelRecordsByVehicleAndDateRange(vehicleId: Long, startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<FuelRecord>>
    
    @Query("SELECT * FROM fuel_records WHERE date >= :date ORDER BY date DESC")
    fun getFuelRecordsFromDate(date: LocalDateTime): Flow<List<FuelRecord>>
    
    @Query("SELECT * FROM fuel_records WHERE date <= :date ORDER BY date DESC")
    fun getFuelRecordsUntilDate(date: LocalDateTime): Flow<List<FuelRecord>>
    
    // ===== OPERAÇÕES POR TIPO DE COMBUSTÍVEL =====
    
    @Query("SELECT * FROM fuel_records WHERE fuelType = :fuelType ORDER BY date DESC")
    fun getFuelRecordsByType(fuelType: String): Flow<List<FuelRecord>>
    
    @Query("SELECT * FROM fuel_records WHERE vehicleId = :vehicleId AND fuelType = :fuelType ORDER BY date DESC")
    fun getFuelRecordsByVehicleAndType(vehicleId: Long, fuelType: String): Flow<List<FuelRecord>>
    
    // ===== OPERAÇÕES DE ESTATÍSTICAS =====
    
    @Query("SELECT COUNT(*) FROM fuel_records WHERE vehicleId = :vehicleId")
    suspend fun getFuelRecordCountByVehicle(vehicleId: Long): Int
    
    @Query("SELECT SUM(quantity) FROM fuel_records WHERE vehicleId = :vehicleId")
    suspend fun getTotalFuelQuantityByVehicle(vehicleId: Long): Double?
    
    @Query("SELECT SUM(totalCost) FROM fuel_records WHERE vehicleId = :vehicleId")
    suspend fun getTotalFuelCostByVehicle(vehicleId: Long): Double?
    
    @Query("SELECT AVG(pricePerLiter) FROM fuel_records WHERE vehicleId = :vehicleId AND fuelType = :fuelType")
    suspend fun getAveragePricePerLiter(vehicleId: Long, fuelType: String): Double?
    
    @Query("SELECT AVG(pricePerLiter) FROM fuel_records WHERE fuelType = :fuelType")
    suspend fun getAveragePricePerLiterByType(fuelType: String): Double?
    
    @Query("SELECT MIN(pricePerLiter) FROM fuel_records WHERE vehicleId = :vehicleId AND fuelType = :fuelType")
    suspend fun getMinPricePerLiter(vehicleId: Long, fuelType: String): Double?
    
    @Query("SELECT MAX(pricePerLiter) FROM fuel_records WHERE vehicleId = :vehicleId AND fuelType = :fuelType")
    suspend fun getMaxPricePerLiter(vehicleId: Long, fuelType: String): Double?
    
    // ===== OPERAÇÕES DE CONSUMO =====
    
    @Query("""
        SELECT fr1.* FROM fuel_records fr1
        WHERE fr1.vehicleId = :vehicleId 
        AND fr1.id = (
            SELECT fr2.id FROM fuel_records fr2 
            WHERE fr2.vehicleId = :vehicleId 
            AND fr2.date < fr1.date 
            ORDER BY fr2.date DESC 
            LIMIT 1
        )
        ORDER BY fr1.date DESC
    """)
    suspend fun getConsecutiveFuelRecords(vehicleId: Long): List<FuelRecord>
    
    @Query("SELECT * FROM fuel_records WHERE vehicleId = :vehicleId ORDER BY date DESC LIMIT :limit")
    fun getRecentFuelRecords(vehicleId: Long, limit: Int): Flow<List<FuelRecord>>
    
    // ===== OPERAÇÕES DE BUSCA =====
    
    @Query("""
        SELECT * FROM fuel_records 
        WHERE (gasStation LIKE :query OR location LIKE :query OR receiptNumber LIKE :query)
        ORDER BY date DESC
    """)
    fun searchFuelRecords(query: String): Flow<List<FuelRecord>>
    
    @Query("""
        SELECT * FROM fuel_records 
        WHERE vehicleId = :vehicleId 
        AND (gasStation LIKE :query OR location LIKE :query OR receiptNumber LIKE :query)
        ORDER BY date DESC
    """)
    fun searchFuelRecordsByVehicle(vehicleId: Long, query: String): Flow<List<FuelRecord>>
    
    // ===== OPERAÇÕES DE POSTO =====
    
    @Query("SELECT DISTINCT gasStation FROM fuel_records WHERE gasStation IS NOT NULL ORDER BY gasStation ASC")
    fun getAllGasStations(): Flow<List<String>>
    
    @Query("SELECT * FROM fuel_records WHERE gasStation = :gasStation ORDER BY date DESC")
    fun getFuelRecordsByGasStation(gasStation: String): Flow<List<FuelRecord>>
    
    // ===== FILTROS POR ORGANIZAÇÃO =====
    
    @Query("SELECT * FROM fuel_records WHERE organizationId = :organizationId ORDER BY date DESC")
    suspend fun getFuelRecordsByOrganization(organizationId: String): List<FuelRecord>
    
    @Query("SELECT * FROM fuel_records WHERE organizationId = :organizationId AND vehicleId = :vehicleId ORDER BY date DESC")
    fun getFuelRecordsByOrganizationAndVehicle(organizationId: String, vehicleId: Long): Flow<List<FuelRecord>>
    
    // ===== MÉTODOS DE LIMPEZA =====
    
    @Query("SELECT * FROM fuel_records")
    suspend fun getAllFuelRecordsList(): List<FuelRecord>
    
    @Query("DELETE FROM fuel_records")
    suspend fun deleteAllFuelRecords()
}
