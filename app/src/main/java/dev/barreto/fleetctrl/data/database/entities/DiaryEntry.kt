package dev.barreto.fleetctrl.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Entidade que representa uma entrada no diário de bordo
 */
@Entity(
    tableName = "diary_entries",
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
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Relacionamento
    val vehicleId: Long, // ID do veículo
    
    // Dados da entrada
    val date: LocalDateTime, // Data e hora da entrada
    val type: DiaryEntryType, // Tipo de entrada
    val title: String, // Título da entrada
    val description: String, // Descrição detalhada
    
    // Informações de localização
    val startLocation: String? = null, // Local de origem
    val endLocation: String? = null, // Local de destino
    val distance: Double? = null, // Distância percorrida em km
    
    // Informações de tempo
    val startTime: LocalDateTime? = null, // Horário de início
    val endTime: LocalDateTime? = null, // Horário de fim
    val duration: Long? = null, // Duração em minutos
    
    // Quilometragem
    val startMileage: Long? = null, // Quilometragem inicial
    val endMileage: Long? = null, // Quilometragem final
    
    // Status
    val isCompleted: Boolean = true, // Se a atividade foi concluída
    val priority: Priority = Priority.NORMAL, // Prioridade da entrada
    
    // Metadados
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val tags: String? = null, // Tags separadas por vírgula
    val notes: String? = null // Observações adicionais
)

/**
 * Enum para tipos de entrada no diário
 */
enum class DiaryEntryType {
    TRIP, // Viagem
    MAINTENANCE, // Manutenção
    INCIDENT, // Incidente
    INSPECTION, // Inspeção
    FUEL, // Abastecimento
    GENERAL, // Geral
    REMINDER, // Lembrete
    NOTE // Nota
}

/**
 * Enum para prioridades
 */
enum class Priority {
    LOW, // Baixa
    NORMAL, // Normal
    HIGH, // Alta
    URGENT // Urgente
}