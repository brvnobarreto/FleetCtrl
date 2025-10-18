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
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class HomeRepositoryTest {

    private lateinit var organizationDao: OrganizationDao
    private lateinit var vehicleDao: VehicleDao
    private lateinit var activityRecordDao: ActivityRecordDao
    private lateinit var fuelRecordDao: FuelRecordDao
    private lateinit var maintenanceRecordDao: MaintenanceRecordDao

    private lateinit var repository: HomeRepository

    @Before
    fun setup() {
        organizationDao = mockk()
        vehicleDao = mockk()
        activityRecordDao = mockk()
        fuelRecordDao = mockk()
        maintenanceRecordDao = mockk()

        repository = HomeRepository(
            organizationDao,
            vehicleDao,
            activityRecordDao,
            fuelRecordDao,
            maintenanceRecordDao
        )
    }

    @Test
    fun `loadSummary aggregates overall, local and organization metrics`() = runTest {
        val now = LocalDateTime.now()
        val orgId = "org-1"

        // Organizations and memberships
        val organizations = listOf(
            Organization(id = orgId, code = "C1", name = "Org 1", description = null, ownerId = "u1", ownerEmail = "u1@x.com", isActive = true),
            Organization(id = "org-2", code = "C2", name = "Org 2", description = null, ownerId = "u2", ownerEmail = "u2@x.com", isActive = false),
        )
        val memberships = listOf(
            UserOrganization(userId = "u1", organizationId = orgId, role = "owner", joinedAt = now, isActive = true, userEmail = null, isSynced = false, lastSyncAt = null),
            UserOrganization(userId = "u2", organizationId = orgId, role = "editor", joinedAt = now, isActive = true, userEmail = null, isSynced = false, lastSyncAt = null),
            UserOrganization(userId = "u3", organizationId = "org-2", role = "viewer", joinedAt = now, isActive = false, userEmail = null, isSynced = false, lastSyncAt = null),
        )

        // Vehicles
        val vehicles = listOf(
            createVehicle(id = 1, organizationId = orgId, currentMileage = 100),
            createVehicle(id = 2, organizationId = orgId, currentMileage = 150, isActive = false),
            createVehicle(id = 3, organizationId = null, currentMileage = 200),
        )

        // Activities (two completed, one invalid)
        val activities = listOf(
            ActivityRecord(id = 1, vehicleId = 1, plate = "AAA1111", driver = "D1", date = now.minusDays(1), startMileage = 10, endMileage = 20, observation = null, organizationId = orgId, createdAt = now.minusDays(1), updatedAt = now.minusDays(1)),
            ActivityRecord(id = 2, vehicleId = 1, plate = "AAA1111", driver = "D1", date = now.minusDays(2), startMileage = 30, endMileage = 10, observation = null, organizationId = orgId, createdAt = now.minusDays(2), updatedAt = now.minusDays(2)), // invalid (negative distance)
            ActivityRecord(id = 3, vehicleId = 3, plate = "BBB2222", driver = "D2", date = now, startMileage = 0, endMileage = 50, observation = null, organizationId = null, createdAt = now, updatedAt = now),
        )

        // Fuel
        val fuelRecords = listOf(
            FuelRecord(id = 1, vehicleId = 1, date = now, fuelType = "GAS", quantity = 10.0, pricePerLiter = BigDecimal("5.0"), totalCost = BigDecimal("50.0"), mileage = 100, gasStation = null, location = null, receiptNumber = null, organizationId = orgId, createdAt = now, updatedAt = now, notes = null),
            FuelRecord(id = 2, vehicleId = 3, date = now, fuelType = "DSL", quantity = 20.0, pricePerLiter = BigDecimal("4.5"), totalCost = BigDecimal("90.0"), mileage = 200, gasStation = null, location = null, receiptNumber = null, organizationId = null, createdAt = now, updatedAt = now, notes = null),
        )

        // Maintenance
        val maintenance = listOf(
            MaintenanceRecord(id = 1, vehicleId = 1, date = now, type = dev.barreto.fleetctrl.data.database.entities.MaintenanceType.PREVENTIVE, description = "oil", mileage = 100, laborCost = BigDecimal.ZERO, partsCost = BigDecimal.ZERO, totalCost = BigDecimal("100.0"), workshop = null, mechanic = null, warrantyUntil = null, isCompleted = true, nextMaintenanceMileage = null, nextRevisionDate = null, minRevisionDate = null, maxRevisionDate = null, distanceToDealer = null, organizationId = orgId, createdAt = now, updatedAt = now, notes = null),
            MaintenanceRecord(id = 2, vehicleId = 3, date = now, type = dev.barreto.fleetctrl.data.database.entities.MaintenanceType.CORRECTIVE, description = "brake", mileage = 200, laborCost = BigDecimal.ZERO, partsCost = BigDecimal.ZERO, totalCost = BigDecimal("200.0"), workshop = null, mechanic = null, warrantyUntil = null, isCompleted = false, nextMaintenanceMileage = null, nextRevisionDate = null, minRevisionDate = null, maxRevisionDate = null, distanceToDealer = null, organizationId = null, createdAt = now, updatedAt = now, notes = null),
        )

        // Mocks
        coEvery { organizationDao.getAllOrganizationsList() } returns organizations
        coEvery { organizationDao.getAllUserOrganizationsList() } returns memberships
        coEvery { vehicleDao.getAllVehiclesList() } returns vehicles
        coEvery { activityRecordDao.getAllActivityRecordsList() } returns activities
        coEvery { fuelRecordDao.getAllFuelRecordsList() } returns fuelRecords
        coEvery { maintenanceRecordDao.getAllMaintenanceRecordsList() } returns maintenance

        val summary = repository.loadSummary(orgId)

        // Overall metrics
        assertEquals(3, summary.overall.vehicleCount)
        assertEquals(2, summary.overall.activeVehicleCount)
        // Completed trips: 2 (10->20 and 0->50), total distance = 10 + 50 = 60
        assertEquals(2, summary.overall.tripCount)
        assertEquals(60L, summary.overall.totalDistanceKm)
        // Average mileage: completedTrips avg => 60 / 2 = 30.0
        assertEquals(30.0, summary.overall.averageMileage, 0.0001)
        // Fuel totals
        assertEquals(2, summary.overall.fuelRecordCount)
        assertEquals(140.0, summary.overall.totalFuelCost, 0.0001)
        assertEquals(30.0, summary.overall.totalFuelQuantity, 0.0001)
        // Maintenance
        assertEquals(2, summary.overall.maintenanceCount)
        assertEquals(1, summary.overall.openMaintenanceCount)

        // Local metrics (organizationId == null)
        assertEquals(1, summary.local.vehicleCount)
        assertEquals(1, summary.local.activeVehicleCount)
        assertEquals(1, summary.local.tripCount)
        assertEquals(50L, summary.local.totalDistanceKm)

        // Org metrics
        assertNotNull(summary.organization)
        assertEquals(2, summary.organization!!.vehicleCount)
        assertEquals(1, summary.organization!!.activeVehicleCount)
        assertEquals(1, summary.organization!!.tripCount)
        assertEquals(10L, summary.organization!!.totalDistanceKm)

        // Aggregates
        assertEquals(1, summary.activeOrganizations)
        assertEquals(2, summary.activeMemberships)
        assertEquals(2, summary.activeUsers)
        assertEquals(2, summary.currentOrganizationMemberCount)
        assertNotNull(summary.lastUpdated)
    }

    private fun createVehicle(
        id: Long,
        vehicleNumber: String = "V$id",
        plate: String = "PLT$id",
        currentMileage: Long = 0,
        isActive: Boolean = true,
        organizationId: String? = null
    ): Vehicle {
        return Vehicle(
            id = id,
            vehicleNumber = vehicleNumber,
            plate = plate,
            model = "M$id",
            brand = "B$id",
            year = 2020,
            color = "C$id",
            driver = "D$id",
            showInDiary = true,
            photoPath = null,
            organizationId = organizationId,
            engineType = "Gasolina",
            fuelCapacity = 50.0,
            averageConsumption = 12.0,
            isActive = isActive,
            currentMileage = currentMileage,
            lastMaintenanceMileage = 0,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            notes = null
        )
    }
}
