package dev.barreto.fleetctrl.data.repositories

import dev.barreto.fleetctrl.data.database.daos.ActivityRecordDao
import dev.barreto.fleetctrl.data.database.daos.FuelRecordDao
import dev.barreto.fleetctrl.data.database.daos.MaintenanceRecordDao
import dev.barreto.fleetctrl.data.database.daos.OrganizationDao
import dev.barreto.fleetctrl.data.database.daos.VehicleDao
import dev.barreto.fleetctrl.data.database.entities.ActivityRecord
import dev.barreto.fleetctrl.data.database.entities.FuelRecord
import dev.barreto.fleetctrl.data.database.entities.MaintenanceRecord
import dev.barreto.fleetctrl.data.database.entities.Organization
import dev.barreto.fleetctrl.data.database.entities.UserOrganization
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRepository @Inject constructor(
    private val organizationDao: OrganizationDao,
    private val vehicleDao: VehicleDao,
    private val activityRecordDao: ActivityRecordDao,
    private val fuelRecordDao: FuelRecordDao,
    private val maintenanceRecordDao: MaintenanceRecordDao
) {
    suspend fun loadSummary(currentOrganizationId: String?): HomeSummary = withContext(Dispatchers.IO) {
        val organizations = organizationDao.getAllOrganizationsList()
        val memberships = organizationDao.getAllUserOrganizationsList()
        val vehicles = vehicleDao.getAllVehiclesList()
        val activities = activityRecordDao.getAllActivityRecordsList()
        val fuelRecords = fuelRecordDao.getAllFuelRecordsList()
        val maintenanceRecords = maintenanceRecordDao.getAllMaintenanceRecordsList()

        val overallMetrics = computeMetrics(vehicles, activities, fuelRecords, maintenanceRecords)
        val localVehicles = vehicles.filter { it.organizationId.isNullOrBlank() }
        val localActivities = activities.filter { it.organizationId.isNullOrBlank() }
        val localFuel = fuelRecords.filter { it.organizationId.isNullOrBlank() }
        val localMaintenance = maintenanceRecords.filter { it.organizationId.isNullOrBlank() }
        val localMetrics = computeMetrics(localVehicles, localActivities, localFuel, localMaintenance)

        val orgMetrics = currentOrganizationId?.takeIf { it.isNotBlank() }?.let { orgId ->
            val orgVehicles = vehicles.filter { it.organizationId == orgId }
            val orgActivities = activities.filter { it.organizationId == orgId }
            val orgFuel = fuelRecords.filter { it.organizationId == orgId }
            val orgMaintenance = maintenanceRecords.filter { it.organizationId == orgId }
            computeMetrics(orgVehicles, orgActivities, orgFuel, orgMaintenance)
        }

        val activeOrganizations = organizations.count { it.isActive }
        val activeMemberships = memberships.count { it.isActive }
        val activeUsers = memberships.filter { it.isActive }.map { it.userId }.distinct().size
        val currentOrgMembers = currentOrganizationId?.let { orgId ->
            memberships.count { it.organizationId == orgId && it.isActive }
        } ?: 0

        HomeSummary(
            overall = overallMetrics,
            local = localMetrics,
            organization = orgMetrics,
            activeOrganizations = activeOrganizations,
            activeMemberships = activeMemberships,
            activeUsers = activeUsers,
            currentOrganizationMemberCount = currentOrgMembers,
            lastUpdated = listOfNotNull(
                overallMetrics.latestUpdate,
                localMetrics.latestUpdate,
                orgMetrics?.latestUpdate
            ).maxOrNull()
        )
    }

    private fun computeMetrics(
        vehicles: List<Vehicle>,
        activities: List<ActivityRecord>,
        fuelRecords: List<FuelRecord>,
        maintenanceRecords: List<MaintenanceRecord>
    ): SummaryMetrics {
        val activeVehicles = vehicles.count { it.isActive }

        val completedTrips = activities.filter { it.endMileage > it.startMileage }
        val totalDistance = completedTrips.sumOf { (it.endMileage - it.startMileage).coerceAtLeast(0) }

        // Km médio exibido na Home: distância média por viagem
        // Fallback: média da quilometragem atual dos veículos ativos se não houver viagens concluídas
        val averageMileage = if (completedTrips.isNotEmpty()) {
            totalDistance.toDouble() / completedTrips.size
        } else {
            vehicles.filter { it.isActive }
                .map { it.currentMileage.toDouble() }
                .takeIf { it.isNotEmpty() }
                ?.average() ?: 0.0
        }

        val totalFuelCost = fuelRecords.fold(BigDecimal.ZERO) { acc, record -> acc + record.totalCost }
        val totalFuelQuantity = fuelRecords.sumOf { it.quantity }

        val openMaintenance = maintenanceRecords.count { !it.isCompleted }
        val latestUpdate = listOfNotNull(
            vehicles.maxOfOrNull { it.updatedAt },
            activities.maxOfOrNull { it.updatedAt },
            fuelRecords.maxOfOrNull { it.updatedAt },
            maintenanceRecords.maxOfOrNull { it.updatedAt }
        ).maxOrNull()

        return SummaryMetrics(
            vehicleCount = vehicles.size,
            activeVehicleCount = activeVehicles,
            averageMileage = averageMileage,
            tripCount = completedTrips.size,
            totalDistanceKm = totalDistance,
            fuelRecordCount = fuelRecords.size,
            totalFuelCost = totalFuelCost.toDouble(),
            totalFuelQuantity = totalFuelQuantity,
            maintenanceCount = maintenanceRecords.size,
            openMaintenanceCount = openMaintenance,
            latestUpdate = latestUpdate
        )
    }
}

data class HomeSummary(
    val overall: SummaryMetrics,
    val local: SummaryMetrics,
    val organization: SummaryMetrics?,
    val activeOrganizations: Int,
    val activeMemberships: Int,
    val activeUsers: Int,
    val currentOrganizationMemberCount: Int,
    val lastUpdated: LocalDateTime?
)

data class SummaryMetrics(
    val vehicleCount: Int = 0,
    val activeVehicleCount: Int = 0,
    val averageMileage: Double = 0.0,
    val tripCount: Int = 0,
    val totalDistanceKm: Long = 0L,
    val fuelRecordCount: Int = 0,
    val totalFuelCost: Double = 0.0,
    val totalFuelQuantity: Double = 0.0,
    val maintenanceCount: Int = 0,
    val openMaintenanceCount: Int = 0,
    val latestUpdate: LocalDateTime? = null
)
