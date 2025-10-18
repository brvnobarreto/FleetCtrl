package dev.barreto.fleetctrl.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class OrganizationFirestore(
    @PropertyName("id")
    val id: String = "",
    
    @PropertyName("code")
    val code: String = "",
    
    @PropertyName("name")
    val name: String = "",
    
    @PropertyName("description")
    val description: String? = null,
    
    @PropertyName("ownerId")
    val ownerId: String = "",
    
    @PropertyName("ownerEmail")
    val ownerEmail: String = "",
    
    @PropertyName("isActive")
    val isActive: Boolean = true,
    
    @PropertyName("maxMembers")
    val maxMembers: Int? = null,
    
    @PropertyName("requiresApproval")
    val requiresApproval: Boolean = true,
    
    @PropertyName("createdAt")
    val createdAt: Timestamp? = null,
    
    @PropertyName("updatedAt")
    val updatedAt: Timestamp? = null,
    
    @PropertyName("isSynced")
    val isSynced: Boolean = false,
    
    @PropertyName("lastSyncAt")
    val lastSyncAt: Timestamp? = null
) {
    // Construtor vazio para deserialização do Firestore
    constructor() : this(
        id = "",
        code = "",
        name = "",
        description = null,
        ownerId = "",
        ownerEmail = "",
        isActive = true,
        maxMembers = null,
        requiresApproval = true,
        createdAt = null,
        updatedAt = null,
        isSynced = false,
        lastSyncAt = null
    )
}
