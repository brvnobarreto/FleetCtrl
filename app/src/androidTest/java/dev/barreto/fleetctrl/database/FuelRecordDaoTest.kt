package dev.barreto.fleetctrl.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.barreto.fleetctrl.data.database.AppDatabase
import dev.barreto.fleetctrl.data.database.daos.FuelRecordDao
import dev.barreto.fleetctrl.data.database.entities.FuelRecord
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
class FuelRecordDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: FuelRecordDao

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.fuelRecordDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() { database.close() }

    @Test
    fun insertAndQueryByVehicle() {
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
        val fr1 = FuelRecord(
            id = 0,
            vehicleId = 1,
            date = now,
            fuelType = "GAS",
            quantity = 10.0,
            pricePerLiter = BigDecimal("5.0"),
            totalCost = BigDecimal("50.0"),
            mileage = 100,
            organizationId = "org-1"
        )
        val fr2 = fr1.copy(id = 0, vehicleId = 1, quantity = 20.0, totalCost = BigDecimal("100.0"), mileage = 200)

        runBlockingUnit {
            dao.insertFuelRecord(fr1)
            dao.insertFuelRecord(fr2)

            val count = dao.getFuelRecordCountByVehicle(1)
            assertEquals(2, count)
            val totalCost = dao.getTotalFuelCostByVehicle(1)
            assertEquals(150.0, totalCost!!, 0.0001)

            val avgPriceByType = dao.getAveragePricePerLiterByType("GAS")
            assertEquals(5.0, avgPriceByType!!, 0.0001)
        }
    }

    @Test
    fun dateRange_and_search_and_recent() {
        val now = LocalDateTime.now()
        runBlockingUnit {
            database.vehicleDao().insertVehicle(
                Vehicle(
                    id = 2,
                    vehicleNumber = "V2",
                    plate = "BBB2222",
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
        val fr1 = FuelRecord(id = 0, vehicleId = 2, date = now.minusDays(2), fuelType = "GAS", quantity = 10.0, pricePerLiter = BigDecimal("5.0"), totalCost = BigDecimal("50.0"), mileage = 100, gasStation = "Posto A")
        val fr2 = FuelRecord(id = 0, vehicleId = 2, date = now.minusDays(1), fuelType = "GAS", quantity = 15.0, pricePerLiter = BigDecimal("5.5"), totalCost = BigDecimal("82.5"), mileage = 150, gasStation = "Posto B")
        val fr3 = FuelRecord(id = 0, vehicleId = 2, date = now, fuelType = "DSL", quantity = 20.0, pricePerLiter = BigDecimal("4.0"), totalCost = BigDecimal("80.0"), mileage = 200, gasStation = "Posto C")

        runBlockingUnit {
            dao.insertFuelRecord(fr1)
            dao.insertFuelRecord(fr2)
            dao.insertFuelRecord(fr3)

            val range = dao.getFuelRecordsByVehicleAndDateRange(2, now.minusDays(2), now.minusHours(12)).first()
            assertEquals(2, range.size)

            val search = dao.searchFuelRecordsByVehicle(2, "%Posto%").first()
            assertEquals(3, search.size)

            val recent = dao.getRecentFuelRecords(2, 2).first()
            assertEquals(2, recent.size)
        }
    }
}

// Helper to call suspend functions in instrumented tests without coroutines-test
private fun <T> runBlockingUnit(block: suspend () -> T) {
    kotlinx.coroutines.runBlocking { block() }
}
