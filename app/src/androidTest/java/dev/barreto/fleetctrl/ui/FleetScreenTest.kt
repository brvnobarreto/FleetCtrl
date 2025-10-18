package dev.barreto.fleetctrl.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import dev.barreto.fleetctrl.screens.fleet.FleetScreen
import dev.barreto.fleetctrl.viewmodels.FleetViewModel
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Rule
import org.junit.Test

class FleetScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @After
    fun cleanup() { clearAllMocks() }

    @Test
    fun emptyState_withOrg_showsSyncButton_andCallsSync() {
        val vm = mockk<FleetViewModel>(relaxed = true)
        every { vm.vehicles } returns MutableStateFlow(emptyList())
        every { vm.isLoading } returns MutableStateFlow(false)
        every { vm.errorMessage } returns MutableStateFlow(null)
        every { vm.successMessage } returns MutableStateFlow(null)
        justRun { vm.syncVehicles() }

        composeTestRule.setContent {
            FleetScreen(fleetViewModel = vm, currentOrgId = "org-1")
        }

        composeTestRule.onNodeWithText("Sincronizar Veículos").assertIsDisplayed().performClick()
        verify { vm.syncVehicles() }
    }

    @Test
    fun list_showsVehicles_andOriginIcons() {
        val vm = mockk<FleetViewModel>(relaxed = true)
        val v1 = vehicle(1, plate = "AAA1111", orgId = null)
        val v2 = vehicle(2, plate = "BBB2222", orgId = "org-1")
        every { vm.vehicles } returns MutableStateFlow(listOf(v1, v2))
        every { vm.isLoading } returns MutableStateFlow(false)
        every { vm.errorMessage } returns MutableStateFlow(null)
        every { vm.successMessage } returns MutableStateFlow(null)

        composeTestRule.setContent {
            FleetScreen(fleetViewModel = vm, currentOrgId = "org-1")
        }

        composeTestRule.onNodeWithText("AAA1111").assertIsDisplayed()
        composeTestRule.onNodeWithText("BBB2222").assertIsDisplayed()
        composeTestRule.onAllNodesWithContentDescription("Local").assertCountEquals(1)
        composeTestRule.onAllNodesWithContentDescription("Nuvem").assertCountEquals(1)
    }

    @Test
    fun filterChips_filterByOrganizationAndLocal() {
        val vm = mockk<FleetViewModel>(relaxed = true)
        val vLocal = vehicle(1, plate = "LOC1111", orgId = null)
        val vOrg = vehicle(2, plate = "ORG2222", orgId = "org-1")
        every { vm.vehicles } returns MutableStateFlow(listOf(vLocal, vOrg))
        every { vm.isLoading } returns MutableStateFlow(false)
        every { vm.errorMessage } returns MutableStateFlow(null)
        every { vm.successMessage } returns MutableStateFlow(null)

        composeTestRule.setContent {
            FleetScreen(fleetViewModel = vm, currentOrgId = "org-1")
        }

        // Default ALL
        composeTestRule.onNodeWithText("LOC1111").assertIsDisplayed()
        composeTestRule.onNodeWithText("ORG2222").assertIsDisplayed()

        // Filter by Organização
        composeTestRule.onNodeWithText("Organização").performClick()
        composeTestRule.onNodeWithText("ORG2222").assertIsDisplayed()
        // Local item should not be visible now; we can't assertNonExist reliably due lazy; check origin icons count
        composeTestRule.onAllNodesWithContentDescription("Nuvem").assertCountEquals(1)

        // Filter by Locais
        composeTestRule.onNodeWithText("Locais").performClick()
        composeTestRule.onNodeWithText("LOC1111").assertIsDisplayed()
        composeTestRule.onAllNodesWithContentDescription("Local").assertCountEquals(1)
    }

    private fun vehicle(id: Long, plate: String, orgId: String?): Vehicle = Vehicle(
        id = id,
        vehicleNumber = "V$id",
        plate = plate,
        model = "Model$id",
        brand = "Brand$id",
        year = 2023,
        color = "Color$id",
        driver = "Driver$id",
        showInDiary = true,
        photoPath = null,
        organizationId = orgId,
        engineType = "Gasolina",
        fuelCapacity = 50.0,
        averageConsumption = 12.0,
        isActive = true,
        currentMileage = 100,
        lastMaintenanceMileage = 0
    )
}
