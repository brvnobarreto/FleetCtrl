package dev.barreto.fleetctrl.data.firestore

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

object TombstoneHelpers {
    suspend fun tombstoneFuelRecord(db: FirebaseFirestore, orgId: String, vehicleId: Long, recordId: Long) {
        val ref = db.collection("organizations")
            .document(orgId)
            .collection("vehicles")
            .document(vehicleId.toString())
            .collection("fueling_records")
            .document(recordId.toString())
        ref.set(mapOf(
            "organizationId" to orgId,
            "vehicleId" to vehicleId,
            "isDeleted" to true,
            "updatedAt" to FieldValue.serverTimestamp()
        ), SetOptions.merge()).await()
    }

    suspend fun tombstoneActivityRecord(db: FirebaseFirestore, orgId: String, vehicleId: Long, recordId: Long) {
        val ref = db.collection("organizations")
            .document(orgId)
            .collection("vehicles")
            .document(vehicleId.toString())
            .collection("diary_activity_records")
            .document(recordId.toString())
        ref.set(mapOf(
            "organizationId" to orgId,
            "vehicleId" to vehicleId,
            "isDeleted" to true,
            "updatedAt" to FieldValue.serverTimestamp()
        ), SetOptions.merge()).await()
    }

    suspend fun tombstoneMaintenanceRecord(db: FirebaseFirestore, orgId: String, vehicleId: Long, recordId: Long) {
        val ref = db.collection("organizations")
            .document(orgId)
            .collection("vehicles")
            .document(vehicleId.toString())
            .collection("maintenance_service_records")
            .document(recordId.toString())
        ref.set(mapOf(
            "organizationId" to orgId,
            "vehicleId" to vehicleId,
            "isDeleted" to true,
            "updatedAt" to FieldValue.serverTimestamp()
        ), SetOptions.merge()).await()
    }
}
