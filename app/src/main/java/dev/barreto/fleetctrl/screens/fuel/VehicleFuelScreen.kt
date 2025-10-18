package dev.barreto.fleetctrl.screens.fuel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.barreto.fleetctrl.R
import dev.barreto.fleetctrl.components.FuelRecordItem
import dev.barreto.fleetctrl.data.database.entities.FuelRecord
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import dev.barreto.fleetctrl.screens.SortType
import dev.barreto.fleetctrl.ui.theme.FleetCtrlTheme
import dev.barreto.fleetctrl.viewmodels.FuelViewModel
import java.time.LocalDateTime
import java.math.BigDecimal

/**
 * Tela de registros de abastecimento de um veículo específico
 */
@Composable
fun VehicleFuelScreen(
    vehicle: Vehicle,
    onBackClick: () -> Unit,
    onEditFuelRecord: (FuelRecord) -> Unit,
    onDeleteFuelRecord: (FuelRecord) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FuelViewModel,
    canEdit: Boolean = true
) {
    val fuelRecords by viewModel.fuelRecords.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingFuelRecords.collectAsStateWithLifecycle()
    val message by viewModel.fuelRecordsMessage.collectAsStateWithLifecycle()
    
    // Estado para ordenação
    var showSortMenu by remember { mutableStateOf(false) }
    var sortType by remember { mutableStateOf(SortType.DATE_DESC) }
    
    // Limpa mensagens automaticamente
    LaunchedEffect(message) {
        message?.let {
            kotlinx.coroutines.delay(3000)
            viewModel.clearFuelRecordsMessage()
        }
    }
    
    // Registra o veículo selecionado
    LaunchedEffect(vehicle) {
        viewModel.selectVehicle(vehicle)
    }
    
    // Aplica ordenação aos registros
    val sortedRecords = remember(fuelRecords, sortType) {
        when (sortType) {
            SortType.DATE_ASC -> fuelRecords.sortedBy { it.date }
            SortType.DATE_DESC -> fuelRecords.sortedByDescending { it.date }
            SortType.DISTANCE_ASC -> fuelRecords.sortedBy { it.quantity }
            SortType.DISTANCE_DESC -> fuelRecords.sortedByDescending { it.quantity }
            SortType.MILEAGE_ASC -> fuelRecords.sortedBy { it.totalCost }
            SortType.MILEAGE_DESC -> fuelRecords.sortedByDescending { it.totalCost }
        }
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Header com informações do veículo e botão de ordenação
        VehicleFuelHeader(
            vehicle = vehicle,
            onBackClick = onBackClick,
            onSortClick = { showSortMenu = true },
            showSortMenu = showSortMenu,
            sortType = sortType,
            onSortSelected = { newSort ->
                sortType = newSort
                showSortMenu = false
            },
            onDismissSortMenu = { showSortMenu = false }
        )
        
        // Conteúdo principal
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            sortedRecords.isEmpty() -> {
                EmptyFuelRecordsState()
            }
            
            else -> {
                val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
                val contentBottom = navBottomPadding + if (screenHeight < 640) 56.dp else 96.dp

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = contentBottom),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(sortedRecords) { record ->
                        FuelRecordItem(
                            record = record,
                            onEditClick = onEditFuelRecord,
                            onDeleteClick = onDeleteFuelRecord,
                            isLocal = record.organizationId.isNullOrBlank(),
                            canEdit = canEdit
                        )
                    }
                }
            }
        }
    }
    
    
    // Snackbar para mensagens
    message?.let { msg ->
        LaunchedEffect(msg) {
            // Snackbar será mostrado pelo MainNavigation
        }
    }
}

@Composable
private fun VehicleFuelHeader(
    vehicle: Vehicle,
    onBackClick: () -> Unit,
    onSortClick: () -> Unit,
    showSortMenu: Boolean,
    sortType: SortType,
    onSortSelected: (SortType) -> Unit,
    onDismissSortMenu: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar"
                )
            }
            
            Column {
                Text(
                    text = stringResource(R.string.fuel_vehicle_fuel_title, vehicle.plate),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${vehicle.brand} ${vehicle.model}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Box {
            IconButton(onClick = onSortClick) {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = "Ordenar"
                )
            }
            
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = onDismissSortMenu,
                modifier = Modifier.width(250.dp)
            ) {
                listOf(
                    SortType.DATE_ASC to "Data (Mais Antigo)",
                    SortType.DATE_DESC to "Data (Mais Recente)",
                    SortType.DISTANCE_ASC to "Quantidade (Menor)",
                    SortType.DISTANCE_DESC to "Quantidade (Maior)",
                    SortType.MILEAGE_ASC to "Custo (Menor)",
                    SortType.MILEAGE_DESC to "Custo (Maior)"
                ).forEach { (sort, displayName) ->
                    DropdownMenuItem(
                        text = { 
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(displayName)
                                if (sortType == sort) {
                                    Text("✓", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        },
                        onClick = { onSortSelected(sort) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyFuelRecordsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.fuel_empty_records),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.fuel_add_record),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}



@Preview(showBackground = true)
@Composable
private fun VehicleFuelScreenPreview() {
    FleetCtrlTheme {
        Text("Vehicle Fuel Screen Preview")
    }
}
