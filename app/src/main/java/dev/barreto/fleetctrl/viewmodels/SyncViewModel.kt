package dev.barreto.fleetctrl.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.barreto.fleetctrl.data.repositories.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _realtimeVehiclesActive = MutableStateFlow(false)
    val realtimeVehiclesActive: StateFlow<Boolean> = _realtimeVehiclesActive.asStateFlow()

    fun startRealtime(orgId: String) {
        viewModelScope.launch {
            syncRepository.startVehiclesRealtime(orgId)
            _realtimeVehiclesActive.value = true
        }
    }

    fun stopRealtime() {
        viewModelScope.launch {
            syncRepository.stopVehiclesRealtime()
            _realtimeVehiclesActive.value = false
        }
    }
}
