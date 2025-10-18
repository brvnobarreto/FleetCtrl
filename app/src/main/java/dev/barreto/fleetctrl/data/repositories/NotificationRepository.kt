package dev.barreto.fleetctrl.data.repositories

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import dev.barreto.fleetctrl.data.database.daos.NotificationDao
import dev.barreto.fleetctrl.data.database.entities.Notification
import dev.barreto.fleetctrl.data.database.entities.NotificationType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val notificationDao: NotificationDao
) {
    private fun anyToLocalDateTime(value: Any?): java.time.LocalDateTime? {
        return when (value) {
            is com.google.firebase.Timestamp -> {
                val date = value.toDate()
                java.time.LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault())
            }
            is java.util.Date -> {
                java.time.LocalDateTime.ofInstant(value.toInstant(), java.time.ZoneId.systemDefault())
            }
            is Map<*, *> -> {
                val seconds = (value["seconds"] as? Number)?.toLong()
                val nanos = (value["nanoseconds"] as? Number)?.toInt()
                if (seconds != null) {
                    val instant = java.time.Instant.ofEpochSecond(seconds, (nanos ?: 0).toLong())
                    java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
                } else null
            }
            is Long -> { // epoch millis
                val instant = java.time.Instant.ofEpochMilli(value)
                java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
            }
            is String -> {
                // Tenta ISO_INSTANT primeiro, depois ISO_LOCAL_DATE_TIME
                try {
                    val instant = java.time.Instant.parse(value)
                    java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
                } catch (_: Exception) {
                    try {
                        java.time.LocalDateTime.parse(value)
                    } catch (_: Exception) {
                        null
                    }
                }
            }
            else -> null
        }
    }

    private fun documentToNotification(doc: DocumentSnapshot): Notification? {
        val data = doc.data ?: return null
        return try {
            Notification(
                id = doc.id,
                userId = data["userId"] as? String ?: return null,
                organizationId = data["organizationId"] as? String ?: "",
                type = run {
                    val typeStr = data["type"] as? String ?: "JOIN_REQUEST"
                    try { NotificationType.valueOf(typeStr) } catch (_: Exception) { NotificationType.JOIN_REQUEST }
                },
                title = data["title"] as? String ?: "",
                message = data["message"] as? String ?: "",
                relatedUserId = data["relatedUserId"] as? String,
                relatedUserName = data["relatedUserName"] as? String,
                relatedUserEmail = data["relatedUserEmail"] as? String,
                actionData = data["actionData"] as? String,
                isRead = (data["isRead"] as? Boolean) ?: (data["read"] as? Boolean) ?: false,
                isActionable = data["isActionable"] as? Boolean ?: true,
                createdAt = anyToLocalDateTime(data["createdAt"]) ?: java.time.LocalDateTime.now(),
                readAt = anyToLocalDateTime(data["readAt"]),
                isSynced = true,
                lastSyncAt = java.time.LocalDateTime.now()
            )
        } catch (_: Exception) {
            null
        }
    }

    fun observeUserNotifications(userId: String): Flow<List<Notification>> = callbackFlow {
        val registration = firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList()).isSuccess
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { documentToNotification(it) } ?: emptyList()
                // Atualiza cache local em background e emite
                launch(Dispatchers.IO) {
                    try {
                        if (list.isNotEmpty()) {
                            notificationDao.insertNotifications(list)
                        }
                    } catch (_: Exception) {}
                }
                trySend(list).isSuccess
            }
        awaitClose { registration.remove() }
    }
    
    suspend fun getUserNotifications(userId: String): List<Notification> {
        return try {
            // Primeiro, buscar do banco local
            val localNotifications = notificationDao.getUserNotifications(userId)
            
            // Depois, sincronizar com o Firestore
            syncNotificationsFromFirestore(userId)
            
            // Retornar notificações atualizadas
            notificationDao.getUserNotifications(userId)
        } catch (e: Exception) {
            // Em caso de erro, retornar dados locais
            notificationDao.getUserNotifications(userId)
        }
    }
    
    suspend fun createNotification(notification: Notification) {
        try {
            // Salvar no Firestore
            firestore.collection("notifications")
                .document(notification.id)
                .set(notification)
                .await()
            
            // Salvar no banco local
            notificationDao.insertNotification(notification)
        } catch (e: Exception) {
            // Em caso de erro, salvar apenas localmente
            notificationDao.insertNotification(notification)
        }
    }
    
    suspend fun markAsRead(notificationId: String) {
        try {
            // Atualizar no Firestore
            firestore.collection("notifications")
                .document(notificationId)
                .update("isRead", true, "readAt", System.currentTimeMillis())
                .await()
            
            // Atualizar no banco local
            notificationDao.markAsRead(notificationId)
        } catch (e: Exception) {
            // Em caso de erro, atualizar apenas localmente
            notificationDao.markAsRead(notificationId)
        }
    }
    
    suspend fun deleteNotification(notificationId: String) {
        try {
            // Deletar do Firestore
            firestore.collection("notifications")
                .document(notificationId)
                .delete()
                .await()
            
            // Deletar do banco local
            notificationDao.deleteNotification(notificationId)
        } catch (e: Exception) {
            // Em caso de erro, deletar apenas localmente
            notificationDao.deleteNotification(notificationId)
        }
    }
    
    suspend fun approveJoinRequest(notification: Notification) {
        // Esta função será implementada quando criarmos o sistema de convites
        // Por enquanto, apenas marca como lida
        markAsRead(notification.id)
    }
    
    suspend fun rejectJoinRequest(notification: Notification) {
        // Esta função será implementada quando criarmos o sistema de convites
        // Por enquanto, apenas marca como lida
        markAsRead(notification.id)
    }
    
    private suspend fun syncNotificationsFromFirestore(userId: String) {
        try {
            val snapshot = firestore.collection("notifications")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            fun anyToLocalDateTime(value: Any?): java.time.LocalDateTime? {
                return when (value) {
                    is com.google.firebase.Timestamp -> {
                        val date = value.toDate()
                        java.time.LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault())
                    }
                    is java.util.Date -> {
                        java.time.LocalDateTime.ofInstant(value.toInstant(), java.time.ZoneId.systemDefault())
                    }
                    is Map<*, *> -> {
                        val seconds = (value["seconds"] as? Number)?.toLong()
                        val nanos = (value["nanoseconds"] as? Number)?.toInt()
                        if (seconds != null) {
                            val instant = java.time.Instant.ofEpochSecond(seconds, (nanos ?: 0).toLong())
                            java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
                        } else null
                    }
                    is Long -> { // epoch millis
                        val instant = java.time.Instant.ofEpochMilli(value)
                        java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
                    }
                    is String -> {
                        // Tenta ISO_INSTANT primeiro, depois ISO_LOCAL_DATE_TIME
                        try {
                            val instant = java.time.Instant.parse(value)
                            java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
                        } catch (_: Exception) {
                            try {
                                java.time.LocalDateTime.parse(value)
                            } catch (_: Exception) {
                                null
                            }
                        }
                    }
                    else -> null
                }
            }

            snapshot.documents.forEach { doc ->
                val data = doc.data ?: return@forEach
                try {
                    val notification = Notification(
                        id = doc.id,
                        userId = data["userId"] as? String ?: return@forEach,
                        organizationId = data["organizationId"] as? String ?: "",
                        type = run {
                            val typeStr = data["type"] as? String ?: "JOIN_REQUEST"
                            try { NotificationType.valueOf(typeStr) } catch (_: Exception) { NotificationType.JOIN_REQUEST }
                        },
                        title = data["title"] as? String ?: "",
                        message = data["message"] as? String ?: "",
                        relatedUserId = data["relatedUserId"] as? String,
                        relatedUserName = data["relatedUserName"] as? String,
                        relatedUserEmail = data["relatedUserEmail"] as? String,
                        actionData = data["actionData"] as? String,
                        isRead = (data["isRead"] as? Boolean)
                            ?: (data["read"] as? Boolean)
                            ?: false,
                        isActionable = data["isActionable"] as? Boolean ?: true,
                        createdAt = anyToLocalDateTime(data["createdAt"]) ?: java.time.LocalDateTime.now(),
                        readAt = anyToLocalDateTime(data["readAt"]),
                        isSynced = true,
                        lastSyncAt = java.time.LocalDateTime.now()
                    )
                    notificationDao.insertNotification(notification)
                } catch (_: Exception) {
                    // Ignora documento malformado
                }
            }
        } catch (e: Exception) {
            // Falha silenciosa - usar dados locais
        }
    }
}