package dev.barreto.fleetctrl.data.repositories

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import android.net.Uri
import java.io.File
import dev.barreto.fleetctrl.data.database.daos.*
import dev.barreto.fleetctrl.data.database.entities.ActivityRecord
import dev.barreto.fleetctrl.data.database.entities.FuelRecord
import dev.barreto.fleetctrl.data.database.entities.MaintenanceRecord
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import dev.barreto.fleetctrl.data.firestore.models.*
import dev.barreto.fleetctrl.data.preferences.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val vehicleDao: VehicleDao,
    private val fuelRecordDao: FuelRecordDao,
    private val activityRecordDao: ActivityRecordDao,
    private val maintenanceRecordDao: MaintenanceRecordDao,
    private val organizationDao: OrganizationDao,
    private val appPreferences: AppPreferences
) {
    companion object { private const val TAG = "SyncRepository" }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val pushSyncFlow = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 32)

    // Realtime listeners
    private var vehiclesListener: ListenerRegistration? = null
    private var fuelListener: ListenerRegistration? = null
    private var activityListener: ListenerRegistration? = null
    private var maintenanceListener: ListenerRegistration? = null

    init {
        scope.launch {
            @OptIn(FlowPreview::class)
            pushSyncFlow
                .debounce(750)
                .onEach { (scopeKey, orgId) ->
                    when (scopeKey) {
                        "vehicles" -> syncVehicles(orgId)
                        "fuel" -> syncFuelRecords(orgId)
                        "activity" -> syncActivityRecords(orgId)
                        "maintenance" -> syncMaintenanceRecords(orgId)
                    }
                }
                .collect { }
        }
    }

    fun triggerScopeSync(scopeKey: String, orgId: String) {
        pushSyncFlow.tryEmit(scopeKey to orgId)
    }

    private suspend fun isUploadEnabled(): Boolean = appPreferences.uploadLocalToCloud.first()

    private suspend fun canWriteToOrg(organizationId: String): Boolean {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        // 1) Owner direto da organização
        try {
            val orgSnap = firestore.collection("organizations").document(organizationId).get(Source.SERVER).await()
            if (orgSnap.exists() && orgSnap.getString("ownerId") == uid) return true
        } catch (_: Exception) {}

        // 2) Vínculo local (tolerante a legado)
        val localLink = organizationDao.getUserOrganization(uid, organizationId)
        val localRole = localLink?.role?.lowercase()
        if (localRole == "owner" || localRole == "editor") return true

        // 3) Vínculo remoto (tolerante a isActive ausente)
        return try {
            val memberDocId = "${uid}_${organizationId}"
            val snap = firestore.collection("user_organizations").document(memberDocId).get(Source.SERVER).await()
            if (!snap.exists()) return false
            val remoteRole = (snap.getString("role") ?: "").lowercase()
            val remoteActive = snap.getBoolean("isActive")
            (remoteRole == "owner" || remoteRole == "editor") && (remoteActive != false)
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun ensureRemoteMembership(organizationId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Log.w(TAG, "ensureRemoteMembership: no user; orgId=$organizationId")
            return
        }
        val localLink = organizationDao.getUserOrganization(uid, organizationId)
        if (localLink == null || !localLink.isActive) {
            Log.w(TAG, "ensureRemoteMembership: inactive or missing local link; proceeding to create remote viewer link; uid=$uid orgId=$organizationId")
        }

        val memberDocId = "${uid}_${organizationId}"
        val ref = firestore.collection("user_organizations").document(memberDocId)
        val snap = ref.get(Source.SERVER).await()

        // Determina role desejado considerando ownership da organização
        var desiredRole = (localLink?.role ?: "viewer").ifBlank { "viewer" }
        try {
            val orgSnap = firestore.collection("organizations").document(organizationId).get(Source.SERVER).await()
            if (orgSnap.exists() && orgSnap.getString("ownerId") == uid) {
                desiredRole = "owner"
            }
        } catch (e: Exception) {
            Log.w(TAG, "ensureRemoteMembership: failed to fetch organization for role check: ${e.message}")
        }

        if (!snap.exists()) {
            val data = hashMapOf(
                "userId" to uid,
                "organizationId" to organizationId,
                "role" to desiredRole,
                "isActive" to true,
                "joinedAt" to FieldValue.serverTimestamp()
            )
            ref.set(data, SetOptions.merge()).await()
            Log.d(TAG, "ensureRemoteMembership: created remote link uid=$uid orgId=$organizationId role=${localLink?.role}")
        } else if (snap.getBoolean("isActive") != true) {
            ref.update(mapOf("isActive" to true)).await()
            Log.d(TAG, "ensureRemoteMembership: reactivated remote link uid=$uid orgId=$organizationId")
        } else {
            val currentRemoteRole = (snap.getString("role") ?: "").lowercase()
            if ((desiredRole == "owner" || desiredRole == "editor") && currentRemoteRole != desiredRole) {
                try {
                    ref.update(mapOf("role" to desiredRole, "isActive" to true)).await()
                    Log.d(TAG, "ensureRemoteMembership: promoted role to $desiredRole in user_organizations uid=$uid orgId=$organizationId")
                } catch (e: Exception) {
                    Log.w(TAG, "ensureRemoteMembership: failed to update role: ${e.message}")
                }
            }
            Log.d(TAG, "ensureRemoteMembership: remote link already active uid=$uid orgId=$organizationId role=${snap.getString("role")}")
        }
    }

    // ===== REALTIME: VEHICLES =====
    fun startVehiclesRealtime(organizationId: String) {
        // Stop previous, if any
        vehiclesListener?.remove()
        val ref = firestore.collection("organizations")
            .document(organizationId)
            .collection("vehicles")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(100)

        vehiclesListener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("SyncRepository", "vehicles listener error: ${error.message}", error)
                return@addSnapshotListener
            }
            if (snapshot == null) return@addSnapshotListener

            scope.launch {
                snapshot.documentChanges.forEach { change ->
                    when (change.type) {
                        DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                            try {
                                val fs = change.document.toObject(VehicleFirestore::class.java)
                                val idFromDoc = change.document.getLong("id") ?: change.document.id.toLongOrNull() ?: 0L
                                if (fs.isDeleted == true) {
                                    // Propaga remoção local
                                    if (idFromDoc != 0L) vehicleDao.deleteVehicleById(idFromDoc)
                                } else {
                                    val local = fs.copy(id = idFromDoc).toLocal()
                                    val existing = vehicleDao.getVehicleByPlate(local.plate)
                                    if (existing == null) {
                                        vehicleDao.insertVehicle(local)
                                    } else {
                                        vehicleDao.updateVehicle(local.copy(id = existing.id))
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("SyncRepository", "vehicles listener upsert error: ${e.message}", e)
                            }
                        }
                        DocumentChange.Type.REMOVED -> {
                            try {
                                val idFromDoc = change.document.getLong("id") ?: change.document.id.toLongOrNull()
                                if (idFromDoc != null) {
                                    vehicleDao.deleteVehicleById(idFromDoc)
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("SyncRepository", "vehicles listener delete error: ${e.message}", e)
                            }
                        }
                    }
                }
                try { appPreferences.setLastSyncAt("vehicles_pull", organizationId, java.time.LocalDateTime.now()) } catch (_: Exception) {}
            }
        }
    }

    fun startActivityRealtime(organizationId: String, vehicleId: Long) {
        activityListener?.remove()
        val ref = firestore.collection("organizations")
            .document(organizationId)
            .collection("vehicles")
            .document(vehicleId.toString())
            .collection("activity_records")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(100)

        activityListener = ref.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            scope.launch {
                snapshot.documentChanges.forEach { change ->
                    when (change.type) {
                        DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                            val fs = change.document.toObject(ActivityRecordFirestore::class.java)
                            val local = fs.toLocal()
                            val existing = activityRecordDao.getActivityRecordById(local.id)
                            if (existing == null) activityRecordDao.insertActivityRecord(local) else activityRecordDao.updateActivityRecord(local)
                        }
                        DocumentChange.Type.REMOVED -> {
                            change.document.id.toLongOrNull()?.let { activityRecordDao.deleteActivityRecordById(it) }
                        }
                    }
                }
                try { appPreferences.setLastSyncAt("activity_pull", organizationId, java.time.LocalDateTime.now()) } catch (_: Exception) {}
            }
        }
    }

    fun startFuelRealtime(organizationId: String, vehicleId: Long) {
        fuelListener?.remove()
        val ref = firestore.collection("organizations")
            .document(organizationId)
            .collection("vehicles")
            .document(vehicleId.toString())
            .collection("fuel_records")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(100)

        fuelListener = ref.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            scope.launch {
                snapshot.documentChanges.forEach { change ->
                    when (change.type) {
                        DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                            val fs = change.document.toObject(FuelRecordFirestore::class.java)
                            val local = fs.toLocal()
                            val existing = fuelRecordDao.getFuelRecordById(local.id)
                            if (existing == null) fuelRecordDao.insertFuelRecord(local) else fuelRecordDao.updateFuelRecord(local)
                        }
                        DocumentChange.Type.REMOVED -> {
                            change.document.id.toLongOrNull()?.let { fuelRecordDao.deleteFuelRecordById(it) }
                        }
                    }
                }
                try { appPreferences.setLastSyncAt("fuel_pull", organizationId, java.time.LocalDateTime.now()) } catch (_: Exception) {}
            }
        }
    }

    fun startMaintenanceRealtime(organizationId: String, vehicleId: Long) {
        maintenanceListener?.remove()
        val ref = firestore.collection("organizations")
            .document(organizationId)
            .collection("vehicles")
            .document(vehicleId.toString())
            .collection("maintenance_records")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(100)

        maintenanceListener = ref.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            scope.launch {
                snapshot.documentChanges.forEach { change ->
                    when (change.type) {
                        DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                            val fs = change.document.toObject(MaintenanceRecordFirestore::class.java)
                            val local = fs.toLocal()
                            val existing = maintenanceRecordDao.getMaintenanceRecordById(local.id)
                            if (existing == null) maintenanceRecordDao.insertMaintenanceRecord(local) else maintenanceRecordDao.updateMaintenanceRecord(local)
                        }
                        DocumentChange.Type.REMOVED -> {
                            change.document.id.toLongOrNull()?.let { maintenanceRecordDao.deleteMaintenanceRecordById(it) }
                        }
                    }
                }
                try { appPreferences.setLastSyncAt("maintenance_pull", organizationId, java.time.LocalDateTime.now()) } catch (_: Exception) {}
            }
        }
    }

    fun stopAllRealtime() {
        vehiclesListener?.remove(); vehiclesListener = null
        activityListener?.remove(); activityListener = null
        fuelListener?.remove(); fuelListener = null
        maintenanceListener?.remove(); maintenanceListener = null
    }

    fun stopVehiclesRealtime() {
        vehiclesListener?.remove()
        vehiclesListener = null
    }

    suspend fun uploadVehicle(vehicle: Vehicle) = withContext(Dispatchers.IO) {
        require(!vehicle.organizationId.isNullOrBlank()) { "organizationId é obrigatório" }
        val enabled = isUploadEnabled()
        Log.d(TAG, "uploadVehicle: id=${vehicle.id} plate=${vehicle.plate} orgId=${vehicle.organizationId} enabled=$enabled")
        if (!enabled) return@withContext
        if (!canWriteToOrg(vehicle.organizationId!!)) {
            Log.w(TAG, "uploadVehicle: user cannot write to org ${vehicle.organizationId}")
            return@withContext
        }
        ensureRemoteMembership(vehicle.organizationId)
        // Não utilizar Firebase Storage: preferir URL direta se já for http(s)
        val base = VehicleFirestore.fromLocal(vehicle)
        val vehicleFirestore = base
        val ref = firestore.collection("organizations").document(vehicle.organizationId)
            .collection("vehicles").document(vehicle.id.toString())
        ref.set(vehicleFirestore, SetOptions.merge()).await()
        ref.update(mapOf("updatedAt" to FieldValue.serverTimestamp())).await()
        Log.d(TAG, "uploadVehicle: done path=organizations/${vehicle.organizationId}/vehicles/${vehicle.id}")
        fetchVehicleDoc(vehicle.organizationId, vehicle.id)
    }

    // Upload desativado: sem Firebase Storage; URLs remotas devem ser usadas diretamente
    private suspend fun uploadVehiclePhotoIfNeeded(vehicle: Vehicle): String? = null

    suspend fun deleteVehicle(vehicle: Vehicle) = withContext(Dispatchers.IO) {
        try {
            if (vehicle.organizationId.isNullOrBlank()) {
                Log.w(TAG, "deleteVehicle: no orgId; id=${vehicle.id} skipping remote delete")
                return@withContext
            }
            if (!isUploadEnabled()) {
                Log.w(TAG, "deleteVehicle: upload disabled; id=${vehicle.id} skipping remote delete")
                return@withContext
            }
            
            Log.d(TAG, "deleteVehicle: start id=${vehicle.id} orgId=${vehicle.organizationId}")
            ensureRemoteMembership(vehicle.organizationId)
            
            val ref = firestore.collection("organizations").document(vehicle.organizationId)
                .collection("vehicles").document(vehicle.id.toString())
            
            Log.d(TAG, "deleteVehicle: soft-delete path=organizations/${vehicle.organizationId}/vehicles/${vehicle.id}")
            // Soft delete: marca como isDeleted=true e atualiza updatedAt para propagar via listener com limite
            ref.set(
                mapOf(
                    "isDeleted" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
            Log.d(TAG, "deleteVehicle: soft-delete success id=${vehicle.id}")

            // Após propagar o soft-delete, remove definitivamente o documento
            try {
                // Pequeno atraso para garantir que listeners recebam o MODIFIED antes do DELETE
                kotlinx.coroutines.delay(300)
                ref.delete().await()
                Log.d(TAG, "deleteVehicle: hard-delete success id=${vehicle.id}")
            } catch (e: Exception) {
                Log.w(TAG, "deleteVehicle: hard-delete failed id=${vehicle.id} msg=${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteVehicle: error id=${vehicle.id} msg=${e.message}", e)
            throw e
        }
    }

    suspend fun uploadFuelRecord(record: FuelRecord) = withContext(Dispatchers.IO) {
        require(!record.organizationId.isNullOrBlank()) { "organizationId é obrigatório" }
        if (!isUploadEnabled()) return@withContext
        val orgId = record.organizationId!!
        if (!canWriteToOrg(orgId)) return@withContext
        ensureRemoteMembership(orgId)
        val fs = FuelRecordFirestore.fromLocal(record)
        val ref = firestore.collection("organizations").document(orgId)
            .collection("vehicles").document((record.vehicleId ?: 0L).toString())
            .collection("fuel_records").document(record.id.toString())
        ref.set(fs, SetOptions.merge()).await()
        ref.update(mapOf("updatedAt" to FieldValue.serverTimestamp())).await()
        fetchFuelDoc(orgId, record.id)
    }
    suspend fun deleteFuelRecord(record: FuelRecord) = withContext(Dispatchers.IO) {
        if (record.organizationId.isNullOrBlank()) return@withContext
        if (!isUploadEnabled()) return@withContext
        val orgId = record.organizationId!!
        ensureRemoteMembership(orgId)
        val ref = firestore.collection("organizations").document(orgId)
            .collection("vehicles").document((record.vehicleId ?: 0L).toString())
            .collection("fuel_records").document(record.id.toString())
        // Soft-delete + hard-delete
        ref.set(mapOf("isDeleted" to true, "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge()).await()
        try { kotlinx.coroutines.delay(300); ref.delete().await() } catch (_: Exception) {}
    }
    suspend fun uploadActivityRecord(record: ActivityRecord) = withContext(Dispatchers.IO) {
        require(!record.organizationId.isNullOrBlank()) { "organizationId é obrigatório" }
        if (!isUploadEnabled()) return@withContext
        val orgId = record.organizationId!!
        if (!canWriteToOrg(orgId)) return@withContext
        ensureRemoteMembership(orgId)
        val fs = ActivityRecordFirestore.fromLocal(record)
        val ref = firestore.collection("organizations").document(orgId)
            .collection("vehicles").document((record.vehicleId ?: 0L).toString())
            .collection("activity_records").document(record.id.toString())
        ref.set(fs, SetOptions.merge()).await()
        ref.update(mapOf("updatedAt" to FieldValue.serverTimestamp())).await()
        fetchActivityDoc(orgId, record.id)
    }
    suspend fun deleteActivityRecord(record: ActivityRecord) = withContext(Dispatchers.IO) {
        if (record.organizationId.isNullOrBlank()) return@withContext
        if (!isUploadEnabled()) return@withContext
        val orgId = record.organizationId!!
        ensureRemoteMembership(orgId)
        val ref = firestore.collection("organizations").document(orgId)
            .collection("vehicles").document((record.vehicleId ?: 0L).toString())
            .collection("activity_records").document(record.id.toString())
        ref.set(mapOf("isDeleted" to true, "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge()).await()
        try { kotlinx.coroutines.delay(300); ref.delete().await() } catch (_: Exception) {}
    }
    suspend fun uploadMaintenanceRecord(record: MaintenanceRecord) = withContext(Dispatchers.IO) {
        require(!record.organizationId.isNullOrBlank()) { "organizationId é obrigatório" }
        if (!isUploadEnabled()) return@withContext
        val orgId = record.organizationId!!
        if (!canWriteToOrg(orgId)) return@withContext
        ensureRemoteMembership(orgId)
        val fs = MaintenanceRecordFirestore.fromLocal(record)
        val ref = firestore.collection("organizations").document(orgId)
            .collection("vehicles").document((record.vehicleId ?: 0L).toString())
            .collection("maintenance_records").document(record.id.toString())
        ref.set(fs, SetOptions.merge()).await()
        ref.update(mapOf("updatedAt" to FieldValue.serverTimestamp())).await()
        fetchMaintenanceDoc(orgId, record.id)
    }
    suspend fun deleteMaintenanceRecord(record: MaintenanceRecord) = withContext(Dispatchers.IO) {
        if (record.organizationId.isNullOrBlank()) return@withContext
        if (!isUploadEnabled()) return@withContext
        val orgId = record.organizationId!!
        ensureRemoteMembership(orgId)
        val ref = firestore.collection("organizations").document(orgId)
            .collection("vehicles").document((record.vehicleId ?: 0L).toString())
            .collection("maintenance_records").document(record.id.toString())
        ref.set(mapOf("isDeleted" to true, "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge()).await()
        try { kotlinx.coroutines.delay(300); ref.delete().await() } catch (_: Exception) {}
    }

    suspend fun syncOrganizationData(organizationId: String) = withContext(Dispatchers.IO) {
        ensureRemoteMembership(organizationId)
        android.util.Log.d(TAG, "syncOrganizationData: start orgId=$organizationId")
        syncVehicles(organizationId)
        syncFuelRecords(organizationId)
        syncActivityRecords(organizationId)
        syncMaintenanceRecords(organizationId)
        android.util.Log.d(TAG, "syncOrganizationData: end orgId=$organizationId")
    }

    private suspend fun syncVehicles(organizationId: String) {
        try {
            // Buscar veículos da organização no Firestore (coleção aninhada)
            val vehiclesRef = firestore.collection("organizations")
                .document(organizationId)
                .collection("vehicles")

            val snapshot = vehiclesRef
                .whereEqualTo("isDeleted", false)
                .get()
                .await()
            
            val firestoreVehicles = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(VehicleFirestore::class.java)?.copy(
                        id = doc.getLong("id") ?: 0L
                    )
                } catch (e: Exception) {
                    println("Erro ao converter documento de veículo: ${e.message}")
                    null
                }
            }
            
            // Converter para entidades locais e salvar no banco local
            val localVehicles = firestoreVehicles.map { firestoreVehicle ->
                firestoreVehicle.toLocal()
            }
            
            // Salvar no banco local
            localVehicles.forEach { vehicle ->
                try {
                    // Verificar se o veículo já existe localmente
                    val existingVehicle = vehicleDao.getVehicleByPlate(vehicle.plate)
                    if (existingVehicle != null) {
                        // Atualizar veículo existente (preservando o ID local)
                        val updatedVehicle = vehicle.copy(id = existingVehicle.id)
                        vehicleDao.updateVehicle(updatedVehicle)
                    } else {
                        // Inserir novo veículo
                        vehicleDao.insertVehicle(vehicle)
                    }
                } catch (e: Exception) {
                    println("Erro ao salvar veículo ${vehicle.plate}: ${e.message}")
                }
            }
            
            println("Sincronização de veículos concluída: ${localVehicles.size} veículos processados")
            try {
                appPreferences.setLastSyncAt("vehicles_pull", organizationId, java.time.LocalDateTime.now())
            } catch (_: Exception) {}
            
        } catch (e: Exception) {
            println("Erro na sincronização de veículos: ${e.message}")
            throw e
        }
    }

    private suspend fun syncFuelRecords(organizationId: String) {
        // Implementação
    }

    private suspend fun syncActivityRecords(organizationId: String) {
        // Implementação
    }

    private suspend fun syncMaintenanceRecords(organizationId: String) {
        // Implementação
    }

    suspend fun syncVehiclesForAllOrganizations() = withContext(Dispatchers.IO) {
        organizationDao.getAllActiveOrganizations().first().forEach { syncVehicles(it.id) }
    }

    suspend fun syncFuelRecordsForAllOrganizations() = withContext(Dispatchers.IO) {
        organizationDao.getAllActiveOrganizations().first().forEach { syncFuelRecords(it.id) }
    }

    suspend fun syncActivityRecordsForAllOrganizations() = withContext(Dispatchers.IO) {
        organizationDao.getAllActiveOrganizations().first().forEach { syncActivityRecords(it.id) }
    }

    suspend fun syncMaintenanceRecordsForAllOrganizations() = withContext(Dispatchers.IO) {
        organizationDao.getAllActiveOrganizations().first().forEach { syncMaintenanceRecords(it.id) }
    }

    suspend fun syncVehiclesOnly(organizationId: String) = withContext(Dispatchers.IO) {
        syncVehicles(organizationId)
    }

    suspend fun syncFuelOnly(organizationId: String) = withContext(Dispatchers.IO) {
        syncFuelRecords(organizationId)
    }

    suspend fun syncActivityOnly(organizationId: String) = withContext(Dispatchers.IO) {
        syncActivityRecords(organizationId)
    }

    suspend fun syncMaintenanceOnly(organizationId: String) = withContext(Dispatchers.IO) {
        syncMaintenanceRecords(organizationId)
    }
    
    // ===== SINCRONIZAÇÃO LOCAL PARA NUVEM =====
    
    suspend fun syncLocalDataToCloud(organizationId: String, force: Boolean = false) = withContext(Dispatchers.IO) {
        try {
            if (!force && !isUploadEnabled()) {
                println("Upload local->nuvem desabilitado por preferência. Abortando sync manual.")
                return@withContext
            }
            if (!canWriteToOrg(organizationId)) {
                Log.w(TAG, "syncLocalDataToCloud: user cannot write to org $organizationId; skipping push")
                return@withContext
            }
            ensureRemoteMembership(organizationId)
            pushVehiclesToCloud(organizationId)
            pushFuelRecordsToCloud(organizationId)
            pushActivityRecordsToCloud(organizationId)
            pushMaintenanceRecordsToCloud(organizationId)
            try {
                appPreferences.setLastSyncAt("push", organizationId, java.time.LocalDateTime.now())
            } catch (_: Exception) {}
            println("Sincronização local para nuvem concluída para organização $organizationId")
        } catch (e: Exception) {
            println("Erro na sincronização local para nuvem: ${e.message}")
            throw e
        }
    }
    
    private suspend fun pushVehiclesToCloud(organizationId: String) {
        try {
            val localVehicles = vehicleDao.getVehiclesByOrganization(organizationId)
            val firestoreVehicles = localVehicles.map { vehicle ->
                VehicleFirestore.fromLocal(vehicle)
            }
            
            val batch = firestore.batch()
            val vehiclesRef = firestore.collection("organizations")
                .document(organizationId)
                .collection("vehicles")

            localVehicles.zip(firestoreVehicles).forEach { (local, remote) ->
                val docRef = vehiclesRef.document(local.id.toString())
                batch.set(docRef, remote, SetOptions.merge())
            }
            batch.commit().await()
            
            println("${localVehicles.size} veículos sincronizados para a nuvem")
            try {
                appPreferences.setLastSyncAt("vehicles_push", organizationId, java.time.LocalDateTime.now())
            } catch (_: Exception) {}
        } catch (e: Exception) {
            println("Erro ao sincronizar veículos para a nuvem: ${e.message}")
            throw e
        }
    }
    
    private suspend fun pushFuelRecordsToCloud(organizationId: String) {
        try {
            val localRecords = fuelRecordDao.getFuelRecordsByOrganization(organizationId)
            val firestoreRecords = localRecords.map { record ->
                FuelRecordFirestore.fromLocal(record)
            }
            
            val batch = firestore.batch()
            localRecords.zip(firestoreRecords).forEach { (local, remote) ->
                val vehicleId = local.vehicleId ?: 0L
                if (vehicleId > 0) {
                    val docRef = firestore.collection("organizations")
                        .document(organizationId)
                        .collection("vehicles")
                        .document(vehicleId.toString())
                        .collection("fuel_records")
                        .document(local.id.toString())
                    batch.set(docRef, remote, SetOptions.merge())
                }
            }
            batch.commit().await()
            
            println("${localRecords.size} registros de combustível sincronizados para a nuvem")
        } catch (e: Exception) {
            println("Erro ao sincronizar registros de combustível para a nuvem: ${e.message}")
            throw e
        }
    }
    
    private suspend fun pushActivityRecordsToCloud(organizationId: String) {
        try {
            val localRecords = activityRecordDao.getActivityRecordsByOrganization(organizationId)
            val firestoreRecords = localRecords.map { record ->
                ActivityRecordFirestore.fromLocal(record)
            }
            
            val batch = firestore.batch()
            localRecords.zip(firestoreRecords).forEach { (local, remote) ->
                val vehicleId = local.vehicleId ?: 0L
                if (vehicleId > 0) {
                    val docRef = firestore.collection("organizations")
                        .document(organizationId)
                        .collection("vehicles")
                        .document(vehicleId.toString())
                        .collection("activity_records")
                        .document(local.id.toString())
                    batch.set(docRef, remote, SetOptions.merge())
                }
            }
            batch.commit().await()
            
            println("${localRecords.size} registros de atividade sincronizados para a nuvem")
        } catch (e: Exception) {
            println("Erro ao sincronizar registros de atividade para a nuvem: ${e.message}")
            throw e
        }
    }
    
    private suspend fun pushMaintenanceRecordsToCloud(organizationId: String) {
        try {
            val localRecords = maintenanceRecordDao.getMaintenanceRecordsByOrganization(organizationId)
            val firestoreRecords = localRecords.map { record ->
                MaintenanceRecordFirestore.fromLocal(record)
            }
            
            val batch = firestore.batch()
            localRecords.zip(firestoreRecords).forEach { (local, remote) ->
                val vehicleId = local.vehicleId ?: 0L
                if (vehicleId > 0) {
                    val docRef = firestore.collection("organizations")
                        .document(organizationId)
                        .collection("vehicles")
                        .document(vehicleId.toString())
                        .collection("maintenance_records")
                        .document(local.id.toString())
                    batch.set(docRef, remote, SetOptions.merge())
                }
            }
            batch.commit().await()
            
            println("${localRecords.size} registros de manutenção sincronizados para a nuvem")
        } catch (e: Exception) {
            println("Erro ao sincronizar registros de manutenção para a nuvem: ${e.message}")
            throw e
        }
    }

    private suspend fun fetchVehicleDoc(orgId: String, id: Long) {
        try {
            val ref = firestore.collection("organizations")
                .document(orgId)
                .collection("vehicles")
                .document(id.toString())
            val snap = ref.get(Source.SERVER).await()
            if (snap.exists()) {
                val fs = snap.toObject(VehicleFirestore::class.java) ?: return
                val local = fs.toLocal()
                val existing = vehicleDao.getVehicleByPlate(local.plate)
                if (existing == null) {
                    vehicleDao.insertVehicle(local)
                } else {
                    vehicleDao.updateVehicle(local.copy(id = existing.id))
                }
            }
        } catch (e: Exception) {
            println("Erro ao atualizar doc de veículo $id: ${e.message}")
        }
    }

    private suspend fun fetchFuelDoc(orgId: String, id: Long) {
        // Este método exigiria o vehicleId para path completo; manter no ar por enquanto
    }

    private suspend fun fetchActivityDoc(orgId: String, id: Long) {
        // Idem: path depende de vehicleId
    }

    private suspend fun fetchMaintenanceDoc(orgId: String, id: Long) {
        // Idem
    }
}
