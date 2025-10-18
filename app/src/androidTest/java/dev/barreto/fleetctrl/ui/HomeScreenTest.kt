package dev.barreto.fleetctrl.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import dev.barreto.fleetctrl.data.database.entities.Organization
import dev.barreto.fleetctrl.data.repositories.HomeSummary
import dev.barreto.fleetctrl.data.repositories.SummaryMetrics
import dev.barreto.fleetctrl.screens.home.HomeScreen
import dev.barreto.fleetctrl.viewmodels.HomeUiState
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime

class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsEmptyState_whenNoSummaryAndNotLoading() {
        composeRule.setContent {
            HomeScreen(
                state = HomeUiState(isLoading = false, summary = null),
                currentOrganization = null,
                onRefresh = {},
                onNavigateToFleet = {},
                onNavigateToDiary = {},
                onNavigateToFuel = {},
                onNavigateToMaintenance = {}
            )
        }
        composeRule.onNodeWithText("Sincronizar agora").assertIsDisplayed()
    }

    @Test
    fun showsSummarySections_whenSummaryProvided() {
        val summary = HomeSummary(
            overall = SummaryMetrics(vehicleCount = 2, activeVehicleCount = 1, averageMileage = 10.0, tripCount = 1, totalDistanceKm = 50, fuelRecordCount = 1, totalFuelCost = 10.0, totalFuelQuantity = 5.0, maintenanceCount = 1, openMaintenanceCount = 0, latestUpdate = LocalDateTime.now()),
            local = SummaryMetrics(vehicleCount = 1, activeVehicleCount = 1, averageMileage = 5.0, tripCount = 1, totalDistanceKm = 25, fuelRecordCount = 1, totalFuelCost = 5.0, totalFuelQuantity = 2.5, maintenanceCount = 1, openMaintenanceCount = 0, latestUpdate = LocalDateTime.now()),
            organization = SummaryMetrics(vehicleCount = 1, activeVehicleCount = 0, averageMileage = 5.0, tripCount = 0, totalDistanceKm = 25, fuelRecordCount = 0, totalFuelCost = 0.0, totalFuelQuantity = 0.0, maintenanceCount = 0, openMaintenanceCount = 0, latestUpdate = LocalDateTime.now()),
            activeOrganizations = 1,
            activeMemberships = 1,
            activeUsers = 1,
            currentOrganizationMemberCount = 1,
            lastUpdated = LocalDateTime.now()
        )
        composeRule.setContent {
            HomeScreen(
                state = HomeUiState(isLoading = false, summary = summary, lastUpdated = LocalDateTime.now()),
                currentOrganization = Organization(id = "org-1", code = "C1", name = "Org", ownerId = "u1", ownerEmail = "u1@x.com"),
                onRefresh = {},
                onNavigateToFleet = {},
                onNavigateToDiary = {},
                onNavigateToFuel = {},
                onNavigateToMaintenance = {}
            )
        }
        // Check one label occurrence
        composeRule.onAllNodesWithText("Veículos").onFirst().assertIsDisplayed()
    }
}
