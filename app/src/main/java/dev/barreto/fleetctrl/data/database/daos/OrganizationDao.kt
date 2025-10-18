package dev.barreto.fleetctrl.data.database.daos

import androidx.room.*
import dev.barreto.fleetctrl.data.database.entities.Organization
import dev.barreto.fleetctrl.data.database.entities.UserOrganization
import kotlinx.coroutines.flow.Flow

@Dao
interface OrganizationDao {
    
    // ===== ORGANIZATIONS =====
    
    @Query("SELECT * FROM organizations WHERE id = :organizationId")
    suspend fun getOrganizationById(organizationId: String): Organization?
    
    @Query("SELECT * FROM organizations WHERE id = :organizationId")
    fun getOrganizationByIdFlow(organizationId: String): Flow<Organization?>
    
    @Query("SELECT * FROM organizations WHERE ownerId = :ownerId")
    fun getOrganizationsByOwner(ownerId: String): Flow<List<Organization>>
    
    @Query("SELECT * FROM organizations WHERE isActive = 1")
    fun getAllActiveOrganizations(): Flow<List<Organization>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrganization(organization: Organization)
    
    @Update
    suspend fun updateOrganization(organization: Organization)
    
    @Delete
    suspend fun deleteOrganization(organization: Organization)
    
    @Query("DELETE FROM organizations WHERE id = :organizationId")
    suspend fun deleteOrganizationById(organizationId: String)
    
    // ===== USER ORGANIZATIONS =====
    
    @Query("SELECT * FROM user_organizations WHERE userId = :userId AND isActive = 1")
    fun getUserOrganizations(userId: String): Flow<List<UserOrganization>>
    
    @Query("SELECT * FROM user_organizations WHERE organizationId = :organizationId AND isActive = 1")
    fun getOrganizationMembers(organizationId: String): Flow<List<UserOrganization>>
    
    @Query("SELECT * FROM user_organizations WHERE userId = :userId AND organizationId = :organizationId")
    suspend fun getUserOrganization(userId: String, organizationId: String): UserOrganization?
    
    @Query("SELECT * FROM user_organizations WHERE userId = :userId AND organizationId = :organizationId")
    fun getUserOrganizationFlow(userId: String, organizationId: String): Flow<UserOrganization?>
    
    @Update
    suspend fun updateUserOrganization(userOrganization: UserOrganization)
    
    @Delete
    suspend fun deleteUserOrganization(userOrganization: UserOrganization)
    
    @Query("DELETE FROM user_organizations WHERE userId = :userId AND organizationId = :organizationId")
    suspend fun deleteUserOrganizationById(userId: String, organizationId: String)
    
    // ===== QUERIES COMPLEXAS =====
    
    @Query("""
        SELECT o.* FROM organizations o
        INNER JOIN user_organizations uo ON o.id = uo.organizationId
        WHERE uo.userId = :userId AND uo.isActive = 1 AND o.isActive = 1
    """)
    fun getUserOrganizationsWithDetails(userId: String): Flow<List<Organization>>
    
    @Query("""
        SELECT o.* FROM organizations o
        INNER JOIN user_organizations uo ON o.id = uo.organizationId
        WHERE uo.userId = :userId AND uo.organizationId = :organizationId AND uo.isActive = 1
    """)
    suspend fun getUserOrganizationWithDetails(userId: String, organizationId: String): Organization?
    
    // ===== SINCRONIZAÇÃO =====
    
    @Query("SELECT * FROM organizations WHERE isSynced = 0")
    suspend fun getUnsyncedOrganizations(): List<Organization>
    
    @Query("SELECT * FROM user_organizations WHERE isSynced = 0")
    suspend fun getUnsyncedUserOrganizations(): List<UserOrganization>
    
    @Query("UPDATE organizations SET isSynced = 1, lastSyncAt = :syncTime WHERE id = :organizationId")
    suspend fun markOrganizationAsSynced(organizationId: String, syncTime: java.time.LocalDateTime)
    
    @Query("UPDATE user_organizations SET isSynced = 1, lastSyncAt = :syncTime WHERE userId = :userId AND organizationId = :organizationId")
    suspend fun markUserOrganizationAsSynced(userId: String, organizationId: String, syncTime: java.time.LocalDateTime)
    
    @Query("UPDATE user_organizations SET isActive = 0 WHERE userId = :userId")
    suspend fun removeUserFromAllOrganizations(userId: String)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserOrganization(userOrganization: UserOrganization)

    // ===== LIMPEZA TOTAL =====
    @Query("DELETE FROM user_organizations")
    suspend fun deleteAllUserOrganizations()

    @Query("DELETE FROM organizations")
    suspend fun deleteAllOrganizations()

    @Query("SELECT * FROM organizations")
    suspend fun getAllOrganizationsList(): List<Organization>

    @Query("SELECT * FROM user_organizations")
    suspend fun getAllUserOrganizationsList(): List<UserOrganization>
}
