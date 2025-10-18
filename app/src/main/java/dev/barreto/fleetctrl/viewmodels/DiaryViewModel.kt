package dev.barreto.fleetctrl.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.barreto.fleetctrl.data.database.entities.ActivityRecord
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import dev.barreto.fleetctrl.data.repositories.ActivityRecordRepository
import dev.barreto.fleetctrl.data.repositories.FleetRepository
import dev.barreto.fleetctrl.data.repositories.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import dev.barreto.fleetctrl.utils.DataRefreshNotifier
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * ViewModel para gerenciar o diário de bordo
 * Gerencia veículos visíveis no diário e registros de atividade
 */
@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val fleetRepository: FleetRepository,
    private val activityRecordRepository: ActivityRecordRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {
    
    // ===== ESTADO DA UI =====
    
    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    // ===== ESTADO ESPECÍFICO DO DIÁRIO =====
    
    private val _selectedVehicle = MutableStateFlow<Vehicle?>(null)
    val selectedVehicle: StateFlow<Vehicle?> = _selectedVehicle.asStateFlow()
    
    private val _activityRecords = MutableStateFlow<List<ActivityRecord>>(emptyList())
    val activityRecords: StateFlow<List<ActivityRecord>> = _activityRecords.asStateFlow()
    
    private val _lastMileage = MutableStateFlow<Long?>(null)
    val lastMileage: StateFlow<Long?> = _lastMileage.asStateFlow()

    // Estado para Pull-to-Refresh
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    // ===== INICIALIZAÇÃO =====
    
    init {
        loadVehiclesForDiary()
        observeDataRefresh()
    }
    
    // ===== OPERAÇÕES DE VEÍCULOS =====
    
    private fun loadVehiclesForDiary() {
        viewModelScope.launch {
            fleetRepository.vehicleRepository.getAllVehicles()
                .onStart { _isLoading.value = true }
                .catch { exception ->
                    _errorMessage.value = "Erro ao carregar veículos: ${exception.message}"
                    _isLoading.value = false
                }
                .collect { allVehicles ->
                    // Filtrar apenas veículos marcados para aparecer no diário
                    val diaryVehicles = allVehicles.filter { it.showInDiary }
                    _vehicles.value = diaryVehicles
                    _isLoading.value = false
                }
        }
    }
    
    private fun observeDataRefresh() {
        DataRefreshNotifier.refreshTrigger
            .onEach {
                println("DEBUG DiaryViewModel: Data refresh triggered, reloading vehicles...")
                // Zera estados relacionados à seleção
                _selectedVehicle.value = null
                _activityRecords.value = emptyList()
                _lastMileage.value = null
                // Recarrega os veículos
                loadVehiclesForDiary()
            }
            .launchIn(viewModelScope)
    }
    
    fun selectVehicle(vehicle: Vehicle) {
        _selectedVehicle.value = vehicle
        loadActivityRecordsForVehicle(vehicle.id)
        loadLastMileageForVehicle(vehicle.plate)
        // Inicia listener realtime para o veículo selecionado (se houver org)
        vehicle.organizationId?.let { orgId ->
            if (orgId.isNotBlank()) {
                syncRepository.startActivityRealtime(orgId, vehicle.id)
            }
        }
    }
    
    fun clearSelectedVehicle() {
        _selectedVehicle.value = null
        _activityRecords.value = emptyList()
        _lastMileage.value = null
        syncRepository.stopAllRealtime()
    }
    
    // ===== OPERAÇÕES DE REGISTROS DE ATIVIDADE =====
    
    private fun loadActivityRecordsForVehicle(vehicleId: Long) {
        viewModelScope.launch {
            activityRecordRepository.getActivityRecordsByVehicle(vehicleId)
                .onStart { _isLoading.value = true }
                .catch { exception ->
                    _errorMessage.value = "Erro ao carregar registros: ${exception.message}"
                    _isLoading.value = false
                }
                .collect { records ->
                    _activityRecords.value = records
                    _isLoading.value = false
                }
        }
    }
    
    private fun loadLastMileageForVehicle(plate: String) {
        viewModelScope.launch {
            try {
                val lastRecord = activityRecordRepository.getLastCompletedTripByPlate(plate)
                _lastMileage.value = lastRecord?.endMileage
            } catch (e: Exception) {
                println("Erro ao carregar última quilometragem: ${e.message}")
                _lastMileage.value = null
            }
        }
    }
    
    fun addActivityRecord(activityRecord: ActivityRecord) {
        viewModelScope.launch {
            try {
                val isValid = activityRecordRepository.validateActivityRecord(activityRecord)
                if (isValid) {
                    val result = activityRecordRepository.insertActivityRecord(activityRecord)
                    if (result.isSuccess) {
                        if (activityRecord.organizationId?.isNotBlank() == true) {
                            syncRepository.uploadActivityRecord(
                                activityRecord.copy(id = result.getOrNull() ?: activityRecord.id)
                            )
                        }
                        _successMessage.value = "Registro adicionado com sucesso"
                        _errorMessage.value = null
                        
                        // Recarregar registros se um veículo estiver selecionado
                        _selectedVehicle.value?.let { vehicle ->
                            loadActivityRecordsForVehicle(vehicle.id)
                            loadLastMileageForVehicle(vehicle.plate)
                        }
                    } else {
                        _errorMessage.value = "Erro ao salvar registro"
                    }
                } else {
                    _errorMessage.value = "Dados do registro inválidos"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao adicionar registro: ${e.message}"
            }
        }
    }
    
    fun updateActivityRecord(activityRecord: ActivityRecord) {
        viewModelScope.launch {
            try {
                val isValid = activityRecordRepository.validateActivityRecord(activityRecord)
                if (isValid) {
                    val result = activityRecordRepository.updateActivityRecord(activityRecord)
                    if (result.isSuccess) {
                        if (activityRecord.organizationId?.isNotBlank() == true) {
                            syncRepository.uploadActivityRecord(activityRecord)
                        }
                        _successMessage.value = "Registro atualizado com sucesso"
                        _errorMessage.value = null
                        
                        // Recarregar registros se um veículo estiver selecionado
                        _selectedVehicle.value?.let { vehicle ->
                            loadActivityRecordsForVehicle(vehicle.id)
                            loadLastMileageForVehicle(vehicle.plate)
                        }
                    } else {
                        _errorMessage.value = "Erro ao atualizar registro"
                    }
                } else {
                    _errorMessage.value = "Dados do registro inválidos"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao atualizar registro: ${e.message}"
            }
        }
    }
    
    fun deleteActivityRecord(activityRecord: ActivityRecord) {
        viewModelScope.launch {
            try {
                val result = activityRecordRepository.deleteActivityRecord(activityRecord)
                if (result.isSuccess) {
                    if (activityRecord.organizationId?.isNotBlank() == true) {
                        syncRepository.deleteActivityRecord(activityRecord)
                    }
                    _successMessage.value = "Registro excluído com sucesso"
                    _errorMessage.value = null
                    
                    // Recarregar registros se um veículo estiver selecionado
                    _selectedVehicle.value?.let { vehicle ->
                        loadActivityRecordsForVehicle(vehicle.id)
                        loadLastMileageForVehicle(vehicle.plate)
                    }
                } else {
                    _errorMessage.value = "Erro ao excluir registro"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao excluir registro: ${e.message}"
            }
        }
    }

    /**
     * Aciona a sincronização manual (usado pelo pull-to-refresh)
     */
    fun triggerRefresh() {
        viewModelScope.launch {
            Log.d("DiaryViewModel", "triggerRefresh() called. vehicles=${vehicles.value.size}")
            _isRefreshing.value = true
            try {
                syncRepository.syncVehiclesForAllOrganizations()
                syncRepository.syncActivityRecordsForAllOrganizations()
                loadVehiclesForDiary()
                _selectedVehicle.value?.let { vehicle ->
                    loadActivityRecordsForVehicle(vehicle.id)
                    loadLastMileageForVehicle(vehicle.plate)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro durante a sincronização: ${e.message}"
                Log.e("DiaryViewModel", "Erro durante triggerRefresh", e)
            } finally {
                _isRefreshing.value = false
                Log.d("DiaryViewModel", "triggerRefresh() finished")
            }
        }
    }
    
    // ===== UTILITÁRIOS =====
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
    
    fun refreshData() {
        loadVehiclesForDiary()
        _selectedVehicle.value?.let { vehicle ->
            loadActivityRecordsForVehicle(vehicle.id)
            loadLastMileageForVehicle(vehicle.plate)
        }
    }
    
    // ===== ESTATÍSTICAS =====
    
    suspend fun getVehicleStatistics(vehicleId: Long): VehicleDiaryStatistics {
        return try {
            val totalActivities = activityRecordRepository.getActivityCountByVehicle(vehicleId)
            val totalDistance = activityRecordRepository.getTotalDistanceByVehicle(vehicleId)
            
            VehicleDiaryStatistics(
                totalActivities = totalActivities,
                totalDistance = totalDistance
            )
        } catch (e: Exception) {
            println("Erro ao calcular estatísticas: ${e.message}")
            VehicleDiaryStatistics(0, 0L)
        }
    }
}

/**
 * Estatísticas de um veículo no diário
 */
data class VehicleDiaryStatistics(
    val totalActivities: Int,
    val totalDistance: Long
)
