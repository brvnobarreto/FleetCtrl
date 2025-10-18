package dev.barreto.fleetctrl.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Entidade que representa um registro de atividade no diário de bordo
 * Registra saídas e chegadas de veículos com quilometragem
 */
@Entity(
    tableName = "activity_records",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["vehicleId"]), 
        Index(value = ["plate"]), 
        Index(value = ["date"]),
        Index(value = ["driver"])
    ]
)
data class ActivityRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Relacionamento com veículo
    val vehicleId: Long?, // ID do veículo (opcional para evitar conflitos de FK)
    
    // Dados da atividade
    val plate: String, // Placa do veículo (para referência rápida)
    val driver: String, // Condutor
    val date: LocalDateTime, // Data e hora do registro
    
    // Quilometragem
    val startMileage: Long, // Quilômetro de saída
    val endMileage: Long, // Quilômetro de chegada
    
    // Observações
    val observation: String? = null, // Observação opcional
    
    // Metadados
    val organizationId: String? = null, // ID da organização (para sincronização)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Calcula a distância percorrida
     */
    val distance: Long
        get() = endMileage - startMileage
    
    /**
     * Verifica se é um registro de saída (sem quilômetro de chegada)
     */
    val isDeparture: Boolean
        get() = endMileage == startMileage
}