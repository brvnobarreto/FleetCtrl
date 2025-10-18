package dev.barreto.fleetctrl.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import dev.barreto.fleetctrl.data.repositories.SyncRepository
import dev.barreto.fleetctrl.data.repositories.NotificationRepository
import dev.barreto.fleetctrl.data.database.entities.Notification
import dev.barreto.fleetctrl.data.database.entities.NotificationType
import dev.barreto.fleetctrl.utils.NotificationHelper

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
    @Inject lateinit var notificationRepository: NotificationRepository

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val scope = remoteMessage.data["scope"]
        val orgId = remoteMessage.data["orgId"]
        val type = remoteMessage.data["type"] // e.g., maintenance_due, user_joined
        val title = remoteMessage.data["title"]
        val message = remoteMessage.data["message"]
        val userId = remoteMessage.data["userId"]

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
                // Criação de notificação local (in-app + push) se dados presentes
                if (!userId.isNullOrBlank() && !type.isNullOrBlank() && !title.isNullOrBlank() && !message.isNullOrBlank()) {
                    val notif = Notification(
                        id = System.currentTimeMillis().toString(),
                        userId = userId,
                        organizationId = orgId,
                        type = when (type.lowercase()) {
                            "maintenance_due" -> NotificationType.MAINTENANCE_DUE
                            "user_joined" -> NotificationType.JOIN_APPROVED
                            else -> NotificationType.SYSTEM_UPDATE
                        },
                        title = title,
                        message = message,
                        isActionable = false
                    )
                    try { notificationRepository.createNotification(notif) } catch (_: Exception) {}
                    NotificationHelper.show(
                        context = this@PushSyncMessagingService,
                        notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                        title = title,
                        message = message
                    )
                }
            } catch (_: Exception) {
                // Evita crash; logs podem ser adicionados conforme necessidade
            }
        }
    }
}
