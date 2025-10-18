package dev.barreto.fleetctrl.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.barreto.fleetctrl.data.database.AppDatabase
import dev.barreto.fleetctrl.data.database.daos.MaintenanceRecordDao
import dev.barreto.fleetctrl.data.database.entities.MaintenanceRecord
import dev.barreto.fleetctrl.data.database.entities.MaintenanceType
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class MaintenanceRecordDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: MaintenanceRecordDao

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.maintenanceRecordDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() { database.close() }

    @Test
    fun insertAndQueryLatest() {
        val now = LocalDateTime.now()
        // FK parent
        runBlockingUnit {
            database.vehicleDao().insertVehicle(
                Vehicle(
                    id = 1,
                    vehicleNumber = "V1",
                    plate = "AAA1111",
                    model = "Model",
                    brand = "Brand",
                    year = 2020,
                    color = "Color",
                    driver = "Driver",
                    showInDiary = true,
                    engineType = "Gasolina",
                    fuelCapacity = 50.0,
                    averageConsumption = 12.0
                )
            )
        }
        val m1 = MaintenanceRecord(id = 0, vehicleId = 1, date = now.minusDays(2), type = MaintenanceType.PREVENTIVE, description = "oil", mileage = 100, totalCost = BigDecimal("100.0"))
        val m2 = MaintenanceRecord(id = 0, vehicleId = 1, date = now, type = MaintenanceType.CORRECTIVE, description = "brake", mileage = 200, totalCost = BigDecimal("200.0"))

        runBlockingUnit {
            dao.insertMaintenanceRecord(m1)
            dao.insertMaintenanceRecord(m2)
            val latest = dao.getLatestMaintenanceRecord(1)
            assertNotNull(latest)
            assertEquals(MaintenanceType.CORRECTIVE, latest!!.type)
            assertEquals(200, latest.mileage)

            val listByVehicle = dao.getMaintenanceRecordsByVehicle(1).first()
            assertEquals(2, listByVehicle.size)
        }
    }
}

// Local helper for suspend calls
private fun <T> runBlockingUnit(block: suspend () -> T) {
    kotlinx.coroutines.runBlocking { block() }
}
