package dev.barreto.fleetctrl.screens.fuel

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.barreto.fleetctrl.R
import dev.barreto.fleetctrl.components.VehicleCard
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import dev.barreto.fleetctrl.screens.common.debugTouchLogger
import dev.barreto.fleetctrl.ui.theme.FleetCtrlTheme
import dev.barreto.fleetctrl.viewmodels.FuelViewModel
import kotlinx.coroutines.launch

private enum class FilterMode { ALL, ORG, LOCAL }

/**
 * Tela principal de abastecimentos
 * Lista todos os veículos da frota para seleção
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FuelScreen(
    onVehicleClick: (Vehicle) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FuelViewModel,
    currentOrgId: String? = null
) {
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingVehicles.collectAsStateWithLifecycle()
    val message by viewModel.vehiclesMessage.collectAsStateWithLifecycle()

    // Lógica de Pull-to-Refresh
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val screenTag = "FuelScreen"
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            Log.d(screenTag, "onRefresh invoked. vehicles=${vehicles.size}, isRefreshing=$isRefreshing")
            coroutineScope.launch {
                Log.d(screenTag, "Launching triggerRefresh() coroutine")
                viewModel.triggerRefresh()
            }
        }
    )

    // Limpa mensagens automaticamente
    LaunchedEffect(message) {
        message?.let {
            kotlinx.coroutines.delay(3000)
            viewModel.clearVehiclesMessage()
        }
    }

    var filter by remember { mutableStateOf(FilterMode.ALL) }
    val isInOrg = currentOrgId != null

    Box(modifier = modifier
        .fillMaxSize()
        .debugTouchLogger(screenTag)
        .pullRefresh(pullRefreshState)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp)
        ) {
            // Filtros
            var showSearch by remember { mutableStateOf(false) }
            var searchQuery by remember { mutableStateOf("") }

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
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { showSearch = !showSearch }) {
                    Icon(Icons.Default.Search, contentDescription = "Buscar")
                }
            }
            if (showSearch) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    singleLine = true,
                    label = { Text("Buscar veículo") }
                )
            }
            val filtered = remember(vehicles, filter, currentOrgId, searchQuery) {
                val base = when (filter) {
                    FilterMode.ALL -> vehicles
                    FilterMode.ORG -> vehicles.filter { it.organizationId == currentOrgId }
                    FilterMode.LOCAL -> vehicles.filter { it.organizationId.isNullOrBlank() }
                }
                if (searchQuery.isBlank()) base else base.filter { dev.barreto.fleetctrl.utils.SearchIndex.vehicleMatches(it, searchQuery) }
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
                                EmptyFuelVehiclesState(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    onSyncRequest = { viewModel.triggerRefresh() }
                                )
                            }
                        }
                    }

                    else -> {
                        items(filtered, key = { it.id }) { vehicle ->
                            VehicleCard(
                                vehicle = vehicle,
                                onVehicleClick = { onVehicleClick(vehicle) },
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

@Composable
private fun EmptyFuelVehiclesState(
    modifier: Modifier = Modifier,
    onSyncRequest: () -> Unit
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.fuel_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.fuel_empty_description),
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
private fun FuelScreenPreview() {
    FleetCtrlTheme {
        EmptyFuelVehiclesState(onSyncRequest = {})
    }
}
