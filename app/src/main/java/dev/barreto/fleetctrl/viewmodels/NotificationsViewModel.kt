package dev.barreto.fleetctrl.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.barreto.fleetctrl.data.models.Notification
import dev.barreto.fleetctrl.data.repositories.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()
    
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    fun loadNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // Como não temos mais sistema de notificações complexo,
                // vamos apenas carregar notificações básicas se necessário
                _notifications.value = emptyList()
            } catch (e: Exception) {
                _error.value = e.message ?: "Erro ao carregar notificações"
            }
            
            _isLoading.value = false
        }
    }
    
    fun clearMessage() {
        _message.value = null
    }
    
    fun clearError() {
        _error.value = null
    }
}