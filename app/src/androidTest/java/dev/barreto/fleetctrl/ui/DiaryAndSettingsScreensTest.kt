package dev.barreto.fleetctrl.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import dev.barreto.fleetctrl.screens.diary.DiaryScreen
import dev.barreto.fleetctrl.screens.diary.VehicleActivityScreen
import dev.barreto.fleetctrl.screens.settings.ConnectivityScreen
import dev.barreto.fleetctrl.screens.settings.SettingsScreen
import dev.barreto.fleetctrl.viewmodels.DiaryViewModel
import dev.barreto.fleetctrl.viewmodels.OrganizationViewModel
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class DiaryAndSettingsScreensTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun diary_emptyState_showsSyncButton_andFilters() {
        val vm = mockk<DiaryViewModel>(relaxed = true)
        every { vm.vehicles } returns MutableStateFlow(emptyList())
        every { vm.isLoading } returns MutableStateFlow(false)
        every { vm.errorMessage } returns MutableStateFlow(null)
        every { vm.successMessage } returns MutableStateFlow(null)
        every { vm.isRefreshing } returns MutableStateFlow(false)
        justRun { vm.triggerRefresh() }

        composeRule.setContent {
            DiaryScreen(diaryViewModel = vm)
        }

        composeRule.onNodeWithText("Sincronizar agora").assertIsDisplayed()
        composeRule.onNodeWithText("Todos").assertIsDisplayed()
        composeRule.onNodeWithText("Organização").assertIsDisplayed()
        composeRule.onNodeWithText("Locais").assertIsDisplayed()
    }

    @Test
    fun vehicleActivity_empty_andSortMenu() {
        val vm = mockk<DiaryViewModel>(relaxed = true)
        every { vm.activityRecords } returns MutableStateFlow(emptyList())
        every { vm.isLoading } returns MutableStateFlow(false)
        every { vm.errorMessage } returns MutableStateFlow(null)
        every { vm.successMessage } returns MutableStateFlow(null)
        justRun { vm.selectVehicle(any()) }

        composeRule.setContent {
            VehicleActivityScreen(
                vehicle = vehicle("AAA1111"),
                onBackClick = {},
                onAddRecord = {},
                diaryViewModel = vm
            )
        }

        composeRule.onNodeWithContentDescription("Ordenar registros").assertIsDisplayed()
    }

    @Test
    fun connectivity_headerAndActions() {
        val orgVm = mockk<OrganizationViewModel>(relaxed = true)
        every { orgVm.userOrganizations } returns MutableStateFlow(emptyList())
        every { orgVm.currentOrganization } returns MutableStateFlow(null)
        every { orgVm.isLoading } returns MutableStateFlow(false)
        every { orgVm.message } returns MutableStateFlow(null)
        every { orgVm.error } returns MutableStateFlow(null)
        every { orgVm.isRefreshing } returns MutableStateFlow(false)
        justRun { orgVm.loadUserOrganizations() }

        composeRule.setContent {
            ConnectivityScreen(onBack = {}, organizationViewModel = orgVm)
        }

        composeRule.onNodeWithContentDescription("Voltar").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Atualizar").assertIsDisplayed()
        composeRule.onNodeWithText("Criar").assertIsDisplayed()
        composeRule.onNodeWithText("Entrar").assertIsDisplayed()
    }

    // Removed: SettingsScreen requires real ViewModels without Hilt Test setup

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
}
