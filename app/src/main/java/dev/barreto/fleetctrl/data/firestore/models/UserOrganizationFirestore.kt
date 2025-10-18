package dev.barreto.fleetctrl.data.firestore.models

import com.google.firebase.Timestamp
import dev.barreto.fleetctrl.data.database.entities.UserOrganization
import java.time.LocalDateTime
import java.time.ZoneId

data class UserOrganizationFirestore(
    val userId: String = "",
    val organizationId: String = "",
    val role: String = "member",
    val joinedAt: Timestamp? = null,
    val isActive: Boolean = true,
    val userEmail: String? = null
) {
    companion object {
        fun fromLocal(userOrganization: UserOrganization): UserOrganizationFirestore {
            return UserOrganizationFirestore(
                userId = userOrganization.userId,
                organizationId = userOrganization.organizationId,
                role = userOrganization.role,
                joinedAt = userOrganization.joinedAt.toTimestamp(),
                isActive = userOrganization.isActive,
                userEmail = userOrganization.userEmail
            )
        }
    }
    
    fun toLocal(): UserOrganization {
        return UserOrganization(
            userId = this.userId,
            organizationId = this.organizationId,
            role = this.role,
            joinedAt = this.joinedAt?.toLocalDateTime() ?: LocalDateTime.now(),
            isActive = this.isActive,
            userEmail = this.userEmail,
            isSynced = true,
            lastSyncAt = LocalDateTime.now()
        )
    }
}

// Extensões para conversão de data/hora
private fun LocalDateTime.toTimestamp(): Timestamp {
    return Timestamp(java.util.Date.from(this.atZone(ZoneId.systemDefault()).toInstant()))
}

private fun Timestamp.toLocalDateTime(): LocalDateTime {
    return this.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
}
