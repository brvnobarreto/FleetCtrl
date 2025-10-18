package dev.barreto.fleetctrl.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Entidade que representa um registro de abastecimento
 */
@Entity(
    tableName = "fuel_records",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["vehicleId"]), Index(value = ["date"])]
)
data class FuelRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Relacionamento
    val vehicleId: Long?, // ID do veículo (opcional para evitar conflitos de FK)
    
    // Dados do abastecimento
    val date: LocalDateTime, // Data e hora do abastecimento
    val fuelType: String, // Tipo de combustível (Gasolina, Diesel, Etanol, etc.)
    val quantity: Double, // Quantidade abastecida em litros
    val pricePerLiter: BigDecimal, // Preço por litro
    val totalCost: BigDecimal, // Custo total do abastecimento
    
    // Informações adicionais
    val mileage: Long, // Quilometragem no momento do abastecimento
    val gasStation: String? = null, // Posto de gasolina
    val location: String? = null, // Localização do posto
    val receiptNumber: String? = null, // Número do cupom fiscal
    
    // Metadados
    val organizationId: String? = null, // ID da organização (para sincronização)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val notes: String? = null // Observações adicionais
)