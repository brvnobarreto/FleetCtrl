package dev.barreto.fleetctrl.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import dev.barreto.fleetctrl.viewmodels.FleetViewModel
import dev.barreto.fleetctrl.screens.fleet.FleetScreen
import io.mockk.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VehicleListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun listEmpty_showsSyncButtonWhenOrgPresent() {
        // Arrange
        val mockViewModel = mockk<FleetViewModel>(relaxed = true)
        every { mockViewModel.vehicles } returns kotlinx.coroutines.flow.MutableStateFlow(emptyList())
        every { mockViewModel.isLoading } returns kotlinx.coroutines.flow.MutableStateFlow(false)
        every { mockViewModel.errorMessage } returns kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
        every { mockViewModel.successMessage } returns kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

        // Act
        composeTestRule.setContent {
            FleetScreen(fleetViewModel = mockViewModel, currentOrgId = "org-1")
        }

        // Assert
        composeTestRule.onNodeWithText("Sincronizar Veículos").assertIsDisplayed()
    }

    @Test
    fun listWithItems_showsVehicles() {
        // Arrange
        val vehicles = listOf(
            createTestVehicle(1, "001", "ABC1234"),
            createTestVehicle(2, "002", "DEF5678")
        )
        val mockViewModel = mockk<FleetViewModel>(relaxed = true)
        every { mockViewModel.vehicles } returns kotlinx.coroutines.flow.MutableStateFlow(vehicles)
        every { mockViewModel.isLoading } returns kotlinx.coroutines.flow.MutableStateFlow(false)
        every { mockViewModel.errorMessage } returns kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
        every { mockViewModel.successMessage } returns kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

        // Act
        composeTestRule.setContent {
            FleetScreen(fleetViewModel = mockViewModel)
        }

        // Assert
        composeTestRule.onNodeWithText("001")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("002")
            .assertIsDisplayed()
    }

    // Loading overlay test skipped due to lack of explicit semantics contentDescription

    // Helper method
    private fun createTestVehicle(id: Long, vehicleNumber: String, plate: String): Vehicle {
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
            engineType = "Gasolina",
            fuelCapacity = 50.0,
            averageConsumption = 12.5,
            currentMileage = 10000L,
            isActive = true,
            organizationId = null
        )
    }
}
