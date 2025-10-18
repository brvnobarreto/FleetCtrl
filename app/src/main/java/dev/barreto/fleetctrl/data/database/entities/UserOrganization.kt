package dev.barreto.fleetctrl.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.time.LocalDateTime

@Entity(
    tableName = "user_organizations",
    primaryKeys = ["userId", "organizationId"],
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
        Index(value = ["organizationId"])
    ]
)
data class UserOrganization(
    val userId: String, // ID do usuário Firebase
    val organizationId: String, // ID da organização
    val role: String = "member", // Papel do usuário (owner, admin, member)
    val joinedAt: LocalDateTime = LocalDateTime.now(), // Quando entrou
    val isActive: Boolean = true, // Se ainda está ativo na organização
    val userEmail: String? = null, // Email do usuário para exibição

    // Sincronização
    val isSynced: Boolean = false,
    val lastSyncAt: LocalDateTime? = null
)
