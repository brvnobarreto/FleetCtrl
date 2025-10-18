package dev.barreto.fleetctrl.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Notificador global para refresh de dados após limpeza
 */
object DataRefreshNotifier {
    private val _refreshTrigger = MutableSharedFlow<Unit>()
    val refreshTrigger: SharedFlow<Unit> = _refreshTrigger.asSharedFlow()
    
    /**
     * Dispara um evento de refresh para todas as telas
     */
    suspend fun triggerRefresh() {
        _refreshTrigger.emit(Unit)
    }
    
    /**
     * Versão não-suspend para uso em contextos não-suspend
     */
    fun triggerRefreshSync() {
        CoroutineScope(Dispatchers.Main).launch {
            triggerRefresh()
        }
    }
}