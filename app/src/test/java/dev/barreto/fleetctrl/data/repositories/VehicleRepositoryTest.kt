package dev.barreto.fleetctrl.data.repositories

import app.cash.turbine.test
import dev.barreto.fleetctrl.data.database.daos.ActivityRecordDao
import dev.barreto.fleetctrl.data.database.daos.FuelRecordDao
import dev.barreto.fleetctrl.data.database.daos.MaintenanceRecordDao
import dev.barreto.fleetctrl.data.database.daos.VehicleDao
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import dev.barreto.fleetctrl.utils.DataRefreshNotifier
import io.mockk.mockkObject

@OptIn(ExperimentalCoroutinesApi::class)
class VehicleRepositoryTest {

    private lateinit var vehicleDao: VehicleDao
    private lateinit var syncRepository: SyncRepository
    private lateinit var fuelRecordDao: FuelRecordDao
    private lateinit var activityRecordDao: ActivityRecordDao
    private lateinit var maintenanceRecordDao: MaintenanceRecordDao

    private lateinit var repository: VehicleRepository

    @Before
    fun setup() {
        vehicleDao = mockk()
        syncRepository = mockk()
        fuelRecordDao = mockk()
        activityRecordDao = mockk()
        maintenanceRecordDao = mockk()

        repository = VehicleRepository(
            vehicleDao,
            syncRepository,
            fuelRecordDao,
            activityRecordDao,
            maintenanceRecordDao
        )

        // Avoid touching Android Main dispatcher from DataRefreshNotifier during unit tests
        mockkObject(DataRefreshNotifier)
        every { DataRefreshNotifier.triggerRefreshSync() } just Runs
    }

    // ===== getAllVehicles with error handling =====
    @Test
    fun `getAllVehicles emits empty list on error`() = runTest {
        every { vehicleDao.getAllVehicles() } returns flow { throw RuntimeException("db error") }

        repository.getAllVehicles().test {
            val first = awaitItem()
            assertTrue(first.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getAllVehicles returns vehicles`() = runTest {
        val vehicles = listOf(createVehicle(id = 1, organizationId = null))
        every { vehicleDao.getAllVehicles() } returns flowOf(vehicles)

        repository.getAllVehicles().test {
            assertEquals(vehicles, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ===== getVehicleById / Plate / Number =====
    @Test
    fun `getVehicleById returns value`() = runTest {
        val v = createVehicle(id = 10)
        coEvery { vehicleDao.getVehicleById(10) } returns v

        val result = repository.getVehicleById(10)
        assertEquals(v, result)
    }

    @Test
    fun `getVehicleById returns null on exception`() = runTest {
        coEvery { vehicleDao.getVehicleById(99) } throws RuntimeException("boom")
        val result = repository.getVehicleById(99)
        assertNull(result)
    }

    @Test
    fun `getVehicleByPlate returns value`() = runTest {
        val v = createVehicle(id = 2, plate = "ABC1234")
        coEvery { vehicleDao.getVehicleByPlate("ABC1234") } returns v
        assertEquals(v, repository.getVehicleByPlate("ABC1234"))
    }

    @Test
    fun `getVehicleByNumber returns value`() = runTest {
        val v = createVehicle(id = 3, vehicleNumber = "001")
        coEvery { vehicleDao.getVehicleByNumber("001") } returns v
        assertEquals(v, repository.getVehicleByNumber("001"))
    }

    // ===== insertVehicle behavior =====
    @Test
    fun `insertVehicle no orgId does not upload`() = runTest {
        val newId = 42L
        val input = createVehicle(id = 0, organizationId = null)
        coEvery { vehicleDao.insertVehicle(input) } returns newId
        coEvery { vehicleDao.getVehicleById(newId) } returns input.copy(id = newId)
        // No call expected to uploadVehicle

        val resultId = repository.insertVehicle(input)
        assertEquals(newId, resultId)
        coVerify(exactly = 0) { syncRepository.uploadVehicle(any()) }
    }

    @Test
    fun `insertVehicle with orgId uploads new vehicle`() = runTest {
        val newId = 77L
        val input = createVehicle(id = 0, organizationId = "org-1")
        val inserted = input.copy(id = newId)
        coEvery { vehicleDao.insertVehicle(input) } returns newId
        coEvery { vehicleDao.getVehicleById(newId) } returns inserted
        coEvery { syncRepository.uploadVehicle(inserted) } just Runs

        val result = repository.insertVehicle(input)
        assertEquals(newId, result)
        coVerify { syncRepository.uploadVehicle(inserted) }
    }

    // ===== updateVehicle behavior =====
    @Test
    fun `updateVehicle with orgId uploads`() = runTest {
        val v = createVehicle(id = 5, organizationId = "org-1")
        coEvery { vehicleDao.updateVehicle(v) } just Runs
        coEvery { syncRepository.uploadVehicle(v) } just Runs

        repository.updateVehicle(v)
        coVerify { vehicleDao.updateVehicle(v) }
        coVerify { syncRepository.uploadVehicle(v) }
    }

    @Test
    fun `updateVehicle without orgId does not upload`() = runTest {
        val v = createVehicle(id = 5, organizationId = null)
        coEvery { vehicleDao.updateVehicle(v) } just Runs

        repository.updateVehicle(v)
        coVerify { vehicleDao.updateVehicle(v) }
        coVerify(exactly = 0) { syncRepository.uploadVehicle(any()) }
    }

    // ===== deleteVehicle behavior =====
    @Test
    fun `deleteVehicle calls remote delete and cleans dependent records`() = runTest {
        val v = createVehicle(id = 9, organizationId = "org-1")
        coEvery { syncRepository.deleteVehicle(v) } just Runs
        coEvery { activityRecordDao.deleteActivityRecordsByVehicle(9) } just Runs
        coEvery { fuelRecordDao.deleteFuelRecordsByVehicle(9) } just Runs
        coEvery { maintenanceRecordDao.deleteMaintenanceRecordsByVehicle(9) } just Runs
        coEvery { vehicleDao.deleteVehicle(v) } just Runs

        repository.deleteVehicle(v)

        coVerify { syncRepository.deleteVehicle(v) }
        coVerify { activityRecordDao.deleteActivityRecordsByVehicle(9) }
        coVerify { fuelRecordDao.deleteFuelRecordsByVehicle(9) }
        coVerify { maintenanceRecordDao.deleteMaintenanceRecordsByVehicle(9) }
        coVerify { vehicleDao.deleteVehicle(v) }
    }

    @Test
    fun `deleteVehicle skips remote when no org and still deletes local and dependents`() = runTest {
        val v = createVehicle(id = 11, organizationId = null)
        coEvery { activityRecordDao.deleteActivityRecordsByVehicle(11) } just Runs
        coEvery { fuelRecordDao.deleteFuelRecordsByVehicle(11) } just Runs
        coEvery { maintenanceRecordDao.deleteMaintenanceRecordsByVehicle(11) } just Runs
        coEvery { vehicleDao.deleteVehicle(v) } just Runs

        repository.deleteVehicle(v)

        coVerify(exactly = 0) { syncRepository.deleteVehicle(any()) }
        coVerify { activityRecordDao.deleteActivityRecordsByVehicle(11) }
        coVerify { fuelRecordDao.deleteFuelRecordsByVehicle(11) }
        coVerify { maintenanceRecordDao.deleteMaintenanceRecordsByVehicle(11) }
        coVerify { vehicleDao.deleteVehicle(v) }
    }

    @Test
    fun `deleteVehicleById fetches and deletes`() = runTest {
        val v = createVehicle(id = 13, organizationId = null)
        coEvery { vehicleDao.getVehicleById(13) } returns v
        coEvery { activityRecordDao.deleteActivityRecordsByVehicle(13) } just Runs
        coEvery { fuelRecordDao.deleteFuelRecordsByVehicle(13) } just Runs
        coEvery { maintenanceRecordDao.deleteMaintenanceRecordsByVehicle(13) } just Runs
        coEvery { vehicleDao.deleteVehicle(v) } just Runs

        repository.deleteVehicleById(13)

        coVerify { vehicleDao.getVehicleById(13) }
        coVerify { vehicleDao.deleteVehicle(v) }
    }

    // ===== updateMileage =====
    @Test
    fun `updateMileage updates vehicle and uploads when orgId present`() = runTest {
        val base = createVehicle(id = 21, organizationId = "org-1", currentMileage = 100)
        coEvery { vehicleDao.getVehicleById(21) } returns base
        val captured: CapturingSlot<Vehicle> = slot()
        coEvery { vehicleDao.updateVehicle(capture(captured)) } just Runs
        coEvery { syncRepository.uploadVehicle(any()) } just Runs

        repository.updateMileage(21, 500)

        val updated = captured.captured
        assertEquals(500, updated.currentMileage)
        // updatedAt changed to now; just assert it's not before base.updatedAt
        assertTrue(updated.updatedAt >= base.updatedAt)
        coVerify { syncRepository.uploadVehicle(any()) }
    }

    // ===== statistics passthrough =====
    @Test
    fun `statistics methods return dao values`() = runTest {
        coEvery { vehicleDao.getActiveVehicleCount() } returns 2
        coEvery { vehicleDao.getTotalVehicleCount() } returns 5
        coEvery { vehicleDao.getAverageMileage() } returns 1234.5
        coEvery { vehicleDao.getMaxMileage() } returns 9999
        coEvery { vehicleDao.getMinMileage() } returns 1

        assertEquals(2, repository.getActiveVehicleCount())
        assertEquals(5, repository.getTotalVehicleCount())
        assertEquals(1234.5, repository.getAverageMileage()!!, 0.0001)
        assertEquals(9999L, repository.getMaxMileage())
        assertEquals(1L, repository.getMinMileage())
    }

    // ===== validation =====
    @Test
    fun `isPlateUnique returns false when other vehicle exists`() = runTest {
        val other = createVehicle(id = 30, plate = "ABC1234")
        coEvery { vehicleDao.getVehicleByPlate("ABC1234") } returns other
        val result = repository.isPlateUnique("ABC1234", excludeId = 99)
        assertFalse(result)
    }

    @Test
    fun `isPlateUnique returns true when same id`() = runTest {
        val same = createVehicle(id = 40, plate = "ZZZ9999")
        coEvery { vehicleDao.getVehicleByPlate("ZZZ9999") } returns same
        val result = repository.isPlateUnique("ZZZ9999", excludeId = 40)
        assertTrue(result)
    }

    @Test
    fun `isPlateUnique returns true when none exists`() = runTest {
        coEvery { vehicleDao.getVehicleByPlate("FREE000") } returns null
        val result = repository.isPlateUnique("FREE000", excludeId = null)
        assertTrue(result)
    }

    @Test
    fun `validateVehicle fails for basic invalid fields`() = runTest {
        val bad = createVehicle(id = 1, vehicleNumber = "", plate = "", model = "", driver = "", year = 1800)
        val result = repository.validateVehicle(bad)
        assertFalse(result)
    }

    @Test
    fun `validateVehicle fails for non-unique number or plate`() = runTest {
        val v = createVehicle(id = 50, vehicleNumber = "001", plate = "ABC1234")
        coEvery { vehicleDao.getVehicleByNumber("001") } returns createVehicle(id = 51, vehicleNumber = "001")
        val result1 = repository.validateVehicle(v)
        assertFalse(result1)

        // Unique number but plate taken by another id
        coEvery { vehicleDao.getVehicleByNumber("001") } returns v // same id ok
        coEvery { vehicleDao.getVehicleByPlate("ABC1234") } returns createVehicle(id = 52, plate = "ABC1234")
        val result2 = repository.validateVehicle(v)
        assertFalse(result2)
    }

    @Test
    fun `validateVehicle succeeds when valid and unique`() = runTest {
        val v = createVehicle(id = 60, vehicleNumber = "060", plate = "PLT6060")
        coEvery { vehicleDao.getVehicleByNumber("060") } returns v // same id OK
        coEvery { vehicleDao.getVehicleByPlate("PLT6060") } returns v // same id OK
        val result = repository.validateVehicle(v)
        assertTrue(result)
    }

    // ===== helpers =====
    private fun createVehicle(
        id: Long,
        vehicleNumber: String = "001",
        plate: String = "ABC1234",
        model: String = "Model X",
        brand: String = "Brand Y",
        year: Int = 2022,
        color: String = "Blue",
        driver: String = "John",
        organizationId: String? = null,
        currentMileage: Long = 0
    ): Vehicle {
        return Vehicle(
            id = id,
            vehicleNumber = vehicleNumber,
            plate = plate,
            model = model,
            brand = brand,
            year = year,
            color = color,
            driver = driver,
            showInDiary = true,
            photoPath = null,
            organizationId = organizationId,
            engineType = "Gasolina",
            fuelCapacity = 50.0,
            averageConsumption = 12.5,
            isActive = true,
            currentMileage = currentMileage,
            lastMaintenanceMileage = 0,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            notes = null
        )
    }
}
