package dev.barreto.fleetctrl.data.firestore.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import java.time.LocalDateTime
import java.time.ZoneId

data class VehicleFirestore(
    val id: Long = 0,
    val vehicleNumber: String = "",
    val plate: String = "",
    val model: String = "",
    val brand: String = "",
    val year: Int = 0,
    val color: String = "",
    val driver: String = "",
    val showInDiary: Boolean = false,
    val photoBase64: String = "", // Imagem em Base64
    val photoUrl: String = "", // URL da imagem em storage externo (preferido)
    val organizationId: String = "", // ID da organização
    val engineType: String = "",
    val fuelCapacity: Double = 0.0,
    val averageConsumption: Double = 0.0,
    @get:PropertyName("isActive") @set:PropertyName("isActive")
    var activeFlag: Boolean = true,
    // Compat com dados legados que usam "active" no lugar de "isActive"
    @get:PropertyName("active") @set:PropertyName("active")
    var legacyActive: Boolean? = null,
    @get:PropertyName("isDeleted") @set:PropertyName("isDeleted")
    var isDeleted: Boolean? = false,
    val currentMileage: Long = 0,
    val lastMaintenanceMileage: Long = 0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val notes: String? = null
) {
    companion object {
        fun fromLocal(vehicle: Vehicle): VehicleFirestore {
            val path = vehicle.photoPath
            val isRemoteUrl = path?.startsWith("http://", true) == true || path?.startsWith("https://", true) == true
            return VehicleFirestore(
                id = vehicle.id,
                vehicleNumber = vehicle.vehicleNumber,
                plate = vehicle.plate,
                model = vehicle.model,
                brand = vehicle.brand,
                year = vehicle.year,
                color = vehicle.color,
                driver = vehicle.driver,
                showInDiary = vehicle.showInDiary,
                // Nunca envia Base64; usa sempre URL remota quando disponível
                photoBase64 = "",
                photoUrl = if (isRemoteUrl) path!! else "",
                organizationId = vehicle.organizationId ?: "",
                engineType = vehicle.engineType,
                fuelCapacity = vehicle.fuelCapacity,
                averageConsumption = vehicle.averageConsumption,
                activeFlag = vehicle.isActive,
                isDeleted = false, // Sempre false ao enviar
                currentMileage = vehicle.currentMileage,
                lastMaintenanceMileage = vehicle.lastMaintenanceMileage,
                createdAt = vehicle.createdAt.toTimestamp(),
                updatedAt = vehicle.updatedAt.toTimestamp(),
                notes = vehicle.notes
            )
        }
    }
    
    fun toLocal(): Vehicle {
        return Vehicle(
            id = this.id,
            vehicleNumber = this.vehicleNumber,
            plate = this.plate,
            model = this.model,
            brand = this.brand,
            year = this.year,
            color = this.color,
            driver = this.driver,
            showInDiary = this.showInDiary,
            // Preferir URL remota; fallback: manter Base64 (UI tratará)
            photoPath = if (this.photoUrl.isNotBlank()) this.photoUrl else this.photoBase64,
            organizationId = this.organizationId,
            engineType = this.engineType,
            fuelCapacity = this.fuelCapacity,
            averageConsumption = this.averageConsumption,
            isActive = this.activeFlag || (this.legacyActive == true),
            currentMileage = this.currentMileage,
            lastMaintenanceMileage = this.lastMaintenanceMileage,
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
