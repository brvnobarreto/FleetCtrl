package dev.barreto.fleetctrl.data.models

import java.util.Date

/**
 * Modelo de dados simplificado para Notification
 */
data class Notification(
    val id: String = "",
    val userId: String = "",
    val organizationId: String = "",
    val type: String = "", // JOIN_REQUEST, JOIN_APPROVED, JOIN_REJECTED
    val title: String = "",
    val message: String = "",
    val relatedUserId: String = "",
    val relatedUserName: String = "",
    val relatedUserEmail: String = "",
    val actionData: String = "",
    val isRead: Boolean = false,
    val isActionable: Boolean = true,
    val createdAt: Date = Date(),
    val readAt: Date? = null,
    val isSynced: Boolean = false,
    val lastSyncAt: Date? = null
) {
    // Construtor vazio para deserialização do Firestore
    constructor() : this(
        id = "",
        userId = "",
        organizationId = "",
        type = "",
        title = "",
        message = "",
        relatedUserId = "",
        relatedUserName = "",
        relatedUserEmail = "",
        actionData = "",
        isRead = false,
        isActionable = true,
        createdAt = Date(),
        readAt = null,
        isSynced = false,
        lastSyncAt = null
    )
}
