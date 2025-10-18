package dev.barreto.fleetctrl.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.barreto.fleetctrl.data.repositories.HomeRepository
import dev.barreto.fleetctrl.data.repositories.HomeSummary
import dev.barreto.fleetctrl.utils.DataRefreshNotifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private var currentOrganizationId: String? = null

    init {
        observeDataRefresh()
    }
    
    private fun observeDataRefresh() {
        DataRefreshNotifier.refreshTrigger
            .onEach {
                refreshSummary(currentOrganizationId)
            }
            .launchIn(viewModelScope)
    }

    fun setOrganizationId(organizationId: String?) {
        if (currentOrganizationId != organizationId) {
            currentOrganizationId = organizationId
            refreshSummary(organizationId)
        }
    }

    fun refreshSummary(currentOrganizationId: String?) {
        this.currentOrganizationId = currentOrganizationId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val summary = homeRepository.loadSummary(currentOrganizationId)
                _uiState.value = HomeUiState(
                    isLoading = false,
                    summary = summary,
                    lastUpdated = summary.lastUpdated
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

data class HomeUiState(
    val isLoading: Boolean = false,
    val summary: HomeSummary? = null,
    val errorMessage: String? = null,
    val lastUpdated: LocalDateTime? = null
)
