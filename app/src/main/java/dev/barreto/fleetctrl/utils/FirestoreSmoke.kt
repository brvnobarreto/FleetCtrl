// utils/FirestoreSmoke.kt
package dev.barreto.fleetctrl.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

suspend fun smokeTestWriteVehicle(orgId: String) {
  val uid = FirebaseAuth.getInstance().currentUser?.uid ?: error("No auth")
  println("SMOKE uid=$uid orgId=$orgId")
  val db = FirebaseFirestore.getInstance()

  // Ensure membership doc exists and is active
  val memberDocId = "${uid}_${orgId}"
  val memberRef = db.collection("user_organizations").document(memberDocId)
  val memberSnap = memberRef.get(Source.SERVER).await()
  if (!memberSnap.exists()) {
    memberRef.set(
      mapOf(
        "userId" to uid,
        "organizationId" to orgId,
        "role" to "viewer",
        "isActive" to true,
        "joinedAt" to FieldValue.serverTimestamp()
      )
    ).await()
    println("SMOKE created user_organizations/$memberDocId")
  }

  val data = mapOf(
    "organizationId" to orgId,
    "plate" to "TEST1234",
    "model" to "Smoke",
    "isActive" to true
  )
  val ref = db.collection("organizations")
    .document(orgId)
    .collection("vehicles")
    .add(data)
    .await()
  println("SMOKE OK add organizations/$orgId/vehicles/${ref.id}")
}
