package dev.barreto.fleetctrl.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.barreto.fleetctrl.data.database.entities.MaintenanceRecord
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import dev.barreto.fleetctrl.data.repositories.MaintenanceRecordRepository
import dev.barreto.fleetctrl.data.repositories.VehicleRepository
import dev.barreto.fleetctrl.data.repositories.ActivityRecordRepository
import dev.barreto.fleetctrl.data.repositories.SyncRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import dev.barreto.fleetctrl.utils.DataRefreshNotifier
import javax.inject.Inject

/**
 * ViewModel para gerenciar o estado da tela de manutenção
 */
@HiltViewModel
class MaintenanceViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val maintenanceRecordRepository: MaintenanceRecordRepository,
    private val activityRecordRepository: ActivityRecordRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {
    
    // Estado dos veículos
    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles.asStateFlow()
    
    private val _isLoadingVehicles = MutableStateFlow(false)
    val isLoadingVehicles: StateFlow<Boolean> = _isLoadingVehicles.asStateFlow()
    
    private val _vehiclesMessage = MutableStateFlow<String?>(null)
    val vehiclesMessage: StateFlow<String?> = _vehiclesMessage.asStateFlow()
    
    // Estado dos registros de manutenção
    private val _maintenanceRecords = MutableStateFlow<List<MaintenanceRecord>>(emptyList())
    val maintenanceRecords: StateFlow<List<MaintenanceRecord>> = _maintenanceRecords.asStateFlow()
    
    private val _isLoadingMaintenanceRecords = MutableStateFlow(false)
    val isLoadingMaintenanceRecords: StateFlow<Boolean> = _isLoadingMaintenanceRecords.asStateFlow()
    
    private val _maintenanceRecordsMessage = MutableStateFlow<String?>(null)
    val maintenanceRecordsMessage: StateFlow<String?> = _maintenanceRecordsMessage.asStateFlow()
    
    // Veículo selecionado
    private val _selectedVehicle = MutableStateFlow<Vehicle?>(null)
    val selectedVehicle: StateFlow<Vehicle?> = _selectedVehicle.asStateFlow()
    
    // Última quilometragem do veículo
    private val _lastMileage = MutableStateFlow<Long?>(null)
    val lastMileage: StateFlow<Long?> = _lastMileage.asStateFlow()

    // Estado para Pull-to-Refresh
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    init {
        observeVehicles()
        // Limpa estados quando dados locais são apagados
        DataRefreshNotifier.refreshTrigger
            .onEach {
                _selectedVehicle.value = null
                _maintenanceRecords.value = emptyList()
                _lastMileage.value = null
                loadVehicles()
            }
            .launchIn(viewModelScope)
    }
    
    /**
     * Observa mudanças na lista de veículos
     */
    private fun observeVehicles() {
        viewModelScope.launch {
            try {
                println("DEBUG MaintenanceViewModel: Starting to observe vehicles")
                vehicleRepository.getAllVehicles().collect { vehicles ->
                    println("DEBUG MaintenanceViewModel: Received ${vehicles.size} vehicles")
                    _vehicles.value = vehicles
                }
            } catch (e: Exception) {
                println("DEBUG MaintenanceViewModel: Error loading vehicles: ${e.message}")
                _vehiclesMessage.value = "Erro ao carregar veículos: ${e.message}"
            }
        }
    }
    
    /**
     * Carrega a lista de veículos (one-shot)
     */
    fun loadVehicles() {
        viewModelScope.launch {
            _isLoadingVehicles.value = true
            try {
                val vehicles = vehicleRepository.getAllVehicles().first()
                _vehicles.value = vehicles
            } catch (e: Exception) {
                _vehiclesMessage.value = "Erro ao carregar veículos: ${e.message}"
            } finally {
                _isLoadingVehicles.value = false
            }
        }
    }
    
    /**
     * Seleciona um veículo e carrega seus registros de manutenção
     */
    fun selectVehicle(vehicle: Vehicle) {
        _selectedVehicle.value = vehicle
        observeMaintenanceRecords(vehicle.id)
        loadLastMileage(vehicle.id)
        // Inicia listener realtime para o veículo (se houver org)
        vehicle.organizationId?.let { orgId ->
            if (orgId.isNotBlank()) {
                syncRepository.startMaintenanceRealtime(orgId, vehicle.id)
            }
        }
    }
    
    /**
     * Observa mudanças nos registros de manutenção de um veículo
     */
    private fun observeMaintenanceRecords(vehicleId: Long) {
        viewModelScope.launch {
            try {
                maintenanceRecordRepository.getMaintenanceRecordsByVehicle(vehicleId).collect { records ->
                    _maintenanceRecords.value = records
                }
            } catch (e: Exception) {
                _maintenanceRecordsMessage.value = "Erro ao carregar registros de manutenção: ${e.message}"
            }
        }
    }
    
    /**
     * Carrega os registros de manutenção de um veículo (one-shot)
     */
    fun loadMaintenanceRecords(vehicleId: Long) {
        viewModelScope.launch {
            _isLoadingMaintenanceRecords.value = true
            try {
                val records = maintenanceRecordRepository.getMaintenanceRecordsByVehicle(vehicleId).first()
                _maintenanceRecords.value = records
            } catch (e: Exception) {
                _maintenanceRecordsMessage.value = "Erro ao carregar registros de manutenção: ${e.message}"
            } finally {
                _isLoadingMaintenanceRecords.value = false
            }
        }
    }

    fun clearSelectedVehicle() {
        _selectedVehicle.value = null
        _maintenanceRecords.value = emptyList()
        _lastMileage.value = null
        syncRepository.stopAllRealtime()
    }
    
    /**
     * Carrega a última quilometragem do veículo (do diário de bordo)
     */
    private fun loadLastMileage(vehicleId: Long) {
        viewModelScope.launch {
            try {
                println("DEBUG MaintenanceViewModel: Loading last mileage for vehicle $vehicleId")
                val lastRecord = activityRecordRepository.getLastActivityRecordByVehicle(vehicleId)
                if (lastRecord != null) {
                    _lastMileage.value = lastRecord.endMileage
                    println("DEBUG MaintenanceViewModel: Found last mileage: ${lastRecord.endMileage}")
                } else {
                    _lastMileage.value = null
                    println("DEBUG MaintenanceViewModel: No activity records found")
                }
            } catch (e: Exception) {
                println("DEBUG MaintenanceViewModel: Error loading last mileage: ${e.message}")
                _lastMileage.value = null
            }
        }
    }
    
    /**
     * Adiciona um novo registro de manutenção
     */
    fun addMaintenanceRecord(maintenanceRecord: MaintenanceRecord) {
        viewModelScope.launch {
            try {
                val newId = maintenanceRecordRepository.insertMaintenanceRecord(maintenanceRecord)
                val saved = maintenanceRecordRepository.getMaintenanceRecordById(newId)
                if (saved?.organizationId?.isNotBlank() == true) {
                    syncRepository.uploadMaintenanceRecord(saved)
                }
                _maintenanceRecordsMessage.value = "Registro de manutenção adicionado com sucesso"
            } catch (e: Exception) {
                _maintenanceRecordsMessage.value = "Erro ao adicionar registro: ${e.message}"
            }
        }
    }
    
    /**
     * Atualiza um registro de manutenção
     */
    fun updateMaintenanceRecord(maintenanceRecord: MaintenanceRecord) {
        viewModelScope.launch {
            try {
                maintenanceRecordRepository.updateMaintenanceRecord(maintenanceRecord)
                if (maintenanceRecord.organizationId?.isNotBlank() == true) {
                    syncRepository.uploadMaintenanceRecord(maintenanceRecord)
                }
                _maintenanceRecordsMessage.value = "Registro de manutenção atualizado com sucesso"
            } catch (e: Exception) {
                _maintenanceRecordsMessage.value = "Erro ao atualizar registro: ${e.message}"
            }
        }
    }
    
    /**
     * Remove um registro de manutenção
     */
    fun deleteMaintenanceRecord(maintenanceRecord: MaintenanceRecord) {
        viewModelScope.launch {
            try {
                maintenanceRecordRepository.deleteMaintenanceRecord(maintenanceRecord)
                if (maintenanceRecord.organizationId?.isNotBlank() == true) {
                    syncRepository.deleteMaintenanceRecord(maintenanceRecord)
                }
                _maintenanceRecordsMessage.value = "Registro de manutenção removido com sucesso"
            } catch (e: Exception) {
                _maintenanceRecordsMessage.value = "Erro ao remover registro: ${e.message}"
            }
        }
    }

    /**
     * Aciona a sincronização manual (usado pelo pull-to-refresh)
     */
    fun triggerRefresh() {
        viewModelScope.launch {
            Log.d("MaintenanceViewModel", "triggerRefresh() called. vehicles=${vehicles.value.size}")
            _isRefreshing.value = true
            try {
                syncRepository.syncVehiclesForAllOrganizations()
                syncRepository.syncMaintenanceRecordsForAllOrganizations()
                loadVehicles()

            } catch (e: Exception) {
                _vehiclesMessage.value = "Erro durante a sincronização: ${e.message}"
                Log.e("MaintenanceViewModel", "Erro durante triggerRefresh", e)
            } finally {
                _isRefreshing.value = false
                Log.d("MaintenanceViewModel", "triggerRefresh() finished")
            }
        }
    }
    
    /**
     * Limpa a mensagem de veículos
     */
    fun clearVehiclesMessage() {
        _vehiclesMessage.value = null
    }
    
    /**
     * Limpa a mensagem de registros de manutenção
     */
    fun clearMaintenanceRecordsMessage() {
        _maintenanceRecordsMessage.value = null
    }
    
    /**
     * Limpa o veículo selecionado
     */
    // (removida duplicidade de clearSelectedVehicle)
}
