package dev.barreto.fleetctrl.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Entidade que representa um registro de manutenção
 */
@Entity(
    tableName = "maintenance_records",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["vehicleId"]), Index(value = ["date"]), Index(value = ["type"])]
)
data class MaintenanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Relacionamento
    val vehicleId: Long?, // ID do veículo (opcional para evitar conflitos de FK)
    
    // Dados da manutenção
    val date: LocalDateTime, // Data da manutenção
    val type: MaintenanceType, // Tipo de manutenção
    val description: String, // Descrição do serviço realizado
    val mileage: Long, // Quilometragem no momento da manutenção
    
    // Custos
    val laborCost: BigDecimal = BigDecimal.ZERO, // Custo da mão de obra
    val partsCost: BigDecimal = BigDecimal.ZERO, // Custo das peças
    val totalCost: BigDecimal, // Custo total
    
    // Informações adicionais
    val workshop: String? = null, // Oficina que realizou o serviço
    val mechanic: String? = null, // Mecânico responsável
    val warrantyUntil: LocalDateTime? = null, // Garantia até quando
    
    // Status
    val isCompleted: Boolean = true, // Se a manutenção foi concluída
    val nextMaintenanceMileage: Long? = null, // Próxima manutenção em quantos km
    
    // Próxima revisão
    val nextRevisionDate: LocalDateTime? = null, // Data da próxima revisão
    val minRevisionDate: LocalDateTime? = null, // Data mínima da revisão
    val maxRevisionDate: LocalDateTime? = null, // Data máxima da revisão
    val distanceToDealer: Long? = null, // Distância até a concessionária (km)
    
    // Metadados
    val organizationId: String? = null, // ID da organização (para sincronização)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val notes: String? = null // Observações adicionais
)

/**
 * Enum para tipos de manutenção
 */
enum class MaintenanceType {
    PREVENTIVE, // Preventiva
    CORRECTIVE, // Corretiva
    PREDICTIVE, // Preditiva
    EMERGENCY, // Emergencial
    INSPECTION, // Inspeção
    CALIBRATION, // Calibração
    CLEANING, // Limpeza
    OTHER // Outros
}