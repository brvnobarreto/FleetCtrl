package dev.barreto.fleetctrl.data.firestore.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import java.time.LocalDateTime
import java.time.ZoneId

data class OrganizationFirestore(
    val id: String = "",
    val code: String = "",
    val name: String = "",
    val description: String? = null,
    val ownerId: String = "",
    val ownerEmail: String = "",
    @get:PropertyName("isActive") @set:PropertyName("isActive")
    var activeFlag: Boolean = true,
    @get:PropertyName("active") @set:PropertyName("active")
    var legacyActive: Boolean? = null,
    val maxMembers: Int? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val memberCount: Int = 0
) {
    companion object {
        fun fromLocal(organization: dev.barreto.fleetctrl.data.database.entities.Organization): OrganizationFirestore {
            return OrganizationFirestore(
                id = organization.id,
                code = organization.code,
                name = organization.name,
                description = organization.description,
                ownerId = organization.ownerId,
                ownerEmail = organization.ownerEmail,
                activeFlag = organization.isActive,
                maxMembers = organization.maxMembers,
                createdAt = organization.createdAt.toTimestamp(),
                updatedAt = organization.updatedAt.toTimestamp(),
                memberCount = 0 // Será calculado separadamente
            )
        }
    }
    
    fun toLocal(): dev.barreto.fleetctrl.data.database.entities.Organization {
        return dev.barreto.fleetctrl.data.database.entities.Organization(
            id = this.id,
            code = this.code,
            name = this.name,
            description = this.description,
            ownerId = this.ownerId,
            ownerEmail = this.ownerEmail,
            isActive = this.activeFlag || (this.legacyActive == true),
            maxMembers = this.maxMembers,
            createdAt = this.createdAt?.toLocalDateTime() ?: LocalDateTime.now(),
            updatedAt = this.updatedAt?.toLocalDateTime() ?: LocalDateTime.now(),
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
