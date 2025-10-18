package dev.barreto.fleetctrl.data.repositories

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository principal que agrupa todos os repositories da aplicação
 * Facilita a injeção de dependência e o acesso centralizado aos dados
 */
@Singleton
class FleetRepository @Inject constructor(
    val vehicleRepository: VehicleRepository,
    val fuelRecordRepository: FuelRecordRepository,
    val maintenanceRecordRepository: MaintenanceRecordRepository,
    private val syncRepository: SyncRepository
) {
    
    // ===== OPERAÇÕES DE ESTATÍSTICAS GERAIS =====
    
    suspend fun getFleetStatistics(): FleetStatistics {
        val totalVehicles = vehicleRepository.getTotalVehicleCount()
        val activeVehicles = vehicleRepository.getActiveVehicleCount()
        val averageMileage = vehicleRepository.getAverageMileage()
        
        return FleetStatistics(
            totalVehicles = totalVehicles,
            activeVehicles = activeVehicles,
            averageMileage = averageMileage ?: 0.0
        )
    }
    
    // ===== OPERAÇÕES DE SINCRONIZAÇÃO =====
    
    suspend fun syncVehiclesFromOrganization(organizationId: String) {
        // Usar o SyncRepository para sincronizar veículos
        syncRepository.syncVehiclesOnly(organizationId)
    }
    
    suspend fun syncOrganizationFromCloud(organizationId: String) {
        // Sincronizar todos os dados da organização da nuvem para o local
        syncRepository.syncOrganizationData(organizationId)
    }

    suspend fun syncLocalDataToCloud(organizationId: String, force: Boolean = false) {
        // Sincronizar dados locais para a nuvem
        syncRepository.syncLocalDataToCloud(organizationId, force)
    }
    
    // ===== OPERAÇÕES DE VALIDAÇÃO GLOBAL =====
    
    suspend fun validateAllData(): Boolean {
        // Aqui poderiam ser adicionadas validações globais
        // que envolvem múltiplas entidades
        return true
    }
}

/**
 * Estatísticas gerais da frota
 */
data class FleetStatistics(
    val totalVehicles: Int,
    val activeVehicles: Int,
    val averageMileage: Double
)