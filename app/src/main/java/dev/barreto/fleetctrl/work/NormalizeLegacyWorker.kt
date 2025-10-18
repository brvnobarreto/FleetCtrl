package dev.barreto.fleetctrl.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.barreto.fleetctrl.data.database.daos.OrganizationDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

class NormalizeLegacyWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DepsEntryPoint {
        fun organizationDao(): OrganizationDao
        fun firestore(): FirebaseFirestore
        fun auth(): FirebaseAuth
    }

    private val deps: DepsEntryPoint by lazy {
        EntryPointAccessors.fromApplication(applicationContext, DepsEntryPoint::class.java)
    }

    override suspend fun doWork(): Result {
        val db = deps.firestore()
        val uid = deps.auth().currentUser?.uid ?: return Result.success()

        // Carrega todas as orgs ativas que o usuário participa
        val orgs = deps.organizationDao().getAllActiveOrganizations().first()
        for (org in orgs) {
            val link = deps.organizationDao().getUserOrganization(uid, org.id)
            val role = link?.role?.lowercase()
            val canWriteSub = role == "owner" || role == "editor"
            val canWriteOrg = role == "owner"

            try {
                if (canWriteOrg) normalizeOrganizationActive(db, org.id)
                if (canWriteSub) {
                    normalizeVehiclesActive(db, org.id)
                    normalizeLegacyDeleted(db, org.id, "fueling_records")
                    normalizeLegacyDeleted(db, org.id, "diary_activity_records")
                    normalizeLegacyDeleted(db, org.id, "maintenance_service_records")
                }
            } catch (_: Exception) { }
        }

        return Result.success()
    }

    private suspend fun normalizeOrganizationActive(db: FirebaseFirestore, orgId: String) {
        val doc = db.collection("organizations").document(orgId).get().await()
        if (doc.exists() && doc.get("active") != null) {
            val v = (doc.get("active") as? Boolean) ?: return
            doc.reference.set(mapOf("isActive" to v, "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge()).await()
        }
    }

    private suspend fun normalizeVehiclesActive(db: FirebaseFirestore, orgId: String) {
        // active == true
        val col = db.collection("organizations").document(orgId).collection("vehicles")
        val q1 = col.whereEqualTo("active", true).get().await()
        for (d in q1.documents) {
            d.reference.set(mapOf("isActive" to true, "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge()).await()
        }
        // active == false
        val q2 = col.whereEqualTo("active", false).get().await()
        for (d in q2.documents) {
            d.reference.set(mapOf("isActive" to false, "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge()).await()
        }
    }

    private suspend fun normalizeLegacyDeleted(db: FirebaseFirestore, orgId: String, group: String) {
        // Define isDeleted=true onde deleted==true
        val snap = db.collectionGroup(group)
            .whereEqualTo("organizationId", orgId)
            .whereEqualTo("deleted", true)
            .limit(200)
            .get()
            .await()
        for (d in snap.documents) {
            val vId = (d.get("vehicleId") as? Number)?.toLong()
            val payload = hashMapOf<String, Any>(
                "isDeleted" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (vId != null) payload["vehicleId"] = vId
            payload["organizationId"] = orgId
            d.reference.set(payload, SetOptions.merge()).await()
        }
    }
}
