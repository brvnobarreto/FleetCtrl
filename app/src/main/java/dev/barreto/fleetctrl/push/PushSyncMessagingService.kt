package dev.barreto.fleetctrl.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import dev.barreto.fleetctrl.data.repositories.SyncRepository

/**
 * Firebase Messaging Service para receber eventos de "push to sync" específicos
 * data payload esperado:
 *  - scope: "vehicles" | "fuel" | "activity" | "maintenance"
 *  - orgId: ID da organização
 *  - (opcional) vehicleId: Long, para futuramente fazer sync focalizado
 */
@AndroidEntryPoint
class PushSyncMessagingService : FirebaseMessagingService() {

    @Inject lateinit var syncRepository: SyncRepository

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val scope = remoteMessage.data["scope"]
        val orgId = remoteMessage.data["orgId"]

        if (scope.isNullOrBlank() || orgId.isNullOrBlank()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (scope.lowercase()) {
                    "vehicles", "fleet" -> syncRepository.triggerScopeSync("vehicles", orgId)
                    "fuel", "fuel_records" -> syncRepository.triggerScopeSync("fuel", orgId)
                    "activity", "activity_records", "diary" -> syncRepository.triggerScopeSync("activity", orgId)
                    "maintenance", "maintenance_records" -> syncRepository.triggerScopeSync("maintenance", orgId)
                    else -> Unit
                }
            } catch (_: Exception) {
                // Evita crash; logs podem ser adicionados conforme necessidade
            }
        }
    }
}
