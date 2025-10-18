package dev.barreto.fleetctrl.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.barreto.fleetctrl.utils.DatabaseCleaner
import dev.barreto.fleetctrl.data.repositories.OrganizationRepository
import dev.barreto.fleetctrl.data.preferences.AppPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DataManagementViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val organizationRepository: OrganizationRepository
) : ViewModel() {
    
    // Estados
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _dataCleared = MutableStateFlow(false)
    val dataCleared: StateFlow<Boolean> = _dataCleared.asStateFlow()

    // Preferência: enviar dados locais para a nuvem
    val uploadLocalToCloud: Flow<Boolean> = appPreferences.uploadLocalToCloud
    val autoSyncEnabled: Flow<Boolean> = appPreferences.autoSyncEnabled

    // Preferências de conectividade
    val syncWifiOnly: Flow<Boolean> = appPreferences.syncWifiOnly
    val allowMobileData: Flow<Boolean> = appPreferences.allowMobileData
    val offlineMode: Flow<Boolean> = appPreferences.offlineMode
    val uploadQuality: Flow<String> = appPreferences.uploadQuality

    fun setUploadLocalToCloud(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setUploadLocalToCloud(enabled)
        }
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setAutoSyncEnabled(enabled) }
    }

    fun setSyncWifiOnly(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setSyncWifiOnly(enabled) }
    }

    fun setAllowMobileData(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setAllowMobileData(enabled) }
    }

    fun setOfflineMode(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setOfflineMode(enabled) }
    }

    fun setUploadQuality(quality: String) {
        viewModelScope.launch { appPreferences.setUploadQuality(quality) }
    }
    
    /**
     * Limpa todos os dados do banco de dados
     */
    fun clearAllData(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _message.value = null
            
            try {
                // Limpa DB via Repository (DAOs + prefs) - método removido na versão simplificada
                // organizationRepository.clearAllData()
                // Limpa arquivos locais e envia refresh
                val success = DatabaseCleaner.clearAllFiles(context)
                if (success) {
                    _message.value = "✅ Todos os dados foram removidos com sucesso!\n\nNavegue para outras telas para ver as mudanças."
                    _dataCleared.value = true
                    
                    // Aguardar um pouco para o usuário ver a mensagem
                    kotlinx.coroutines.delay(1000)
                } else {
                    _error.value = "❌ Falha ao remover os dados. Verifique os logs para mais detalhes."
                }
            } catch (e: Exception) {
                _error.value = "❌ Erro ao remover dados: ${e.message}"
                e.printStackTrace()
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Limpa apenas dados de exemplo
     */
    fun cleanSampleData(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _message.value = null
            
            try {
                val success = DatabaseCleaner.cleanSampleData(context)
                if (success) {
                    _message.value = "Dados de exemplo removidos com sucesso!"
                } else {
                    _error.value = "Falha ao remover dados de exemplo"
                }
            } catch (e: Exception) {
                _error.value = "Erro ao remover dados de exemplo: ${e.message}"
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Mostra uma mensagem
     */
    fun showMessage(message: String) {
        _message.value = message
    }
    
    /**
     * Limpa as mensagens
     */
    fun clearMessage() {
        _message.value = null
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun clearMessages() {
        _message.value = null
        _error.value = null
    }
    
    fun resetDataCleared() {
        _dataCleared.value = false
    }
}
