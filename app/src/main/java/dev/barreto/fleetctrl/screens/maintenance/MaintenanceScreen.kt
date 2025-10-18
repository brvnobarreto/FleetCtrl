package dev.barreto.fleetctrl.screens.maintenance

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.barreto.fleetctrl.R
import dev.barreto.fleetctrl.components.VehicleCard
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import dev.barreto.fleetctrl.screens.common.debugTouchLogger
import dev.barreto.fleetctrl.ui.theme.FleetCtrlTheme
import dev.barreto.fleetctrl.viewmodels.MaintenanceViewModel
import kotlinx.coroutines.launch

private enum class FilterMode { ALL, ORG, LOCAL }

/**
 * Tela principal de manutenção
 * Mostra lista de veículos para seleção
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MaintenanceScreen(
    modifier: Modifier = Modifier,
    maintenanceViewModel: MaintenanceViewModel = viewModel(),
    onVehicleClick: (Vehicle) -> Unit = {},
    currentOrgId: String? = null
) {
    val vehicles by maintenanceViewModel.vehicles.collectAsState()
    val isLoading by maintenanceViewModel.isLoadingVehicles.collectAsState()
    val errorMessage by maintenanceViewModel.vehiclesMessage.collectAsState()
    val successMessage by maintenanceViewModel.vehiclesMessage.collectAsState()

    // Lógica de Pull-to-Refresh
    val isRefreshing by maintenanceViewModel.isRefreshing.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val screenTag = "MaintenanceScreen"
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            Log.d(screenTag, "onRefresh invoked. vehicles=${vehicles.size}, isRefreshing=$isRefreshing")
            coroutineScope.launch {
                Log.d(screenTag, "Launching triggerRefresh() coroutine")
                maintenanceViewModel.triggerRefresh()
            }
        }
    )

    // Limpeza automática de mensagens
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            kotlinx.coroutines.delay(3000)
            maintenanceViewModel.clearVehiclesMessage()
        }
    }

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            kotlinx.coroutines.delay(3000)
            maintenanceViewModel.clearVehiclesMessage()
        }
    }

    var filter by remember { mutableStateOf(FilterMode.ALL) }
    val isInOrg = currentOrgId != null

    Box(modifier = modifier
        .fillMaxSize()
        .debugTouchLogger(screenTag)
        .pullRefresh(pullRefreshState)) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Filtros - sempre visíveis
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter == FilterMode.ALL,
                    onClick = { filter = FilterMode.ALL },
                    label = { Text("Todos") }
                )
                FilterChip(
                    selected = filter == FilterMode.ORG,
                    onClick = { if (isInOrg) filter = FilterMode.ORG },
                    enabled = isInOrg,
                    label = { Text("Organização") }
                )
                FilterChip(
                    selected = filter == FilterMode.LOCAL,
                    onClick = { filter = FilterMode.LOCAL },
                    label = { Text("Locais") }
                )
            }

            val filtered = remember(vehicles, filter, currentOrgId) {
                when (filter) {
                    FilterMode.ALL -> vehicles
                    FilterMode.ORG -> vehicles.filter { it.organizationId == currentOrgId }
                    FilterMode.LOCAL -> vehicles.filter { it.organizationId.isNullOrBlank() }
                }
            }

            val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
            val contentBottom = navBottomPadding + if (screenHeight < 640) 56.dp else 96.dp

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = contentBottom, start = 8.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when {
                    isLoading && !isRefreshing -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    filtered.isEmpty() -> {
                        item(span = { GridItemSpan(2) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyMaintenanceVehiclesState(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 32.dp),
                                    onSyncRequest = { maintenanceViewModel.triggerRefresh() }
                                )
                            }
                        }
                    }

                    else -> {
                        items(
                            items = filtered,
                            key = { vehicle -> vehicle.id }
                        ) { vehicle ->
                            VehicleCard(
                                vehicle = vehicle,
                                onVehicleClick = {
                                    maintenanceViewModel.selectVehicle(it)
                                    onVehicleClick(it)
                                },
                                isLocal = vehicle.organizationId.isNullOrBlank()
                            )
                        }
                    }
                }
            }
        }
        PullRefreshIndicator(isRefreshing, pullRefreshState, Modifier.align(Alignment.TopCenter))
    }
}

/**
 * Estado vazio quando não há veículos
 */
@Composable
private fun EmptyMaintenanceVehiclesState(
    modifier: Modifier = Modifier,
    onSyncRequest: () -> Unit
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Build,
            contentDescription = "Manutenção",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.maintenance_empty_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.maintenance_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(onClick = onSyncRequest) {
            Text("Sincronizar agora")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MaintenanceScreenPreview() {
    FleetCtrlTheme {
        EmptyMaintenanceVehiclesState(onSyncRequest = {})
    }
}
