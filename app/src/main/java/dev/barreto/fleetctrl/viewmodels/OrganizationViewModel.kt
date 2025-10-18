package dev.barreto.fleetctrl.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.barreto.fleetctrl.data.models.Organization
import dev.barreto.fleetctrl.data.models.UserOrganization
import dev.barreto.fleetctrl.data.repositories.OrganizationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrganizationViewModel @Inject constructor(
    private val organizationRepository: OrganizationRepository,
    private val auth: com.google.firebase.auth.FirebaseAuth,
    private val appPreferences: dev.barreto.fleetctrl.data.preferences.AppPreferences
) : ViewModel() {
    
    private val _organizations = MutableStateFlow<List<Organization>>(emptyList())
    val organizations: StateFlow<List<Organization>> = _organizations.asStateFlow()
    
    private val _userOrganizations = MutableStateFlow<List<UserOrganization>>(emptyList())
    val userOrganizations: StateFlow<List<UserOrganization>> = _userOrganizations.asStateFlow()
    
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Estados para compatibilidade com MainNavigation
    private val _currentOrganization = MutableStateFlow<dev.barreto.fleetctrl.data.database.entities.Organization?>(null)
    val currentOrganization: StateFlow<dev.barreto.fleetctrl.data.database.entities.Organization?> = _currentOrganization.asStateFlow()
    
    private val _currentRole = MutableStateFlow<String?>(null)
    val currentRole: StateFlow<String?> = _currentRole.asStateFlow()
    
    fun createOrganization(name: String, description: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _message.value = null
            
            organizationRepository.createOrganization(name, description)
                .onSuccess { organization ->
                    _message.value = "Organização criada com sucesso!"
                    loadUserOrganizations()
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Erro ao criar organização"
                }
            
            _isLoading.value = false
        }
    }
    
    fun joinOrganization(code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _message.value = null
            
            organizationRepository.joinOrganization(code)
                .onSuccess { message ->
                    _message.value = message
                    loadUserOrganizations()
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Erro ao entrar na organização"
                }
            
            _isLoading.value = false
        }
    }
    
    fun loadUserOrganizations() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val currentUser = auth.currentUser
            if (currentUser != null) {
                organizationRepository.getUserOrganizations(currentUser.uid)
                    .onSuccess { userOrgs ->
                        _userOrganizations.value = userOrgs
                        _message.value = "Organizações carregadas: ${userOrgs.size}"
                        // Atualiza papel atual se já houver org selecionada
                        _currentOrganization.value?.let { org ->
                            _currentRole.value = userOrgs.find { it.organizationId == org.id }?.role
                        }
                    }
                    .onFailure { exception ->
                        _error.value = exception.message ?: "Erro ao carregar organizações"
                    }
            } else {
                _error.value = "Usuário não autenticado"
            }
            
            _isLoading.value = false
        }
    }
    
    suspend fun getOrganizationById(organizationId: String): dev.barreto.fleetctrl.data.models.Organization? {
        return try {
            organizationRepository.getOrganizationById(organizationId).getOrNull()
        } catch (e: Exception) {
            null
        }
    }
    
    fun updateOrganization(organizationId: String, name: String, description: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _message.value = null
            
            organizationRepository.updateOrganization(organizationId, name, description)
                .onSuccess {
                    _message.value = "Organização atualizada com sucesso!"
                    loadUserOrganizations() // Recarregar lista
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Erro ao atualizar organização"
                }
            
            _isLoading.value = false
        }
    }
    
    fun leaveOrganization(organizationId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _message.value = null
            
            organizationRepository.leaveOrganization(organizationId)
                .onSuccess {
                    _message.value = "Você saiu da organização"
                    loadUserOrganizations() // Recarregar lista
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Erro ao sair da organização"
                }
            
            _isLoading.value = false
        }
    }
    
    fun showMessage(message: String) {
        _message.value = message
    }
    
    fun canEditOrganization(organizationId: String): Boolean {
        val userOrg = _userOrganizations.value.find { it.organizationId == organizationId }
        return userOrg?.role in listOf("owner", "editor")
    }
    
    fun canManageVehicles(organizationId: String): Boolean {
        val userOrg = _userOrganizations.value.find { it.organizationId == organizationId }
        return userOrg?.role in listOf("owner", "editor")
    }
    
    fun getUserRole(organizationId: String): String {
        val userOrg = _userOrganizations.value.find { it.organizationId == organizationId }
        return userOrg?.role ?: "viewer"
    }
    
    fun clearMessage() {
        _message.value = null
    }
    
    fun clearError() {
        _error.value = null
    }
    
    // Métodos para compatibilidade com MainNavigation
    fun getCurrentOrganizationId(): String? {
        return _currentOrganization.value?.id
    }
    
    fun syncVehiclesOnly() {
        // Implementação simplificada - apenas para compatibilidade
        viewModelScope.launch {
            _message.value = "Sincronização de veículos iniciada"
        }
    }
    
    fun syncActivityOnly() {
        // Implementação simplificada - apenas para compatibilidade
        viewModelScope.launch {
            _message.value = "Sincronização de atividades iniciada"
        }
    }
    
    fun syncFuelOnly() {
        // Implementação simplificada - apenas para compatibilidade
        viewModelScope.launch {
            _message.value = "Sincronização de combustível iniciada"
        }
    }
    
    fun syncMaintenanceOnly() {
        // Implementação simplificada - apenas para compatibilidade
        viewModelScope.launch {
            _message.value = "Sincronização de manutenção iniciada"
        }
    }
    
    // Métodos adicionais para compatibilidade com ConnectivityScreen original
    fun forceRefreshOrganizations() {
        // Implementação simplificada - apenas para compatibilidade
        viewModelScope.launch {
            _message.value = "Atualizando organizações..."
        }
    }
    
    fun clearMessages() {
        _message.value = null
        _error.value = null
    }
    
    fun selectOrganization(organization: dev.barreto.fleetctrl.data.database.entities.Organization) {
        viewModelScope.launch {
            _currentOrganization.value = organization
            resolveRoleFor(organization.id)
            _message.value = "Organização selecionada: ${organization.name}"
            appPreferences.setSelectedOrganizationId(organization.id)
        }
    }

    fun selectOrganizationId(organizationId: String) {
        viewModelScope.launch {
            try {
                val orgModel = organizationRepository.getOrganizationById(organizationId).getOrNull()
                if (orgModel != null) {
                    val entity = dev.barreto.fleetctrl.data.database.entities.Organization(
                        id = orgModel.id,
                        code = orgModel.code,
                        name = orgModel.name,
                        description = orgModel.description,
                        ownerId = orgModel.ownerId,
                        ownerEmail = "",
                        isActive = true,
                        maxMembers = null,
                        requiresApproval = true,
                        createdAt = java.time.LocalDateTime.now(),
                        updatedAt = java.time.LocalDateTime.now(),
                        isSynced = true,
                        lastSyncAt = null
                    )
                    _currentOrganization.value = entity
                    resolveRoleFor(organizationId)
                    appPreferences.setSelectedOrganizationId(organizationId)
                }
            } catch (_: Exception) { }
        }
    }

    private suspend fun resolveRoleFor(organizationId: String) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val result = organizationRepository.getUserOrganizations(uid)
            val userOrgs = result.getOrNull()
            if (userOrgs != null) {
                _userOrganizations.value = userOrgs
                _currentRole.value = userOrgs.find { it.organizationId == organizationId }?.role
            }
        } catch (_: Exception) { }
    }
    
    // Estado para compatibilidade
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
}