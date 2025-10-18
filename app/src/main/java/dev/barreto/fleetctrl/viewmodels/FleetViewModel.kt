package dev.barreto.fleetctrl.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import dev.barreto.fleetctrl.data.repositories.FleetRepository
import dev.barreto.fleetctrl.data.repositories.FleetStatistics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import dev.barreto.fleetctrl.utils.DataRefreshNotifier
import javax.inject.Inject

/**
 * ViewModel para gerenciar a frota de veículos
 * Demonstra como integrar Room Database com ViewModels
 */
@HiltViewModel
class FleetViewModel @Inject constructor(
    private val fleetRepository: FleetRepository
) : ViewModel() {
    
    // ID da organização atual
    private var currentOrganizationId: String? = null
    
    // ===== ESTADO DA UI =====
    
    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    private val _fleetStatistics = MutableStateFlow<FleetStatistics?>(null)
    val fleetStatistics: StateFlow<FleetStatistics?> = _fleetStatistics.asStateFlow()
    
    // ===== OPERAÇÕES DE VEÍCULOS =====
    
    init {
        observeVehicles() // Observa mudanças no Flow
        loadFleetStatistics()
        observeDataRefresh() // Observa notificações de refresh
    }
    
    fun observeVehicles() {
        viewModelScope.launch {
            if (currentOrganizationId != null) {
                // Carregar veículos da organização selecionada + locais
                fleetRepository.vehicleRepository.getActiveVehiclesByOrganizationOrLocal(currentOrganizationId!!)
                    .onStart { 
                        _isLoading.value = true
                        println("DEBUG: Iniciando carregamento de veículos da org: $currentOrganizationId")
                    }
                    .catch { exception ->
                        println("DEBUG: Erro ao carregar veículos: ${exception.message}")
                        _errorMessage.value = "Erro ao carregar veículos: ${exception.message}"
                        _isLoading.value = false
                    }
                    .collect { vehicleList ->
                        println("DEBUG: Veículos carregados: ${vehicleList.size} veículos")
                        vehicleList.forEach { vehicle ->
                            println("DEBUG: Veículo: ${vehicle.plate} - ${vehicle.model}")
                        }
                        _vehicles.value = vehicleList
                        _isLoading.value = false
                    }
            } else {
                // Carregar todos os veículos ativos (sem filtro de org)
                fleetRepository.vehicleRepository.getAllVehicles()
                    .onStart { 
                        _isLoading.value = true
                        println("DEBUG: Iniciando carregamento de todos os veículos")
                    }
                    .catch { exception ->
                        println("DEBUG: Erro ao carregar veículos: ${exception.message}")
                        _errorMessage.value = "Erro ao carregar veículos: ${exception.message}"
                        _isLoading.value = false
                    }
                    .collect { vehicleList ->
                        println("DEBUG: Veículos carregados: ${vehicleList.size} veículos")
                        vehicleList.forEach { vehicle ->
                            println("DEBUG: Veículo: ${vehicle.plate} - ${vehicle.model}")
                        }
                        _vehicles.value = vehicleList
                        _isLoading.value = false
                    }
            }
        }
    }
    
    private fun observeDataRefresh() {
        DataRefreshNotifier.refreshTrigger
            .onEach {
                println("DEBUG FleetViewModel: Data refresh triggered, reloading vehicles...")
                observeVehicles() // Recarrega os veículos
                loadFleetStatistics() // Recarrega estatísticas
            }
            .launchIn(viewModelScope)
    }
    
    
    fun loadFleetStatistics() {
        viewModelScope.launch {
            try {
                val stats = fleetRepository.getFleetStatistics()
                _fleetStatistics.value = stats
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao carregar estatísticas: ${e.message}"
            }
        }
    }

    suspend fun addVehicle(vehicle: Vehicle): Boolean {
        return try {
            val orgId = currentOrganizationId
            val vehicleWithOrg = if (vehicle.organizationId.isNullOrBlank() && !orgId.isNullOrBlank()) {
                println("DEBUG addVehicle: organizationId vazio na UI — usando currentOrganizationId=$orgId")
                vehicle.copy(organizationId = orgId)
            } else {
                vehicle
            }

            val vehicleToInsert = vehicleWithOrg.copy(isActive = true)

            println("DEBUG: Iniciando adição de veículo: ${vehicleToInsert.plate}, org=${vehicleToInsert.organizationId}, active=${vehicleToInsert.isActive}")

            val isValid = fleetRepository.vehicleRepository.validateVehicle(vehicleToInsert)
            println("DEBUG: Validação do veículo: $isValid")

            if (isValid) {
                val vehicleId = fleetRepository.vehicleRepository.insertVehicle(vehicleToInsert)
                println("DEBUG: Veículo inserido com ID: $vehicleId")
                // Tentativa de sincronização imediata para a nuvem
                try {
                    currentOrganizationId?.let { orgIdNonNull ->
                        println("DEBUG: Disparando syncLocalDataToCloud imediatamente após inserção")
                        fleetRepository.syncLocalDataToCloud(orgIdNonNull, force = true)
                        // Após push, puxa estado mais recente para refletir na UI
                        fleetRepository.syncOrganizationFromCloud(orgIdNonNull)
                    }
                } catch (syncEx: Exception) {
                    println("DEBUG: Falha ao sincronizar após inserção: ${syncEx.message}")
                }
                _successMessage.value = "Veículo adicionado com sucesso"
                _errorMessage.value = null
                DataRefreshNotifier.triggerRefreshSync()
                true
            } else {
                println("DEBUG: Validação falhou para veículo: ${vehicleToInsert.plate}")
                _errorMessage.value = "Dados do veículo inválidos"
                false
            }
        } catch (e: Exception) {
            println("DEBUG: Erro ao adicionar veículo: ${e.message}")
            _errorMessage.value = "Erro ao adicionar veículo: ${e.message}"
            false
        }
    }

    suspend fun updateVehicle(vehicle: Vehicle): Boolean {
        return try {
            val isValid = fleetRepository.vehicleRepository.validateVehicle(vehicle)
            if (isValid) {
                fleetRepository.vehicleRepository.updateVehicle(vehicle)
                // Tentativa de sincronização imediata após atualização
                try {
                    currentOrganizationId?.let { orgIdNonNull ->
                        println("DEBUG: Disparando syncLocalDataToCloud imediatamente após atualização")
                        fleetRepository.syncLocalDataToCloud(orgIdNonNull, force = true)
                        // Após push, puxa estado mais recente para refletir na UI
                        fleetRepository.syncOrganizationFromCloud(orgIdNonNull)
                    }
                } catch (syncEx: Exception) {
                    println("DEBUG: Falha ao sincronizar após atualização: ${syncEx.message}")
                }
                _successMessage.value = "Veículo atualizado com sucesso"
                _errorMessage.value = null
                DataRefreshNotifier.triggerRefreshSync()
                true
            } else {
                _errorMessage.value = "Dados do veículo inválidos"
                false
            }
        } catch (e: Exception) {
            _errorMessage.value = "Erro ao atualizar veículo: ${e.message}"
            false
        }
    }
    
    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            try {
                fleetRepository.vehicleRepository.deleteVehicle(vehicle)
                _successMessage.value = "Veículo excluído com sucesso"
                _errorMessage.value = null
                DataRefreshNotifier.triggerRefreshSync()
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao excluir veículo: ${e.message}"
            }
        }
    }
    
    fun syncVehicles() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                // Sincronizar veículos da organização atual
                currentOrganizationId?.let { orgId ->
                    fleetRepository.syncVehiclesFromOrganization(orgId)
                    loadVehicles() // Recarregar lista
                    _successMessage.value = "Veículos sincronizados com sucesso"
                } ?: run {
                    _errorMessage.value = "Nenhuma organização selecionada"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao sincronizar veículos: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun syncLocalDataToCloud() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                // Sincronizar dados locais para a nuvem
                currentOrganizationId?.let { orgId ->
                    // 1) Envia alterações locais
                    fleetRepository.syncLocalDataToCloud(orgId, force = true)
                    // 2) Busca estado mais recente da nuvem para refletir imediatamente na UI
                    fleetRepository.syncOrganizationFromCloud(orgId)
                    _successMessage.value = "Dados sincronizados para a nuvem com sucesso"
                } ?: run {
                    _errorMessage.value = "Nenhuma organização selecionada"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao sincronizar dados para a nuvem: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun searchVehicles(query: String) {
        if (query.isBlank()) {
            // Se a busca estiver vazia, volta para a lista completa
            return
        }
        
        viewModelScope.launch {
            try {
                fleetRepository.vehicleRepository.searchVehicles(query)
                    .onStart { _isLoading.value = true }
                    .catch { exception ->
                        _errorMessage.value = "Erro na busca: ${exception.message}"
                        _isLoading.value = false
                    }
                    .collect { searchResults ->
                        _vehicles.value = searchResults
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _errorMessage.value = "Erro na busca: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    // ===== OPERAÇÕES DE QUILOMETRAGEM =====
    
    fun updateVehicleMileage(vehicleId: Long, newMileage: Long) {
        viewModelScope.launch {
            try {
                fleetRepository.vehicleRepository.updateMileage(vehicleId, newMileage)
                _successMessage.value = "Quilometragem atualizada com sucesso"
                _errorMessage.value = null
                DataRefreshNotifier.triggerRefreshSync()
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao atualizar quilometragem: ${e.message}"
            }
        }
    }

    // ===== OPERAÇÕES DE MANUTENÇÃO =====
    
    fun getVehiclesNeedingMaintenance(threshold: Long) {
        viewModelScope.launch {
            try {
                fleetRepository.vehicleRepository.getVehiclesNeedingMaintenance(threshold)
                    .onStart { _isLoading.value = true }
                    .catch { exception ->
                        _errorMessage.value = "Erro ao carregar veículos para manutenção: ${exception.message}"
                        _isLoading.value = false
                    }
                    .collect { vehicles ->
                        _vehicles.value = vehicles
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao carregar veículos para manutenção: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // ===== GERENCIAMENTO DE ORGANIZAÇÃO =====
    
    fun setCurrentOrganization(organizationId: String?) {
        currentOrganizationId = organizationId
        // Recarregar dados com filtro de organização
        observeVehicles()
    }
    
    private fun loadVehicles() {
        viewModelScope.launch {
            try {
                if (currentOrganizationId != null) {
                    // Carregar veículos da organização + locais (organizationId NULL)
                    fleetRepository.vehicleRepository.getActiveVehiclesByOrganizationOrLocal(currentOrganizationId!!)
                        .onStart { _isLoading.value = true }
                        .catch { exception ->
                            _errorMessage.value = "Erro ao carregar veículos da organização: ${exception.message}"
                            _isLoading.value = false
                        }
                        .collect { vehicles ->
                            _vehicles.value = vehicles
                            _isLoading.value = false
                        }
                } else {
                    // Carregar todos os veículos ativos
                    fleetRepository.vehicleRepository.getActiveVehicles()
                        .onStart { _isLoading.value = true }
                        .catch { exception ->
                            _errorMessage.value = "Erro ao carregar veículos: ${exception.message}"
                            _isLoading.value = false
                        }
                        .collect { vehicles ->
                            _vehicles.value = vehicles
                            _isLoading.value = false
                        }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao carregar veículos: ${e.message}"
                _isLoading.value = false
            }
        }
    }
}
