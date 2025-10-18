package dev.barreto.fleetctrl.data.repositories

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dev.barreto.fleetctrl.data.models.Organization
import dev.barreto.fleetctrl.data.models.UserOrganization
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrganizationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val notificationRepository: NotificationRepository
) {
    
    private fun anyToDate(value: Any?): Date {
        return when (value) {
            is com.google.firebase.Timestamp -> value.toDate()
            is Date -> value
            is Map<*, *> -> {
                val seconds = (value["_seconds"] as? Number)?.toLong() ?: 0L
                val nanoseconds = (value["_nanoseconds"] as? Number)?.toLong() ?: 0L
                Date(seconds * 1000 + nanoseconds / 1000000)
            }
            is Number -> Date(value.toLong())
            is String -> {
                try {
                    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault()).parse(value) ?: Date()
                } catch (e: Exception) {
                    Date()
                }
            }
            else -> Date()
        }
    }
    
    private fun documentToOrganization(doc: com.google.firebase.firestore.DocumentSnapshot): Organization {
        return Organization(
            id = doc.id,
            name = doc.getString("name") ?: "",
            description = doc.getString("description") ?: "",
            code = doc.getString("code") ?: "",
            ownerId = doc.getString("ownerId") ?: "",
            createdAt = anyToDate(doc.get("createdAt")),
            updatedAt = anyToDate(doc.get("updatedAt"))
        )
    }
    
    suspend fun createOrganization(name: String, description: String = ""): Result<Organization> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Usuário não autenticado"))
            
            val code = generateUniqueOrganizationCode()
            val now = Date()
            
            val organization = Organization(
                id = "", // Será definido pelo Firestore
                name = name,
                description = description,
                code = code,
                ownerId = currentUser.uid,
                createdAt = now,
                updatedAt = now
            )
            
            val docRef = firestore.collection("organizations").add(organization).await()
            val createdOrg = organization.copy(id = docRef.id)
            
            // Criar vínculo do usuário como owner
            createUserOrganizationLink(
                userId = currentUser.uid,
                organizationId = createdOrg.id,
                role = "owner",
                userEmail = currentUser.email ?: ""
            )
            
            Result.success(createdOrg)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getOrganizationByCode(code: String): Result<Organization?> {
        return try {
            val query = firestore.collection("organizations")
                .whereEqualTo("code", code)
                .limit(1)
            
            val snapshot = query.get().await()
            
            if (snapshot.isEmpty) {
                Result.success(null)
            } else {
                val doc = snapshot.documents.first()
                val organization = documentToOrganization(doc)
                Result.success(organization)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getOrganizationById(organizationId: String): Result<Organization> {
        return try {
            val doc = firestore.collection("organizations")
                .document(organizationId)
                .get()
                .await()
            
            if (doc.exists()) {
                val organization = documentToOrganization(doc)
                Result.success(organization)
            } else {
                Result.failure(Exception("Organização não encontrada"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun joinOrganization(code: String): Result<String> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Usuário não autenticado"))
            
            // Buscar organização pelo código
            val orgResult = getOrganizationByCode(code)
            if (orgResult.isFailure) {
                return Result.failure(orgResult.exceptionOrNull() ?: Exception("Erro ao buscar organização"))
            }
            
            val organization = orgResult.getOrNull() ?: return Result.failure(Exception("Organização não encontrada"))
            
            // Verificar se já é membro
            val existingLink = firestore.collection("user_organizations")
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("organizationId", organization.id)
                .limit(1)
                .get()
                .await()
            
            if (!existingLink.isEmpty) {
                return Result.success("Você já é membro desta organização")
            }
            
            // Criar vínculo diretamente
            createUserOrganizationLink(
                userId = currentUser.uid,
                organizationId = organization.id,
                role = "viewer",
                userEmail = currentUser.email ?: ""
            )
            // Criar notificação para o OWNER da organização
            try {
                val title = "Novo membro na organização"
                val message = "${currentUser.email ?: "Um usuário"} entrou na organização ${organization.name}"
                val notif = dev.barreto.fleetctrl.data.database.entities.Notification(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = organization.ownerId,
                    organizationId = organization.id,
                    type = dev.barreto.fleetctrl.data.database.entities.NotificationType.JOIN_APPROVED,
                    title = title,
                    message = message,
                    relatedUserId = currentUser.uid,
                    relatedUserEmail = currentUser.email
                )
                notificationRepository.createNotification(notif)
            } catch (_: Exception) {}
            
            Result.success("Entrada na organização realizada com sucesso!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createUserOrganizationLink(
        userId: String,
        organizationId: String,
        role: String,
        userEmail: String
    ): Result<Unit> {
        return try {
            val userOrg = UserOrganization(
                id = "${userId}_${organizationId}",
                userId = userId,
                organizationId = organizationId,
                role = role,
                joinedAt = Date(),
                userEmail = userEmail
            )
            
            firestore.collection("user_organizations")
                .document(userOrg.id)
                .set(userOrg)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserOrganizations(userId: String): Result<List<UserOrganization>> {
        return try {
            val snapshot = firestore.collection("user_organizations")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val organizations = snapshot.documents.mapNotNull { doc ->
                try {
                    UserOrganization(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        organizationId = doc.getString("organizationId") ?: "",
                        role = doc.getString("role") ?: "viewer",
                        joinedAt = anyToDate(doc.get("joinedAt")),
                        userEmail = doc.getString("userEmail") ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            Result.success(organizations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun generateUniqueOrganizationCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        var code: String
        var isUnique = false
        
        do {
            code = (1..6)
                .map { chars.random() }
                .joinToString("")
            
            val query = firestore.collection("organizations")
                .whereEqualTo("code", code)
                .limit(1)
            
            val snapshot = query.get().await()
            isUnique = snapshot.isEmpty
        } while (!isUnique)
        
        return code
    }
    
    suspend fun updateOrganization(organizationId: String, name: String, description: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Usuário não autenticado"))
            
            // Verificar se é o dono da organização
            val organization = getOrganizationById(organizationId).getOrNull()
                ?: return Result.failure(Exception("Organização não encontrada"))
            
            if (organization.ownerId != currentUser.uid) {
                return Result.failure(Exception("Apenas o dono pode editar a organização"))
            }
            
            firestore.collection("organizations")
                .document(organizationId)
                .update(
                    mapOf(
                        "name" to name,
                        "description" to description,
                        "updatedAt" to Date()
                    )
                )
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun leaveOrganization(organizationId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Usuário não autenticado"))
            
            val userOrgId = "${currentUser.uid}_${organizationId}"
            firestore.collection("user_organizations")
                .document(userOrgId)
                .delete()
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}