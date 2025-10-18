package dev.barreto.fleetctrl.data.firestore.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import dev.barreto.fleetctrl.data.database.entities.ActivityRecord
import java.time.LocalDateTime
import java.time.ZoneId

data class ActivityRecordFirestore(
    val id: Long = 0,
    val vehicleId: Long = 0,
    val plate: String = "",
    val driver: String = "",
    val date: Timestamp? = null,
    val startMileage: Long = 0,
    val endMileage: Long = 0,
    val observation: String? = null,
    val organizationId: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    @get:PropertyName("isDeleted") @set:PropertyName("isDeleted")
    var deletedFlag: Boolean = false,
    @get:PropertyName("deleted") @set:PropertyName("deleted")
    var legacyDeleted: Boolean? = null
) {
    companion object {
        fun fromLocal(activityRecord: ActivityRecord): ActivityRecordFirestore {
            return ActivityRecordFirestore(
                id = activityRecord.id,
                vehicleId = activityRecord.vehicleId ?: 0L,
                plate = activityRecord.plate,
                driver = activityRecord.driver,
                date = activityRecord.date.toTimestamp(),
                startMileage = activityRecord.startMileage,
                endMileage = activityRecord.endMileage,
                observation = activityRecord.observation,
                organizationId = activityRecord.organizationId ?: "",
                createdAt = activityRecord.createdAt.toTimestamp(),
                updatedAt = activityRecord.updatedAt.toTimestamp(),
                deletedFlag = false
            )
        }
    }
    
    fun toLocal(): ActivityRecord {
        return ActivityRecord(
            id = this.id,
            vehicleId = this.vehicleId.takeIf { it > 0 },
            plate = this.plate,
            driver = this.driver,
            date = this.date?.toLocalDateTime() ?: LocalDateTime.now(),
            startMileage = this.startMileage,
            endMileage = this.endMileage,
            observation = this.observation,
            organizationId = this.organizationId,
            createdAt = this.createdAt?.toLocalDateTime() ?: LocalDateTime.now(),
            updatedAt = this.updatedAt?.toLocalDateTime() ?: LocalDateTime.now()
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
