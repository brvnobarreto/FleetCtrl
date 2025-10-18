package dev.barreto.fleetctrl.data.firestore.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import dev.barreto.fleetctrl.data.database.entities.FuelRecord
import java.time.LocalDateTime
import java.time.ZoneId

data class FuelRecordFirestore(
    val id: Long = 0,
    val vehicleId: Long = 0,
    val date: Timestamp? = null,
    val fuelType: String = "",
    val quantity: Double = 0.0,
    val pricePerLiter: Double = 0.0,
    val totalCost: Double = 0.0,
    val mileage: Long = 0,
    val gasStation: String? = null,
    val location: String? = null,
    val receiptNumber: String? = null,
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
        fun fromLocal(fuelRecord: FuelRecord): FuelRecordFirestore {
            return FuelRecordFirestore(
                id = fuelRecord.id,
                vehicleId = fuelRecord.vehicleId ?: 0L,
                date = fuelRecord.date.toTimestamp(),
                fuelType = fuelRecord.fuelType,
                quantity = fuelRecord.quantity,
                pricePerLiter = fuelRecord.pricePerLiter.toDouble(),
                totalCost = fuelRecord.totalCost.toDouble(),
                mileage = fuelRecord.mileage,
                gasStation = fuelRecord.gasStation,
                location = fuelRecord.location,
                receiptNumber = fuelRecord.receiptNumber,
                organizationId = fuelRecord.organizationId ?: "",
                createdAt = fuelRecord.createdAt.toTimestamp(),
                updatedAt = fuelRecord.updatedAt.toTimestamp(),
                deletedFlag = false,
                notes = fuelRecord.notes
            )
        }
    }
    
    fun toLocal(): FuelRecord {
        return FuelRecord(
            id = this.id,
            vehicleId = this.vehicleId,
            date = this.date?.toLocalDateTime() ?: LocalDateTime.now(),
            fuelType = this.fuelType,
            quantity = this.quantity,
            pricePerLiter = java.math.BigDecimal.valueOf(this.pricePerLiter),
            totalCost = java.math.BigDecimal.valueOf(this.totalCost),
            mileage = this.mileage,
            gasStation = this.gasStation,
            location = this.location,
            receiptNumber = this.receiptNumber,
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
