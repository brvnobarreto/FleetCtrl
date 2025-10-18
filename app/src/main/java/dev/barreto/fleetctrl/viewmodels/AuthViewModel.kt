package dev.barreto.fleetctrl.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.barreto.fleetctrl.data.repositories.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    val authRepository: AuthRepository
) : ViewModel() {
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    val currentUser: Flow<FirebaseUser?> = authRepository.currentUser
    
    /**
     * Faz login com Google usando o resultado do ActivityResult
     */
    fun signInWithGoogle(data: android.content.Intent?) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            
            authRepository.signInWithGoogle(data)
                .onSuccess { user ->
                    _successMessage.value = "Login realizado com sucesso"
                    _isLoading.value = false
                }
                .onFailure { exception ->
                    _errorMessage.value = "Erro ao fazer login: ${exception.message}"
                    _isLoading.value = false
                }
        }
    }
    
    /**
     * Obtém o Intent para iniciar o Google Sign-In
     */
    fun getSignInIntent(): android.content.Intent {
        return authRepository.getSignInIntent()
    }
    
    /**
     * Faz logout
     */
    fun signOut() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            
            authRepository.signOut()
                .onSuccess {
                    _successMessage.value = "Logout realizado com sucesso"
                    _isLoading.value = false
                }
                .onFailure { exception ->
                    _errorMessage.value = "Erro ao fazer logout: ${exception.message}"
                    _isLoading.value = false
                }
        }
    }
    
    /**
     * Limpa as mensagens
     */
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
    
    /**
     * Verifica se o usuário está logado
     */
    fun isLoggedIn(): Boolean = authRepository.isLoggedIn()
}