package dev.barreto.fleetctrl.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.barreto.fleetctrl.components.AppHeader
import dev.barreto.fleetctrl.components.VehicleListItem
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import dev.barreto.fleetctrl.viewmodels.MainViewModel
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppHeaderAndVehicleItemTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun appHeader_showsSyncIcon_andCallsCallback() {
        val vm = mockk<MainViewModel>(relaxed = true)
        var called = false
        composeRule.setContent {
            AppHeader(
                title = "Título",
                viewModel = vm,
                onSyncToCloud = { called = true }
            )
        }
        composeRule.onNodeWithContentDescription("Sincronizar para nuvem").assertIsDisplayed().performClick()
        assertTrue(called)
    }

    @Test
    fun vehicleListItem_showsLocalIcon_andActionButtons() {
        var editCalled = false
        var deleteCalled = false
        var clicked = false
        val vehicle = Vehicle(
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
            averageConsumption = 12.0,
            organizationId = null
        )
        composeRule.setContent {
            VehicleListItem(
                vehicle = vehicle,
                onVehicleClick = { clicked = true },
                onEditClick = { editCalled = true },
                onDeleteClick = { deleteCalled = true },
                canEdit = true,
                isLocal = true
            )
        }
        // Local icon visible
        composeRule.onNodeWithContentDescription("Local").assertIsDisplayed()
        // Edit and delete buttons visible by their contentDescriptions
        composeRule.onNodeWithContentDescription("Editar").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription("Excluir").assertIsDisplayed().performClick()
        // Click by plate text on card
        composeRule.onNodeWithText("AAA1111").performClick()
        assertTrue(editCalled && deleteCalled && clicked)
    }
}
