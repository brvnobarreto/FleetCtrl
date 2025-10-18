package dev.barreto.fleetctrl.screens.fleet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Sync
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
import dev.barreto.fleetctrl.components.*
import dev.barreto.fleetctrl.components.EmptyState
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import dev.barreto.fleetctrl.ui.theme.FleetCtrlTheme
import dev.barreto.fleetctrl.viewmodels.FleetViewModel
import android.util.Log

private enum class FilterMode { ALL, ORG, LOCAL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetScreen(
    modifier: Modifier = Modifier,
    fleetViewModel: FleetViewModel = viewModel(),
    onEditVehicle: (Vehicle) -> Unit = {},
    canEdit: Boolean = true,
    currentOrgId: String? = null
) {
    val vehicles by fleetViewModel.vehicles.collectAsState()
    val isLoading by fleetViewModel.isLoading.collectAsState()
    val errorMessage by fleetViewModel.errorMessage.collectAsState()
    val successMessage by fleetViewModel.successMessage.collectAsState()

    var selectedVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var vehicleToDelete by remember { mutableStateOf<Vehicle?>(null) }

    // Limpar mensagens automaticamente
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            kotlinx.coroutines.delay(3000)
            fleetViewModel.clearError()
        }
    }

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            kotlinx.coroutines.delay(3000)
            fleetViewModel.clearSuccessMessage()
        }
    }

    // Filtro: Todos / Organização / Locais
    var filter by remember { mutableStateOf(FilterMode.ALL) }

    val isInOrg = currentOrgId != null

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (vehicles.isEmpty() && !isLoading) {
            // Estado vazio
            EmptyState(
                modifier = Modifier.align(Alignment.Center),
                icon = Icons.Default.DirectionsCar,
                title = stringResource(R.string.fleet_empty_title),
                subtitle = stringResource(R.string.fleet_empty_subtitle),
                onSync = if (currentOrgId != null) { 
                    { fleetViewModel.syncVehicles() } 
                } else null,
                syncButtonText = "Sincronizar Veículos"
            )
        } else {
            // Lista de veículos
            // Espaço inferior responsivo para não colidir com o footer/FAB
            val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
            val contentBottom = navBottomPadding + if (screenHeight < 640) 56.dp else 96.dp

            // Cabeçalho de filtro
            Column(modifier = Modifier.fillMaxSize()) {
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

                val filteredVehicles = remember(vehicles, filter, currentOrgId) {
                    when (filter) {
                        FilterMode.ALL -> vehicles
                        FilterMode.ORG -> vehicles.filter { it.organizationId == currentOrgId }
                        FilterMode.LOCAL -> vehicles.filter { it.organizationId.isNullOrBlank() }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = contentBottom)
                ) {
                    items(filteredVehicles) { vehicle ->
                        VehicleListItem(
                            vehicle = vehicle,
                            onVehicleClick = { selectedVehicle = vehicle },
                            onEditClick = { if (canEdit) onEditVehicle(vehicle) },
                            onDeleteClick = { if (canEdit) vehicleToDelete = vehicle },
                            canEdit = canEdit,
                            isLocal = vehicle.organizationId.isNullOrBlank()
                        )
                    }
                }
            }
        }

        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }

    // Card de detalhes do veículo
    selectedVehicle?.let { vehicle ->
        VehicleDetailCard(
            vehicle = vehicle,
            onClose = { selectedVehicle = null },
            onEdit = {
                selectedVehicle = null
                onEditVehicle(vehicle)
            },
            onDelete = {
                selectedVehicle = null
                vehicleToDelete = vehicle
            },
            canEdit = canEdit
        )
    }

    // Dialog de confirmação de exclusão
    vehicleToDelete?.let { vehicle ->
        DeleteConfirmationDialog(
            vehicle = vehicle,
            onConfirm = {
                Log.d("DELETE_FLOW", "FleetScreen: Confirm button clicked for vehicle: ${vehicle.plate}")
                fleetViewModel.deleteVehicle(vehicle)
                vehicleToDelete = null
            },
            onDismiss = { vehicleToDelete = null }
        )
    }

    // Snackbar para mensagens de erro
    errorMessage?.let { message ->
        LaunchedEffect(message) {
            // Aqui você pode implementar um Snackbar
            fleetViewModel.clearError()
        }
    }

    // Snackbar para mensagens de sucesso
    successMessage?.let { message ->
        LaunchedEffect(message) {
            // Aqui você pode implementar um Snackbar
            fleetViewModel.clearSuccessMessage()
        }
    }
}


@Composable
private fun DeleteConfirmationDialog(
    vehicle: Vehicle,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.fleet_delete_title)) },
        text = {
            Text(stringResource(R.string.fleet_delete_message, vehicle.plate))
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.fleet_delete_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.fleet_delete_cancel))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun FleetScreenPreview() {
    FleetCtrlTheme {
        FleetScreen()
    }
}
