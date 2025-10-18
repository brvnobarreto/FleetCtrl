package dev.barreto.fleetctrl.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.barreto.fleetctrl.data.preferences.AppPreferences
import dev.barreto.fleetctrl.navigation.Screen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val appPreferences: AppPreferences
) : ViewModel() {
    
    var currentScreen by mutableStateOf(Screen.HOME)
        private set
    
    var isProfileMenuExpanded by mutableStateOf(false)
        private set
    
    init {
        // Carregar preferências salvas
        loadPreferences()
    }
    
    private fun loadPreferences() {
        viewModelScope.launch {
            // Carregar tela atual com tratamento de erro
            appPreferences.currentScreen
                .catch { exception ->
                    println("Erro ao carregar tela atual: ${exception.message}")
                    emit(Screen.HOME.name)
                }
                .collect { screenName ->
                    try {
                        currentScreen = Screen.valueOf(screenName)
                    } catch (e: Exception) {
                        currentScreen = Screen.HOME
                    }
                }
            
            // Carregar estado do menu de perfil com tratamento de erro
            appPreferences.isProfileMenuExpanded
                .catch { exception ->
                    println("Erro ao carregar estado do menu: ${exception.message}")
                    emit(false)
                }
                .collect { expanded ->
                    isProfileMenuExpanded = expanded
                }
        }
    }
    
    fun navigateToScreen(screen: Screen) {
        currentScreen = screen
        isProfileMenuExpanded = false
        
        // Salvar preferência
        viewModelScope.launch {
            appPreferences.setCurrentScreen(screen.name)
            appPreferences.setIsProfileMenuExpanded(false)
        }
    }
    
    fun toggleProfileMenu() {
        isProfileMenuExpanded = !isProfileMenuExpanded
        
        // Salvar preferência
        viewModelScope.launch {
            appPreferences.setIsProfileMenuExpanded(isProfileMenuExpanded)
        }
    }
    
    fun closeProfileMenu() {
        isProfileMenuExpanded = false
        
        // Salvar preferência
        viewModelScope.launch {
            appPreferences.setIsProfileMenuExpanded(false)
        }
    }
}
