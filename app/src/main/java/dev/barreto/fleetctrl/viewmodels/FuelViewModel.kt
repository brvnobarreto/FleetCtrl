package dev.barreto.fleetctrl.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.barreto.fleetctrl.data.database.entities.FuelRecord
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import dev.barreto.fleetctrl.data.repositories.FleetRepository
import dev.barreto.fleetctrl.data.repositories.FuelRecordRepository
import dev.barreto.fleetctrl.data.repositories.SyncRepository
import dev.barreto.fleetctrl.utils.DataRefreshNotifier
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * ViewModel para gerenciar o estado da tela de abastecimentos
 */
@HiltViewModel
class FuelViewModel @Inject constructor(
    private val fleetRepository: FleetRepository,
    private val fuelRecordRepository: FuelRecordRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {
    
    // ===== ESTADO DOS VEÍCULOS =====
    
    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles.asStateFlow()
    
    private val _isLoadingVehicles = MutableStateFlow(false)
    val isLoadingVehicles: StateFlow<Boolean> = _isLoadingVehicles.asStateFlow()
    
    private val _vehiclesMessage = MutableStateFlow<String?>(null)
    val vehiclesMessage: StateFlow<String?> = _vehiclesMessage.asStateFlow()
    
    // ===== ESTADO DOS REGISTROS DE ABASTECIMENTO =====
    
    private val _fuelRecords = MutableStateFlow<List<FuelRecord>>(emptyList())
    val fuelRecords: StateFlow<List<FuelRecord>> = _fuelRecords.asStateFlow()
    
    private val _isLoadingFuelRecords = MutableStateFlow(false)
    val isLoadingFuelRecords: StateFlow<Boolean> = _isLoadingFuelRecords.asStateFlow()
    
    private val _fuelRecordsMessage = MutableStateFlow<String?>(null)
    val fuelRecordsMessage: StateFlow<String?> = _fuelRecordsMessage.asStateFlow()
    
    // ===== ESTADO DO VEÍCULO SELECIONADO =====
    
    private val _selectedVehicle = MutableStateFlow<Vehicle?>(null)
    val selectedVehicle: StateFlow<Vehicle?> = _selectedVehicle.asStateFlow()

    // Estado para Pull-to-Refresh
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    // ===== INICIALIZAÇÃO =====
    
    init {
        loadVehicles()
        // Limpa estados quando dados locais são apagados
        DataRefreshNotifier.refreshTrigger
            .onEach {
                _selectedVehicle.value = null
                _fuelRecords.value = emptyList()
                loadVehicles()
            }
            .launchIn(viewModelScope)
    }
    
    // ===== OPERAÇÕES COM VEÍCULOS =====
    
    private fun loadVehicles() {
        viewModelScope.launch {
            fleetRepository.vehicleRepository.getAllVehicles()
                .onStart { _isLoadingVehicles.value = true }
                .catch { exception ->
                    _vehiclesMessage.value = "Erro ao carregar veículos: ${exception.message}"
                    _isLoadingVehicles.value = false
                }
                .collect { vehicles ->
                    _vehicles.value = vehicles
                    _isLoadingVehicles.value = false
                }
        }
    }
    
    fun selectVehicle(vehicle: Vehicle) {
        _selectedVehicle.value = vehicle
        loadFuelRecords(vehicle.id)
        // Inicia listener realtime para o veículo selecionado (se houver org)
        vehicle.organizationId?.let { orgId ->
            if (orgId.isNotBlank()) {
                syncRepository.startFuelRealtime(orgId, vehicle.id)
            }
        }
    }
    
    fun clearSelectedVehicle() {
        _selectedVehicle.value = null
        _fuelRecords.value = emptyList()
        syncRepository.stopAllRealtime()
    }
    
    // ===== OPERAÇÕES COM REGISTROS DE ABASTECIMENTO =====
    
    private fun loadFuelRecords(vehicleId: Long) {
        viewModelScope.launch {
            fuelRecordRepository.getFuelRecordsByVehicle(vehicleId)
                .onStart { _isLoadingFuelRecords.value = true }
                .catch { exception ->
                    _fuelRecordsMessage.value = "Erro ao carregar abastecimentos: ${exception.message}"
                    _isLoadingFuelRecords.value = false
                }
                .collect { records ->
                    _fuelRecords.value = records
                    _isLoadingFuelRecords.value = false
                }
        }
    }
    
    suspend fun addFuelRecord(fuelRecord: FuelRecord): Boolean {
        return try {
            val newId = fuelRecordRepository.insertFuelRecord(fuelRecord)
            // Upload imediato se fizer parte de organização
            val saved = fuelRecordRepository.getFuelRecordById(newId)
            if (saved?.organizationId?.isNotBlank() == true) {
                syncRepository.uploadFuelRecord(saved)
            }
            _fuelRecordsMessage.value = "Abastecimento adicionado com sucesso"
            // Recarrega os registros para atualizar a lista
            selectedVehicle.value?.let { loadFuelRecords(it.id) }
            true
        } catch (e: Exception) {
            _fuelRecordsMessage.value = "Erro ao adicionar abastecimento: ${e.message}"
            false
        }
    }
    
    suspend fun updateFuelRecord(fuelRecord: FuelRecord): Boolean {
        return try {
            fuelRecordRepository.updateFuelRecord(fuelRecord)
            if (fuelRecord.organizationId?.isNotBlank() == true) {
                syncRepository.uploadFuelRecord(fuelRecord)
            }
            _fuelRecordsMessage.value = "Abastecimento atualizado com sucesso"
            // Recarrega os registros para atualizar a lista
            selectedVehicle.value?.let { loadFuelRecords(it.id) }
            true
        } catch (e: Exception) {
            _fuelRecordsMessage.value = "Erro ao atualizar abastecimento: ${e.message}"
            false
        }
    }
    
    suspend fun deleteFuelRecord(fuelRecord: FuelRecord): Boolean {
        return try {
            fuelRecordRepository.deleteFuelRecord(fuelRecord)
            if (fuelRecord.organizationId?.isNotBlank() == true) {
                syncRepository.deleteFuelRecord(fuelRecord)
            }
            _fuelRecordsMessage.value = "Abastecimento excluído com sucesso"
            // Recarrega os registros para atualizar a lista
            selectedVehicle.value?.let { loadFuelRecords(it.id) }
            true
        } catch (e: Exception) {
            _fuelRecordsMessage.value = "Erro ao excluir abastecimento: ${e.message}"
            false
        }
    }

    /**
     * Aciona a sincronização manual (usado pelo pull-to-refresh)
     */
    fun triggerRefresh() {
        viewModelScope.launch {
            Log.d("FuelViewModel", "triggerRefresh() called. vehicles=${vehicles.value.size}")
            _isRefreshing.value = true
            try {
                syncRepository.syncVehiclesForAllOrganizations()
                syncRepository.syncFuelRecordsForAllOrganizations()
                loadVehicles()
            } catch (e: Exception) {
                _vehiclesMessage.value = "Erro durante a sincronização: ${e.message}"
                Log.e("FuelViewModel", "Erro durante triggerRefresh", e)
            } finally {
                _isRefreshing.value = false
                Log.d("FuelViewModel", "triggerRefresh() finished")
            }
        }
    }
    
    // ===== OPERAÇÕES DE MENSAGENS =====
    
    fun clearVehiclesMessage() {
        _vehiclesMessage.value = null
    }
    
    fun clearFuelRecordsMessage() {
        _fuelRecordsMessage.value = null
    }
    
    // ===== TIPOS DE COMBUSTÍVEL =====
    
    fun getFuelTypes(): List<String> {
        return listOf(
            "Gasolina Comum",
            "Gasolina Aditivada",
            "Gasolina Premium",
            "Etanol Hidratado",
            "Etanol Anidro",
            "Diesel Comum",
            "Diesel S-10",
            "Diesel S-500",
            "Diesel B5",
            "Diesel B10",
            "Diesel B15",
            "Diesel B20",
            "GNV (Gás Natural Veicular)",
            "GLP (Gás Liquefeito de Petróleo)",
            "Flex (Gasolina + Etanol)",
            "Elétrico",
            "Híbrido (Gasolina + Elétrico)",
            "Híbrido (Diesel + Elétrico)",
            "Híbrido Flex",
            "Hidrogênio"
        )
    }
    
    // ===== CÁLCULOS =====
    
    fun calculateTotalCost(quantity: Double, pricePerLiter: BigDecimal): BigDecimal {
        return pricePerLiter.multiply(BigDecimal.valueOf(quantity))
    }
    
    fun calculateAverageConsumption(vehicle: Vehicle, recentRecords: List<FuelRecord>): Double {
        if (recentRecords.size < 2) return 0.0
        
        val sortedRecords = recentRecords.sortedBy { it.date }
        val totalDistance = sortedRecords.last().mileage - sortedRecords.first().mileage
        val totalFuel = recentRecords.sumOf { it.quantity.toDouble() }
        
        return if (totalFuel > 0) totalDistance / totalFuel else 0.0
    }
}
