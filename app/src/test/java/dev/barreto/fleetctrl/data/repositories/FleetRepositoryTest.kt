package dev.barreto.fleetctrl.data.repositories

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FleetRepositoryTest {

    private lateinit var vehicleRepository: VehicleRepository
    private lateinit var fuelRecordRepository: FuelRecordRepository
    private lateinit var maintenanceRecordRepository: MaintenanceRecordRepository
    private lateinit var syncRepository: SyncRepository

    private lateinit var repository: FleetRepository

    @Before
    fun setup() {
        vehicleRepository = mockk()
        fuelRecordRepository = mockk()
        maintenanceRecordRepository = mockk()
        syncRepository = mockk()

        repository = FleetRepository(
            vehicleRepository,
            fuelRecordRepository,
            maintenanceRecordRepository,
            syncRepository
        )
    }

    @Test
    fun `getFleetStatistics returns aggregated values`() = runTest {
        coEvery { vehicleRepository.getTotalVehicleCount() } returns 10
        coEvery { vehicleRepository.getActiveVehicleCount() } returns 7
        coEvery { vehicleRepository.getAverageMileage() } returns 12345.67

        val stats = repository.getFleetStatistics()

        assertEquals(10, stats.totalVehicles)
        assertEquals(7, stats.activeVehicles)
        assertEquals(12345.67, stats.averageMileage, 0.0001)
    }

    @Test
    fun `getFleetStatistics handles null averageMileage`() = runTest {
        coEvery { vehicleRepository.getTotalVehicleCount() } returns 5
        coEvery { vehicleRepository.getActiveVehicleCount() } returns 3
        coEvery { vehicleRepository.getAverageMileage() } returns null

        val stats = repository.getFleetStatistics()

        assertEquals(5, stats.totalVehicles)
        assertEquals(3, stats.activeVehicles)
        assertEquals(0.0, stats.averageMileage, 0.0)
    }

    @Test
    fun `syncVehiclesFromOrganization delegates to syncRepository`() = runTest {
        coJustRun { syncRepository.syncVehiclesOnly("org-1") }

        repository.syncVehiclesFromOrganization("org-1")

        coVerify { syncRepository.syncVehiclesOnly("org-1") }
    }

    @Test
    fun `syncOrganizationFromCloud delegates to syncRepository`() = runTest {
        coJustRun { syncRepository.syncOrganizationData("org-1") }

        repository.syncOrganizationFromCloud("org-1")

        coVerify { syncRepository.syncOrganizationData("org-1") }
    }

    @Test
    fun `syncLocalDataToCloud delegates to syncRepository`() = runTest {
        coJustRun { syncRepository.syncLocalDataToCloud("org-1", force = true) }

        repository.syncLocalDataToCloud("org-1", force = true)

        coVerify { syncRepository.syncLocalDataToCloud("org-1", force = true) }
    }

    @Test
    fun `validateAllData returns true`() = runTest {
        assertTrue(repository.validateAllData())
    }
}
