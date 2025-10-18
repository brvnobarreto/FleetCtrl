package dev.barreto.fleetctrl.data.repositories

import dev.barreto.fleetctrl.data.database.daos.FuelRecordDao
import dev.barreto.fleetctrl.data.database.entities.FuelRecord
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository para operações com registros de abastecimento
 */
@Singleton
class FuelRecordRepository @Inject constructor(
    private val fuelRecordDao: FuelRecordDao,
    private val syncRepository: SyncRepository
) {
    
    // ===== OPERAÇÕES BÁSICAS =====
    
    fun getAllFuelRecords(): Flow<List<FuelRecord>> = fuelRecordDao.getAllFuelRecords()
    
    fun getFuelRecordsByVehicle(vehicleId: Long): Flow<List<FuelRecord>> = 
        fuelRecordDao.getFuelRecordsByVehicle(vehicleId)
    
    suspend fun getFuelRecordById(id: Long): FuelRecord? = fuelRecordDao.getFuelRecordById(id)
    
    suspend fun insertFuelRecord(fuelRecord: FuelRecord): Long {
        val id = fuelRecordDao.insertFuelRecord(fuelRecord)
        val saved = fuelRecordDao.getFuelRecordById(id)
        if (saved?.organizationId?.isNotBlank() == true) {
            syncRepository.uploadFuelRecord(saved)
        }
        return id
    }
    
    suspend fun insertFuelRecords(fuelRecords: List<FuelRecord>) {
        fuelRecordDao.insertFuelRecords(fuelRecords)
        fuelRecords.filter { !it.organizationId.isNullOrBlank() }.forEach { syncRepository.uploadFuelRecord(it) }
    }
    
    suspend fun updateFuelRecord(fuelRecord: FuelRecord) {
        fuelRecordDao.updateFuelRecord(fuelRecord)
        if (fuelRecord.organizationId?.isNotBlank() == true) {
            syncRepository.uploadFuelRecord(fuelRecord)
        }
    }
    
    suspend fun deleteFuelRecord(fuelRecord: FuelRecord) {
        if (fuelRecord.organizationId?.isNotBlank() == true) {
            syncRepository.deleteFuelRecord(fuelRecord)
        }
        fuelRecordDao.deleteFuelRecord(fuelRecord)
    }
    
    suspend fun deleteFuelRecordById(id: Long) {
        val existing = fuelRecordDao.getFuelRecordById(id)
        if (existing?.organizationId?.isNotBlank() == true) {
            syncRepository.deleteFuelRecord(existing)
        }
        fuelRecordDao.deleteFuelRecordById(id)
    }
    
    // ===== OPERAÇÕES POR PERÍODO =====
    
    fun getFuelRecordsByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<FuelRecord>> = 
        fuelRecordDao.getFuelRecordsByDateRange(startDate, endDate)
    
    fun getFuelRecordsByVehicleAndDateRange(vehicleId: Long, startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<FuelRecord>> = 
        fuelRecordDao.getFuelRecordsByVehicleAndDateRange(vehicleId, startDate, endDate)
    
    fun getFuelRecordsFromDate(date: LocalDateTime): Flow<List<FuelRecord>> = 
        fuelRecordDao.getFuelRecordsFromDate(date)
    
    fun getFuelRecordsUntilDate(date: LocalDateTime): Flow<List<FuelRecord>> = 
        fuelRecordDao.getFuelRecordsUntilDate(date)
    
    // ===== OPERAÇÕES POR TIPO DE COMBUSTÍVEL =====
    
    fun getFuelRecordsByType(fuelType: String): Flow<List<FuelRecord>> = 
        fuelRecordDao.getFuelRecordsByType(fuelType)
    
    fun getFuelRecordsByVehicleAndType(vehicleId: Long, fuelType: String): Flow<List<FuelRecord>> = 
        fuelRecordDao.getFuelRecordsByVehicleAndType(vehicleId, fuelType)
    
    // ===== OPERAÇÕES DE ESTATÍSTICAS =====
    
    suspend fun getFuelRecordCountByVehicle(vehicleId: Long): Int = 
        fuelRecordDao.getFuelRecordCountByVehicle(vehicleId)
    
    suspend fun getTotalFuelQuantityByVehicle(vehicleId: Long): Double? = 
        fuelRecordDao.getTotalFuelQuantityByVehicle(vehicleId)
    
    suspend fun getTotalFuelCostByVehicle(vehicleId: Long): Double? = 
        fuelRecordDao.getTotalFuelCostByVehicle(vehicleId)
    
    suspend fun getAveragePricePerLiter(vehicleId: Long, fuelType: String): Double? = 
        fuelRecordDao.getAveragePricePerLiter(vehicleId, fuelType)
    
    suspend fun getAveragePricePerLiterByType(fuelType: String): Double? = 
        fuelRecordDao.getAveragePricePerLiterByType(fuelType)
    
    suspend fun getMinPricePerLiter(vehicleId: Long, fuelType: String): Double? = 
        fuelRecordDao.getMinPricePerLiter(vehicleId, fuelType)
    
    suspend fun getMaxPricePerLiter(vehicleId: Long, fuelType: String): Double? = 
        fuelRecordDao.getMaxPricePerLiter(vehicleId, fuelType)
    
    // ===== OPERAÇÕES DE CONSUMO =====
    
    suspend fun getConsecutiveFuelRecords(vehicleId: Long): List<FuelRecord> = 
        fuelRecordDao.getConsecutiveFuelRecords(vehicleId)
    
    fun getRecentFuelRecords(vehicleId: Long, limit: Int = 10): Flow<List<FuelRecord>> = 
        fuelRecordDao.getRecentFuelRecords(vehicleId, limit)
    
    // ===== OPERAÇÕES DE BUSCA =====
    
    fun searchFuelRecords(query: String): Flow<List<FuelRecord>> = 
        fuelRecordDao.searchFuelRecords("%$query%")
    
    fun searchFuelRecordsByVehicle(vehicleId: Long, query: String): Flow<List<FuelRecord>> = 
        fuelRecordDao.searchFuelRecordsByVehicle(vehicleId, "%$query%")
    
    // ===== OPERAÇÕES DE POSTO =====
    
    fun getAllGasStations(): Flow<List<String>> = fuelRecordDao.getAllGasStations()
    
    fun getFuelRecordsByGasStation(gasStation: String): Flow<List<FuelRecord>> = 
        fuelRecordDao.getFuelRecordsByGasStation(gasStation)
    
    // ===== OPERAÇÕES DE VALIDAÇÃO =====
    
    suspend fun validateFuelRecord(fuelRecord: FuelRecord): Boolean {
        // Validar veículo
        if (fuelRecord.vehicleId != null && fuelRecord.vehicleId <= 0) return false
        
        // Validar data
        if (fuelRecord.date.isAfter(LocalDateTime.now())) return false
        
        // Validar tipo de combustível
        if (fuelRecord.fuelType.isBlank()) return false
        
        // Validar quantidade
        if (fuelRecord.quantity <= 0) return false
        
        // Validar preço por litro
        if (fuelRecord.pricePerLiter <= BigDecimal.ZERO) return false
        
        // Validar custo total
        if (fuelRecord.totalCost <= BigDecimal.ZERO) return false
        
        // Validar quilometragem
        if (fuelRecord.mileage < 0) return false
        
        // Validar consistência de preços
        val expectedTotal = fuelRecord.pricePerLiter.multiply(BigDecimal.valueOf(fuelRecord.quantity))
        if (fuelRecord.totalCost != expectedTotal) return false
        
        return true
    }
    
    // ===== OPERAÇÕES DE CÁLCULO =====
    
    suspend fun calculateFuelConsumption(vehicleId: Long, startDate: LocalDateTime, endDate: LocalDateTime): Double? {
        val records = getFuelRecordsByVehicleAndDateRange(vehicleId, startDate, endDate).let { flow ->
            // Converter Flow para List (em um contexto real, isso seria feito no ViewModel)
            // Por simplicidade, retornamos null aqui
            null
        }
        
        // Lógica de cálculo de consumo seria implementada aqui
        return null
    }
    
    suspend fun calculateAverageFuelCost(vehicleId: Long, fuelType: String): Double? = 
        getAveragePricePerLiter(vehicleId, fuelType)
}