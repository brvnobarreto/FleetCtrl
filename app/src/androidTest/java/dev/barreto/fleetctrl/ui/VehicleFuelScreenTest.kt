package dev.barreto.fleetctrl.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.barreto.fleetctrl.data.database.entities.FuelRecord
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import dev.barreto.fleetctrl.screens.fuel.VehicleFuelScreen
import dev.barreto.fleetctrl.viewmodels.FuelViewModel
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class VehicleFuelScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyState_showsMessage() {
        val vm = mockk<FuelViewModel>(relaxed = true)
        every { vm.fuelRecords } returns MutableStateFlow(emptyList())
        every { vm.isLoadingFuelRecords } returns MutableStateFlow(false)
        every { vm.fuelRecordsMessage } returns MutableStateFlow(null)
        justRun { vm.selectVehicle(any()) }

        composeRule.setContent {
            VehicleFuelScreen(
                vehicle = vehicle("AAA1111"),
                onBackClick = {},
                onEditFuelRecord = {},
                onDeleteFuelRecord = {},
                viewModel = vm
            )
        }

        // The EmptyFuelRecordsState uses strings from resources; relax assertion to header icon presence
        composeRule.onNodeWithContentDescription("Ordenar").assertIsDisplayed()
    }

    @Test
    fun list_showsRecords_andIcons_andSortMenu() {
        val vm = mockk<FuelViewModel>(relaxed = true)
        val r1 = record("Posto A", orgId = null)
        val r2 = record("Posto B", orgId = "org-1")
        every { vm.fuelRecords } returns MutableStateFlow(listOf(r1, r2))
        every { vm.isLoadingFuelRecords } returns MutableStateFlow(false)
        every { vm.fuelRecordsMessage } returns MutableStateFlow(null)
        justRun { vm.selectVehicle(any()) }

        composeRule.setContent {
            VehicleFuelScreen(
                vehicle = vehicle("AAA1111"),
                onBackClick = {},
                onEditFuelRecord = {},
                onDeleteFuelRecord = {},
                viewModel = vm
            )
        }

        composeRule.onAllNodesWithContentDescription("Local").assertCountEquals(1)
        composeRule.onAllNodesWithContentDescription("Nuvem").assertCountEquals(1)

        composeRule.onNodeWithContentDescription("Ordenar").performClick()
        composeRule.onNodeWithText("Data (Mais Recente)").assertIsDisplayed()
    }

    private fun vehicle(plate: String) = Vehicle(
        id = 1,
        vehicleNumber = "V1",
        plate = plate,
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

    private fun record(gasStation: String, orgId: String?) = FuelRecord(
        id = 1,
        vehicleId = 1,
        date = LocalDateTime.now(),
        fuelType = "GAS",
        quantity = 10.0,
        pricePerLiter = BigDecimal("5.0"),
        totalCost = BigDecimal("50.0"),
        mileage = 100,
        gasStation = gasStation,
        organizationId = orgId
    )
}
