package dev.barreto.fleetctrl.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.barreto.fleetctrl.data.database.AppDatabase
import dev.barreto.fleetctrl.data.database.daos.VehicleDao
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class VehicleDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var vehicleDao: VehicleDao

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        vehicleDao = database.vehicleDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun whenVehicleInserted_retrievableById() = runBlockingUnit {
        val vehicle = createTestVehicle(0, "001", "ABC1234")
        val id = vehicleDao.insertVehicle(vehicle)
        val retrieved = vehicleDao.getVehicleById(id)
        assertNotNull(retrieved)
        assertEquals("001", retrieved?.vehicleNumber)
        assertEquals("ABC1234", retrieved?.plate)
    }

    @Test
    fun whenVehicleInserted_appearsInGetAllVehicles() = runBlockingUnit {
        val v1 = createTestVehicle(0, "001", "ABC1234")
        val v2 = createTestVehicle(0, "002", "DEF5678")
        vehicleDao.insertVehicle(v1)
        vehicleDao.insertVehicle(v2)
        val all = vehicleDao.getAllVehicles().first()
        assertEquals(2, all.size)
        assertTrue(all.any { it.vehicleNumber == "001" })
        assertTrue(all.any { it.vehicleNumber == "002" })
    }

    @Test
    fun whenVehicleUpdated_reflectsChanges() = runBlockingUnit {
        val vehicle = createTestVehicle(0, "001", "ABC1234")
        val id = vehicleDao.insertVehicle(vehicle)
        val inserted = vehicleDao.getVehicleById(id)!!
        val updated = inserted.copy(model = "Updated Model", brand = "Updated Brand")
        vehicleDao.updateVehicle(updated)
        val retrieved = vehicleDao.getVehicleById(id)
        assertNotNull(retrieved)
        assertEquals("Updated Model", retrieved?.model)
        assertEquals("Updated Brand", retrieved?.brand)
    }

    @Test
    fun whenVehicleDeleted_notRetrievable() = runBlockingUnit {
        val vehicle = createTestVehicle(0, "001", "ABC1234")
        val id = vehicleDao.insertVehicle(vehicle)
        val withId = vehicle.copy(id = id)
        vehicleDao.deleteVehicle(withId)
        val retrieved = vehicleDao.getVehicleById(id)
        assertNull(retrieved)
    }

    @Test
    fun whenSearchingByPlate_returnsCorrectVehicle() = runBlockingUnit {
        val v = createTestVehicle(0, "001", "ABC1234")
        val id = vehicleDao.insertVehicle(v)
        val retrieved = vehicleDao.getVehicleByPlate("ABC1234")
        assertNotNull(retrieved)
        assertEquals("001", retrieved?.vehicleNumber)
        assertEquals("ABC1234", retrieved?.plate)
    }

    @Test
    fun whenSearchingByVehicleNumber_returnsCorrectVehicle() = runBlockingUnit {
        val v = createTestVehicle(0, "001", "ABC1234")
        vehicleDao.insertVehicle(v)
        val retrieved = vehicleDao.getVehicleByNumber("001")
        assertNotNull(retrieved)
        assertEquals("001", retrieved?.vehicleNumber)
        assertEquals("ABC1234", retrieved?.plate)
    }

    @Test
    fun countingActiveVehicles_returnsCorrectCount() = runBlockingUnit {
        // Arrange
        val v1 = createTestVehicle(0, "001", "ABC1234", isActive = true)
        val v2 = createTestVehicle(0, "002", "DEF5678", isActive = true)
        val v3 = createTestVehicle(0, "003", "GHI9012", isActive = false)
        vehicleDao.insertVehicle(v1)
        vehicleDao.insertVehicle(v2)
        vehicleDao.insertVehicle(v3)
        val count = vehicleDao.getActiveVehicleCount()
        assertEquals(2, count)
    }

    @Test
    fun countingTotalVehicles_returnsCorrectCount() = runBlockingUnit {
        // Arrange
        val v1 = createTestVehicle(0, "001", "ABC1234")
        val v2 = createTestVehicle(0, "002", "DEF5678")
        val v3 = createTestVehicle(0, "003", "GHI9012")
        vehicleDao.insertVehicle(v1)
        vehicleDao.insertVehicle(v2)
        vehicleDao.insertVehicle(v3)
        val total = vehicleDao.getTotalVehicleCount()
        assertEquals(3, total)
    }

    @Test
    fun averageMileage_returnsCorrectValue() = runBlockingUnit {
        // Arrange
        val v1 = createTestVehicle(0, "001", "ABC1234", mileage = 10000L)
        val v2 = createTestVehicle(0, "002", "DEF5678", mileage = 20000L)
        val v3 = createTestVehicle(0, "003", "GHI9012", mileage = 30000L)
        vehicleDao.insertVehicle(v1)
        vehicleDao.insertVehicle(v2)
        vehicleDao.insertVehicle(v3)
        val avg = vehicleDao.getAverageMileage()
        assertNotNull(avg)
        assertEquals(20000.0, avg!!, 0.1)
    }

    @Test
    fun maxMileage_returnsHighestValue() = runBlockingUnit {
        // Arrange
        val v1 = createTestVehicle(0, "001", "ABC1234", mileage = 10000L)
        val v2 = createTestVehicle(0, "002", "DEF5678", mileage = 50000L)
        val v3 = createTestVehicle(0, "003", "GHI9012", mileage = 30000L)
        vehicleDao.insertVehicle(v1)
        vehicleDao.insertVehicle(v2)
        vehicleDao.insertVehicle(v3)
        val max = vehicleDao.getMaxMileage()
        assertNotNull(max)
        assertEquals(50000L, max!!)
    }

    @Test
    fun minMileage_returnsLowestValue() = runBlockingUnit {
        // Arrange
        val v1 = createTestVehicle(0, "001", "ABC1234", mileage = 10000L)
        val v2 = createTestVehicle(0, "002", "DEF5678", mileage = 50000L)
        val v3 = createTestVehicle(0, "003", "GHI9012", mileage = 30000L)
        vehicleDao.insertVehicle(v1)
        vehicleDao.insertVehicle(v2)
        vehicleDao.insertVehicle(v3)
        val min = vehicleDao.getMinMileage()
        assertNotNull(min)
        assertEquals(10000L, min!!)
    }

    // Helper method
    private fun createTestVehicle(
        id: Long,
        vehicleNumber: String,
        plate: String,
        mileage: Long = 10000L,
        isActive: Boolean = true
    ): Vehicle {
        return Vehicle(
            id = id,
            vehicleNumber = vehicleNumber,
            plate = plate,
            model = "Test Model",
            brand = "Test Brand",
            year = 2023,
            color = "Test Color",
            driver = "Test Driver",
            showInDiary = true,
            photoPath = null,
            organizationId = null,
            engineType = "Gasolina",
            fuelCapacity = 50.0,
            averageConsumption = 12.5,
            isActive = isActive,
            currentMileage = mileage,
            lastMaintenanceMileage = 0
        )
    }
}

// Local helper for suspend calls
private fun <T> runBlockingUnit(block: suspend () -> T) {
    kotlinx.coroutines.runBlocking { block() }
}
