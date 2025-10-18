package dev.barreto.fleetctrl.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "notifications",
    foreignKeys = [
        ForeignKey(
            entity = Organization::class,
            parentColumns = ["id"],
            childColumns = ["organizationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["organizationId"]),
        Index(value = ["type"]),
        Index(value = ["isRead"]),
        Index(value = ["createdAt"])
    ]
)
data class Notification(
    @PrimaryKey
    val id: String, // ID único da notificação
    val userId: String, // ID do usuário que recebe a notificação
    val organizationId: String, // ID da organização relacionada
    val type: NotificationType, // Tipo da notificação
    val title: String, // Título da notificação
    val message: String, // Mensagem da notificação
    val relatedUserId: String? = null, // ID do usuário relacionado (ex: quem solicitou entrada)
    val relatedUserName: String? = null, // Nome do usuário relacionado
    val relatedUserEmail: String? = null, // Email do usuário relacionado
    val actionData: String? = null, // Dados adicionais em JSON para ações
    val isRead: Boolean = false, // Se a notificação foi lida
    val isActionable: Boolean = true, // Se a notificação requer ação do usuário
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val readAt: LocalDateTime? = null, // Quando foi lida
    val isSynced: Boolean = false,
    val lastSyncAt: LocalDateTime? = null
)

enum class NotificationType {
    JOIN_REQUEST, // Solicitação de entrada na organização
    JOIN_APPROVED, // Entrada aprovada
    JOIN_REJECTED, // Entrada rejeitada
    VEHICLE_ADDED, // Veículo adicionado
    VEHICLE_UPDATED, // Veículo atualizado
    VEHICLE_DELETED, // Veículo excluído
    MAINTENANCE_DUE, // Manutenção devida
    SYSTEM_UPDATE // Atualização do sistema
}