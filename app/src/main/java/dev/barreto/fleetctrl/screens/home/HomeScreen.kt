package dev.barreto.fleetctrl.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.barreto.fleetctrl.R
import dev.barreto.fleetctrl.data.database.entities.Organization
import dev.barreto.fleetctrl.data.repositories.HomeSummary
import dev.barreto.fleetctrl.data.repositories.SummaryMetrics
import dev.barreto.fleetctrl.ui.theme.FleetCtrlTheme
import dev.barreto.fleetctrl.viewmodels.HomeUiState
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    currentOrganization: Organization?,
    onRefresh: () -> Unit,
    onNavigateToFleet: () -> Unit,
    onNavigateToDiary: () -> Unit,
    onNavigateToFuel: () -> Unit,
    onNavigateToMaintenance: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pullRefreshState = rememberPullRefreshState(state.isLoading, onRefresh)

    Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        when {
            state.summary == null && state.isLoading -> {
                LoadingState()
            }

            state.summary == null -> {
                EmptyHomeState(onRefresh = onRefresh)
            }

            else -> {
                SummaryContent(
                    summary = state.summary,
                    currentOrganization = currentOrganization,
                    lastUpdated = state.lastUpdated,
                    onNavigateToFleet = onNavigateToFleet,
                    onNavigateToDiary = onNavigateToDiary,
                    onNavigateToFuel = onNavigateToFuel,
                    onNavigateToMaintenance = onNavigateToMaintenance,
                    onRefresh = onRefresh
                )
            }
        }

        PullRefreshIndicator(
            refreshing = state.isLoading,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun SummaryContent(
    summary: HomeSummary,
    currentOrganization: Organization?,
    lastUpdated: LocalDateTime?,
    onNavigateToFleet: () -> Unit,
    onNavigateToDiary: () -> Unit,
    onNavigateToFuel: () -> Unit,
    onNavigateToMaintenance: () -> Unit,
    onRefresh: () -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm") }
    val lastUpdateText = lastUpdated?.let { formatter.format(it) }

    val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
    val contentBottom = navBottomPadding + if (screenHeight < 640) 56.dp else 96.dp

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = contentBottom)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.title_home),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                lastUpdateText?.let { text ->
                    Text(
                        text = stringResource(R.string.home_last_update, text),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            SummarySection(
                title = stringResource(R.string.home_section_overview),
                metrics = summary.overall
            )
        }

        if (summary.organization != null && currentOrganization != null) {
            item {
                SummarySection(
                    title = buildString {
                        append(stringResource(R.string.home_section_org))
                        append(" • ")
                        append(currentOrganization.name)
                    },
                    subtitle = stringResource(R.string.home_section_org_members, summary.currentOrganizationMemberCount),
                    metrics = summary.organization
                )
            }
        }

        if (summary.local.vehicleCount > 0 || summary.local.tripCount > 0 || summary.local.fuelRecordCount > 0 || summary.local.maintenanceCount > 0) {
            item {
                SummarySection(
                    title = stringResource(R.string.home_section_local),
                    metrics = summary.local
                )
            }
        }

        item {
            MemberStatsCard(
                organizations = summary.activeOrganizations,
                memberships = summary.activeMemberships,
                users = summary.activeUsers
            )
        }

        item {
            OutlinedButton(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.home_sync_button))
            }
        }
    }
}

@Composable
private fun SummarySection(
    title: String,
    metrics: SummaryMetrics,
    subtitle: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val items = listOf(
            SummaryItemData(
                label = stringResource(R.string.home_metric_vehicles),
                value = formatInt(metrics.vehicleCount)
            ),
            SummaryItemData(
                label = stringResource(R.string.home_metric_active_vehicles),
                value = formatInt(metrics.activeVehicleCount)
            ),
            SummaryItemData(
                label = stringResource(R.string.home_metric_avg_mileage),
                value = formatKm(metrics.averageMileage)
            ),
            SummaryItemData(
                label = stringResource(R.string.home_metric_trips),
                value = formatInt(metrics.tripCount)
            ),
            SummaryItemData(
                label = stringResource(R.string.home_metric_distance),
                value = formatDistance(metrics.totalDistanceKm)
            ),
            SummaryItemData(
                label = stringResource(R.string.home_metric_fuel_records),
                value = formatInt(metrics.fuelRecordCount)
            ),
            SummaryItemData(
                label = stringResource(R.string.home_metric_fuel_cost),
                value = formatCurrency(metrics.totalFuelCost)
            ),
            SummaryItemData(
                label = stringResource(R.string.home_metric_fuel_volume),
                value = formatLiters(metrics.totalFuelQuantity)
            ),
            SummaryItemData(
                label = stringResource(R.string.home_metric_maintenances),
                value = formatInt(metrics.maintenanceCount)
            ),
            SummaryItemData(
                label = stringResource(R.string.home_metric_open_maintenances),
                value = formatInt(metrics.openMaintenanceCount)
            )
        )

        SummaryGrid(items)
    }
}

@Composable
private fun SummaryGrid(items: List<SummaryItemData>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    SummaryCard(item, Modifier.weight(1f))
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(data: SummaryItemData, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = data.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = data.value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MemberStatsCard(organizations: Int, memberships: Int, users: Int) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.home_member_summary_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.home_header_orgs, organizations),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = stringResource(R.string.home_header_members, memberships),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = stringResource(R.string.home_header_users, users),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyHomeState(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.home_empty_message),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onRefresh) {
            Text(stringResource(R.string.home_sync_button))
        }
    }
}

private data class SummaryItemData(
    val label: String,
    val value: String
)

private fun formatInt(value: Int): String = NumberFormat.getIntegerInstance().format(value)

private fun formatKm(value: Double): String {
    val formatter = NumberFormat.getNumberInstance().apply {
        maximumFractionDigits = 1
        minimumFractionDigits = 0
    }
    return formatter.format(value) + " km"
}

private fun formatDistance(value: Long): String = NumberFormat.getNumberInstance().format(value) + " km"

private fun formatCurrency(value: Double): String = NumberFormat.getCurrencyInstance().format(value)

private fun formatLiters(value: Double): String {
    val formatter = NumberFormat.getNumberInstance().apply {
        maximumFractionDigits = 1
        minimumFractionDigits = 0
    }
    return formatter.format(value) + " L"
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    FleetCtrlTheme {
        HomeScreen(
            state = HomeUiState(
                isLoading = false,
                summary = HomeSummary(
                    overall = SummaryMetrics(
                        vehicleCount = 8,
                        activeVehicleCount = 6,
                        averageMileage = 48200.0,
                        tripCount = 152,
                        totalDistanceKm = 38200,
                        fuelRecordCount = 97,
                        totalFuelCost = 15873.45,
                        totalFuelQuantity = 2450.0,
                        maintenanceCount = 34,
                        openMaintenanceCount = 2,
                        latestUpdate = LocalDateTime.now()
                    ),
                    local = SummaryMetrics(
                        vehicleCount = 2,
                        activeVehicleCount = 2,
                        averageMileage = 12000.0,
                        tripCount = 12,
                        totalDistanceKm = 1800,
                        fuelRecordCount = 10,
                        totalFuelCost = 3200.0,
                        totalFuelQuantity = 400.0,
                        maintenanceCount = 4,
                        openMaintenanceCount = 1,
                        latestUpdate = LocalDateTime.now()
                    ),
                    organization = SummaryMetrics(
                        vehicleCount = 6,
                        activeVehicleCount = 4,
                        averageMileage = 56000.0,
                        tripCount = 140,
                        totalDistanceKm = 36400,
                        fuelRecordCount = 87,
                        totalFuelCost = 12673.45,
                        totalFuelQuantity = 2050.0,
                        maintenanceCount = 30,
                        openMaintenanceCount = 1,
                        latestUpdate = LocalDateTime.now()
                    ),
                    activeOrganizations = 2,
                    activeMemberships = 12,
                    activeUsers = 8,
                    currentOrganizationMemberCount = 6,
                    lastUpdated = LocalDateTime.now()
                ),
                lastUpdated = LocalDateTime.now()
            ),
            currentOrganization = Organization(
                id = "org-1",
                code = "ABC123",
                name = "Org de Teste",
                ownerId = "owner",
                ownerEmail = "owner@example.com"
            ),
            onRefresh = {},
            onNavigateToFleet = {},
            onNavigateToDiary = {},
            onNavigateToFuel = {},
            onNavigateToMaintenance = {}
        )
    }
}
