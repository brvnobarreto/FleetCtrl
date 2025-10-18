package dev.barreto.fleetctrl.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Entidade que representa um veículo na frota
 */
@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Informações básicas
    val vehicleNumber: String, // Número próprio do carro (ID customizado)
    val plate: String, // Placa do veículo
    val model: String, // Modelo do veículo
    val brand: String, // Marca do veículo
    val year: Int, // Ano do veículo
    val color: String, // Cor do veículo
    val driver: String, // Condutor do veículo
    
    // Configurações
    val showInDiary: Boolean = true, // Se deve aparecer no diário de bordo
    val photoPath: String? = null, // Caminho da foto do veículo
    val organizationId: String? = null, // ID da organização (para sincronização)
    
    // Informações técnicas
    val engineType: String, // Tipo de motor (Gasolina, Diesel, Elétrico, etc.)
    val fuelCapacity: Double, // Capacidade do tanque em litros
    val averageConsumption: Double, // Consumo médio em km/l
    
    // Status e controle
    val isActive: Boolean = true, // Se o veículo está ativo na frota
    val currentMileage: Long = 0, // Quilometragem atual
    val lastMaintenanceMileage: Long = 0, // Quilometragem da última manutenção
    
    // Metadados
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val notes: String? = null // Observações adicionais
)