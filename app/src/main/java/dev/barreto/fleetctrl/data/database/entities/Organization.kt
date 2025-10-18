package dev.barreto.fleetctrl.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName
import java.time.LocalDateTime

@Entity(tableName = "organizations")
data class Organization(
    @PrimaryKey
    val id: String = "", // ID único da organização
    
    val code: String = "", // Código da organização (ex: "ABC123")
    val name: String = "", // Nome da organização
    val description: String? = null, // Descrição opcional
    val ownerId: String = "", // ID do usuário que criou a organização
    val ownerEmail: String = "", // Email do dono para exibição
    
    // Configurações da organização
    val isActive: Boolean = true, // Se a organização está ativa
    val maxMembers: Int? = null, // Limite de membros (null = ilimitado)
    val requiresApproval: Boolean = true, // Se requer aprovação manual para entrada
    
    // Metadados
    @PropertyName("createdAt")
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @PropertyName("updatedAt")
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    
    // Sincronização
    val isSynced: Boolean = false, // Se foi sincronizado com Firestore
    @PropertyName("lastSyncAt")
    val lastSyncAt: LocalDateTime? = null // Última sincronização
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
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
        isSynced = false,
        lastSyncAt = null
    )
}