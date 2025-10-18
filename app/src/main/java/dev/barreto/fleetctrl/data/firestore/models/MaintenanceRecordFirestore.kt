package dev.barreto.fleetctrl.data.firestore.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import dev.barreto.fleetctrl.data.database.entities.MaintenanceRecord
import dev.barreto.fleetctrl.data.database.entities.MaintenanceType
import java.time.LocalDateTime
import java.time.ZoneId

data class MaintenanceRecordFirestore(
    val id: Long = 0,
    val vehicleId: Long = 0,
    val date: Timestamp? = null,
    val type: String = "",
    val description: String = "",
    val mileage: Long = 0,
    val laborCost: Double = 0.0,
    val partsCost: Double = 0.0,
    val totalCost: Double = 0.0,
    val workshop: String? = null,
    val mechanic: String? = null,
    val warrantyUntil: Timestamp? = null,
    val isCompleted: Boolean = true,
    val nextMaintenanceMileage: Long? = null,
    val nextRevisionDate: Timestamp? = null,
    val minRevisionDate: Timestamp? = null,
    val maxRevisionDate: Timestamp? = null,
    val distanceToDealer: Long? = null,
    val organizationId: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    @get:PropertyName("isDeleted") @set:PropertyName("isDeleted")
    var deletedFlag: Boolean = false,
    @get:PropertyName("deleted") @set:PropertyName("deleted")
    var legacyDeleted: Boolean? = null,
    val notes: String? = null
) {
    companion object {
        fun fromLocal(maintenanceRecord: MaintenanceRecord): MaintenanceRecordFirestore {
            return MaintenanceRecordFirestore(
                id = maintenanceRecord.id,
                vehicleId = maintenanceRecord.vehicleId ?: 0L,
                date = maintenanceRecord.date.toTimestamp(),
                type = maintenanceRecord.type.name,
                description = maintenanceRecord.description,
                mileage = maintenanceRecord.mileage,
                laborCost = maintenanceRecord.laborCost.toDouble(),
                partsCost = maintenanceRecord.partsCost.toDouble(),
                totalCost = maintenanceRecord.totalCost.toDouble(),
                workshop = maintenanceRecord.workshop,
                mechanic = maintenanceRecord.mechanic,
                warrantyUntil = maintenanceRecord.warrantyUntil?.toTimestamp(),
                isCompleted = maintenanceRecord.isCompleted,
                nextMaintenanceMileage = maintenanceRecord.nextMaintenanceMileage,
                nextRevisionDate = maintenanceRecord.nextRevisionDate?.toTimestamp(),
                minRevisionDate = maintenanceRecord.minRevisionDate?.toTimestamp(),
                maxRevisionDate = maintenanceRecord.maxRevisionDate?.toTimestamp(),
                distanceToDealer = maintenanceRecord.distanceToDealer,
                organizationId = maintenanceRecord.organizationId ?: "",
                createdAt = maintenanceRecord.createdAt.toTimestamp(),
                updatedAt = maintenanceRecord.updatedAt.toTimestamp(),
                deletedFlag = false,
                notes = maintenanceRecord.notes
            )
        }
    }
    
    fun toLocal(): MaintenanceRecord {
        return MaintenanceRecord(
            id = this.id,
            vehicleId = this.vehicleId,
            date = this.date?.toLocalDateTime() ?: LocalDateTime.now(),
            type = MaintenanceType.valueOf(this.type),
            description = this.description,
            mileage = this.mileage,
            laborCost = java.math.BigDecimal.valueOf(this.laborCost),
            partsCost = java.math.BigDecimal.valueOf(this.partsCost),
            totalCost = java.math.BigDecimal.valueOf(this.totalCost),
            workshop = this.workshop,
            mechanic = this.mechanic,
            warrantyUntil = this.warrantyUntil?.toLocalDateTime(),
            isCompleted = this.isCompleted,
            nextMaintenanceMileage = this.nextMaintenanceMileage,
            nextRevisionDate = this.nextRevisionDate?.toLocalDateTime(),
            minRevisionDate = this.minRevisionDate?.toLocalDateTime(),
            maxRevisionDate = this.maxRevisionDate?.toLocalDateTime(),
            distanceToDealer = this.distanceToDealer,
            organizationId = this.organizationId,
            createdAt = this.createdAt?.toLocalDateTime() ?: LocalDateTime.now(),
            updatedAt = this.updatedAt?.toLocalDateTime() ?: LocalDateTime.now(),
            notes = this.notes
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
