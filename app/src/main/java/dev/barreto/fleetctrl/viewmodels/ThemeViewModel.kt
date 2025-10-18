package dev.barreto.fleetctrl.viewmodels

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.barreto.fleetctrl.data.preferences.AppPreferences
import dev.barreto.fleetctrl.ui.theme.ColorPalette
import dev.barreto.fleetctrl.ui.theme.ColorPaletteType
import dev.barreto.fleetctrl.ui.theme.getAvailablePalettes
import dev.barreto.fleetctrl.ui.theme.getAdaptedPaletteForDarkMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val appPreferences: AppPreferences
) : ViewModel() {
    
    var selectedTheme by mutableStateOf("system")
        private set
    
    var selectedPalette by mutableStateOf<ColorPalette?>(null)
        private set
    
    init {
        // Carregar preferências salvas
        loadPreferences()
    }
    
    private fun loadPreferences() {
        viewModelScope.launch {
            // Carregar tema
            selectedTheme = appPreferences.selectedTheme.first()
            
            // Carregar paleta
            val paletteName = appPreferences.selectedPalette.first()
            selectedPalette = if (paletteName != "system") {
                try {
                    val paletteType = ColorPaletteType.valueOf(paletteName)
                    getAvailablePalettes().find { it.type == paletteType }
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }
    }
    
    fun setTheme(theme: String) {
        selectedTheme = theme
        viewModelScope.launch {
            appPreferences.setSelectedTheme(theme)
        }
    }
    
    fun setPalette(palette: ColorPalette?) {
        selectedPalette = palette
        viewModelScope.launch {
            val paletteName = palette?.type?.name ?: "system"
            appPreferences.setSelectedPalette(paletteName)
        }
    }
    
    @Composable
    fun isDarkTheme(): Boolean {
        return when (selectedTheme) {
            "dark" -> true
            "light" -> false
            "system" -> isSystemInDarkTheme()
            else -> isSystemInDarkTheme()
        }
    }
    
    fun getCurrentPalette(): ColorPalette? {
        return selectedPalette
    }
}