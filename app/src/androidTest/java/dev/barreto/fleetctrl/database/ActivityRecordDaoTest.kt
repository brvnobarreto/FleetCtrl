package dev.barreto.fleetctrl.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.barreto.fleetctrl.data.database.AppDatabase
import dev.barreto.fleetctrl.data.database.daos.ActivityRecordDao
import dev.barreto.fleetctrl.data.database.entities.ActivityRecord
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class ActivityRecordDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ActivityRecordDao

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.activityRecordDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() { database.close() }

    @Test
    fun insertAndQueryTotals() {
        val now = LocalDateTime.now()
        val a1 = ActivityRecord(id = 0, vehicleId = 1, plate = "AAA1111", driver = "D1", date = now, startMileage = 10, endMileage = 20, organizationId = "org-1")
        val a2 = ActivityRecord(id = 0, vehicleId = 1, plate = "AAA1111", driver = "D1", date = now, startMileage = 50, endMileage = 70, organizationId = "org-1")
        val a3 = ActivityRecord(id = 0, vehicleId = null, plate = "BBB2222", driver = "D2", date = now, startMileage = 100, endMileage = 100, organizationId = null)

        runBlockingUnit {
            // FK parent inside coroutine
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
            dao.insertActivityRecord(a1)
            dao.insertActivityRecord(a2)
            dao.insertActivityRecord(a3)

            val countByVehicle = dao.getActivityCountByVehicle(1)
            assertEquals(2, countByVehicle)
            val totalByPlate = dao.getTotalDistanceByPlate("AAA1111")
            assertEquals(30L, totalByPlate)

            val lastByVehicle = dao.getLastActivityRecordByVehicle(1)
            assertNotNull(lastByVehicle)
            val byPlateRange = dao.getActivityRecordsByPlateAndDateRange("AAA1111", now.minusDays(3), now).first()
            assertEquals(2, byPlateRange.size)
        }
    }

}

// Local helper for suspend calls
private fun <T> runBlockingUnit(block: suspend () -> T) {
    kotlinx.coroutines.runBlocking { block() }
}
