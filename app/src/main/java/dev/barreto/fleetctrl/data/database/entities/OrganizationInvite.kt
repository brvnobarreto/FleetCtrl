package dev.barreto.fleetctrl.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "organization_invites",
    foreignKeys = [
        ForeignKey(
            entity = Organization::class,
            parentColumns = ["id"],
            childColumns = ["organizationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["organizationId"]),
        Index(value = ["invitedUserId"]),
        Index(value = ["invitedByUserId"]),
        Index(value = ["status"])
    ]
)
data class OrganizationInvite(
    @PrimaryKey
    val id: String, // ID único do convite
    val organizationId: String, // ID da organização
    val organizationName: String, // Nome da organização (para exibição)
    val invitedUserId: String, // ID do usuário convidado
    val invitedUserEmail: String, // Email do usuário convidado
    val invitedUserName: String? = null, // Nome do usuário convidado
    val invitedByUserId: String, // ID do usuário que enviou o convite
    val invitedByUserEmail: String, // Email do usuário que enviou o convite
    val invitedByUserName: String? = null, // Nome do usuário que enviou o convite
    val message: String? = null, // Mensagem opcional do convite
    val status: InviteStatus = InviteStatus.PENDING, // Status do convite
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val expiresAt: LocalDateTime? = null, // Data de expiração (opcional)
    val isSynced: Boolean = false,
    val lastSyncAt: LocalDateTime? = null
)

enum class InviteStatus {
    PENDING,    // Aguardando aprovação
    APPROVED,   // Aprovado pelo dono
    REJECTED,   // Rejeitado pelo dono
    EXPIRED,    // Expirado
    CANCELLED   // Cancelado pelo convidado
}