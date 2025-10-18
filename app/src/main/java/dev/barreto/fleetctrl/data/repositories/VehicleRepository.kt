package dev.barreto.fleetctrl.data.repositories

import dev.barreto.fleetctrl.data.database.daos.ActivityRecordDao
import dev.barreto.fleetctrl.data.database.daos.FuelRecordDao
import dev.barreto.fleetctrl.data.database.daos.MaintenanceRecordDao
import dev.barreto.fleetctrl.data.database.daos.VehicleDao
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository para operações com veículos
 * Implementa o padrão Repository para abstrair o acesso aos dados
 */
@Singleton
class VehicleRepository @Inject constructor(
    private val vehicleDao: VehicleDao,
    private val syncRepository: SyncRepository,
    private val fuelRecordDao: FuelRecordDao,
    private val activityRecordDao: ActivityRecordDao,
    private val maintenanceRecordDao: MaintenanceRecordDao
) {
    
    // ===== OPERAÇÕES BÁSICAS =====
    
    fun getAllVehicles(): Flow<List<Vehicle>> = vehicleDao.getAllVehicles()
        .catch { exception ->
            println("Erro ao carregar veículos: ${exception.message}")
            emit(emptyList())
        }
    
    fun getActiveVehicles(): Flow<List<Vehicle>> = vehicleDao.getActiveVehicles()
        .catch { exception ->
            println("Erro ao carregar veículos ativos: ${exception.message}")
            emit(emptyList())
        }
    
    suspend fun getVehicleById(id: Long): Vehicle? = try {
        vehicleDao.getVehicleById(id)
    } catch (e: Exception) {
        println("Erro ao buscar veículo por ID $id: ${e.message}")
        null
    }
    
    suspend fun getVehicleByPlate(plate: String): Vehicle? = try {
        vehicleDao.getVehicleByPlate(plate)
    } catch (e: Exception) {
        println("Erro ao buscar veículo por placa $plate: ${e.message}")
        null
    }
    
    suspend fun getVehicleByNumber(vehicleNumber: String): Vehicle? = try {
        vehicleDao.getVehicleByNumber(vehicleNumber)
    } catch (e: Exception) {
        println("Erro ao buscar veículo por número $vehicleNumber: ${e.message}")
        null
    }

    suspend fun insertVehicle(vehicle: Vehicle): Long {
        return try {
            val newVehicleId = vehicleDao.insertVehicle(vehicle)
            val newVehicle = getVehicleById(newVehicleId)
            if (newVehicle != null) {
                if (!newVehicle.organizationId.isNullOrBlank()) {
                    syncRepository.uploadVehicle(newVehicle)
                }
            }
            newVehicleId
        } catch (e: Exception) {
            println("Erro ao inserir veículo: ${e.message}")
            throw e
        }
    }
    
    suspend fun insertVehicles(vehicles: List<Vehicle>) = try {
        vehicleDao.insertVehicles(vehicles)
    } catch (e: Exception) {
        println("Erro ao inserir veículos: ${e.message}")
        throw e
    }
    
    suspend fun updateVehicle(vehicle: Vehicle) {
        try {
            vehicleDao.updateVehicle(vehicle)
            if (!vehicle.organizationId.isNullOrBlank()) {
                syncRepository.uploadVehicle(vehicle)
            }
        } catch (e: Exception) {
            println("Erro ao atualizar veículo: ${e.message}")
            throw e
        }
    }
    
    suspend fun deleteVehicle(vehicle: Vehicle) {
        try {
            // Primeiro, exclui do Firestore
            if (!vehicle.organizationId.isNullOrBlank()) {
                syncRepository.deleteVehicle(vehicle)
            }

            // Em seguida, exclui os registros dependentes do banco local
            val vehicleId = vehicle.id
            activityRecordDao.deleteActivityRecordsByVehicle(vehicleId)
            fuelRecordDao.deleteFuelRecordsByVehicle(vehicleId)
            maintenanceRecordDao.deleteMaintenanceRecordsByVehicle(vehicleId)

            // Finalmente, exclui o veículo do banco de dados local
            vehicleDao.deleteVehicle(vehicle)
            
            // Notifica a UI para recarregar os dados
            dev.barreto.fleetctrl.utils.DataRefreshNotifier.triggerRefreshSync()
            
            println("Veículo ${vehicle.id} excluído com sucesso")
        } catch (e: Exception) {
            println("Erro ao excluir veículo ${vehicle.id}: ${e.message}")
            throw e
        }
    }

    suspend fun deleteVehicleById(id: Long) {
        try {
            val vehicle = getVehicleById(id)
            vehicle?.let {
                deleteVehicle(it)
            }
        } catch (e: Exception) {
            println("Erro ao excluir veículo por ID $id: ${e.message}")
            throw e
        }
    }
    
    fun getActiveVehiclesByOrganizationOrLocal(organizationId: String): Flow<List<Vehicle>> {
        return vehicleDao.getActiveVehiclesByOrganization(organizationId)
            .catch { exception ->
                println("Erro ao carregar veículos da organização: ${exception.message}")
                emit(emptyList())
            }
    }
    
    // ===== OPERAÇÕES DE BUSCA =====
    
    fun getVehiclesByBrand(brand: String): Flow<List<Vehicle>> = 
        vehicleDao.getVehiclesByBrand("%$brand%")
    
    fun getVehiclesByModel(model: String): Flow<List<Vehicle>> = 
        vehicleDao.getVehiclesByModel("%$model%")
    
    fun getVehiclesByYear(year: Int): Flow<List<Vehicle>> = 
        vehicleDao.getVehiclesByYear(year)
    
    fun getVehiclesByYearRange(startYear: Int, endYear: Int): Flow<List<Vehicle>> = 
        vehicleDao.getVehiclesByYearRange(startYear, endYear)
    
    fun getVehiclesByEngineType(engineType: String): Flow<List<Vehicle>> = 
        vehicleDao.getVehiclesByEngineType(engineType)
    
    fun searchVehicles(query: String): Flow<List<Vehicle>> = 
        vehicleDao.searchVehicles("%$query%")
    
    // ===== OPERAÇÕES DE QUILOMETRAGEM =====
    
    suspend fun updateMileage(id: Long, mileage: Long) {
        val vehicle = getVehicleById(id)
        vehicle?.let {
            val updatedVehicle = it.copy(
                currentMileage = mileage,
                updatedAt = java.time.LocalDateTime.now()
            )
            updateVehicle(updatedVehicle)
        }
    }
    
    fun getVehiclesWithMinMileage(minMileage: Long): Flow<List<Vehicle>> = 
        vehicleDao.getVehiclesWithMinMileage(minMileage)
    
    fun getVehiclesNeedingMaintenance(maintenanceThreshold: Long): Flow<List<Vehicle>> = 
        vehicleDao.getVehiclesNeedingMaintenance(maintenanceThreshold)
    
    // ===== OPERAÇÕES DE ESTATÍSTICAS =====
    
    suspend fun getActiveVehicleCount(): Int = vehicleDao.getActiveVehicleCount()
    
    suspend fun getTotalVehicleCount(): Int = vehicleDao.getTotalVehicleCount()
    
    suspend fun getAverageMileage(): Double? = vehicleDao.getAverageMileage()
    
    suspend fun getMaxMileage(): Long? = vehicleDao.getMaxMileage()
    
    suspend fun getMinMileage(): Long? = vehicleDao.getMinMileage()
    
    // ===== OPERAÇÕES DE VALIDAÇÃO =====
    
    suspend fun isPlateUnique(plate: String, excludeId: Long? = null): Boolean {
        val existingVehicle = getVehicleByPlate(plate)
        return existingVehicle == null || existingVehicle.id == excludeId
    }
    
    suspend fun isVehicleNumberUnique(vehicleNumber: String, excludeId: Long? = null): Boolean {
        val existingVehicle = getVehicleByNumber(vehicleNumber)
        return existingVehicle == null || existingVehicle.id == excludeId
    }
    
    suspend fun validateVehicle(vehicle: Vehicle): Boolean {
        if (vehicle.vehicleNumber.isBlank()) return false
        if (vehicle.plate.isBlank()) return false
        if (vehicle.model.isBlank()) return false
        if (vehicle.driver.isBlank()) return false
        if (vehicle.year <= 1900) return false
        if (!isVehicleNumberUnique(vehicle.vehicleNumber, vehicle.id)) return false
        if (!isPlateUnique(vehicle.plate, vehicle.id)) return false
        return true
    }
    
    // ===== FILTROS POR ORGANIZAÇÃO =====
    
    fun getActiveVehiclesByOrganization(organizationId: String): Flow<List<Vehicle>> = 
        vehicleDao.getActiveVehiclesByOrganization(organizationId)
            .catch { exception ->
                println("Erro ao carregar veículos da organização: ${exception.message}")
                emit(emptyList())
            }

}
