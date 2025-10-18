package dev.barreto.fleetctrl.screens.maintenance

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.barreto.fleetctrl.R
import dev.barreto.fleetctrl.components.MaintenanceRecordDialog
import dev.barreto.fleetctrl.data.database.entities.MaintenanceRecord
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import dev.barreto.fleetctrl.ui.theme.FleetCtrlTheme
import dev.barreto.fleetctrl.viewmodels.MaintenanceViewModel
import java.time.format.DateTimeFormatter

/**
 * Tela de registros de manutenção de um veículo específico
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VehicleMaintenanceScreen(
    vehicle: Vehicle,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    maintenanceViewModel: MaintenanceViewModel = viewModel(),
    canEdit: Boolean = true
) {
    val maintenanceRecords by maintenanceViewModel.maintenanceRecords.collectAsState()
    val isLoading by maintenanceViewModel.isLoadingMaintenanceRecords.collectAsState()
    val lastMileage by maintenanceViewModel.lastMileage.collectAsState()

    // Estado para edição
    var recordToEdit by remember { mutableStateOf<MaintenanceRecord?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<MaintenanceRecord?>(null) }

    // Estado do carrossel
    val pagerState = rememberPagerState(pageCount = { maintenanceRecords.size })

    // Seleciona o veículo e carrega os registros
    LaunchedEffect(vehicle) {
        maintenanceViewModel.selectVehicle(vehicle)
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Header com informações do veículo
        VehicleMaintenanceHeader(
            vehicle = vehicle,
            onBackClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
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

            maintenanceRecords.isEmpty() -> {
                EmptyMaintenanceRecordsState(vehicle = vehicle)
            }

            else -> {
                val navBottomPadding =
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                val screenHeight =
                    androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
                val contentBottom = navBottomPadding + if (screenHeight < 640) 56.dp else 96.dp

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = contentBottom)
                ) {
                    item {
                        // Card carrossel
                        MaintenanceCarouselCard(
                            records = maintenanceRecords,
                            lastMileage = lastMileage,
                            pagerState = pagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // History Title
                    item {
                        Text(
                            text = stringResource(R.string.maintenance_history_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 8.dp)
                        )
                    }


                    // Lista de histórico
                    items(maintenanceRecords) { record ->
                        MaintenanceRecordItem(
                            record = record,
                            onEditClick = { recordToEdit = it },
                            onDeleteClick = {
                                recordToDelete = it
                                showDeleteConfirmation = true
                            },
                            canEdit = canEdit,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }

    // Dialog de edição
    recordToEdit?.let { record ->
        MaintenanceRecordDialog(
            vehicle = vehicle,
            maintenanceRecord = record,
            onDismiss = { recordToEdit = null },
            onSave = { updatedRecord ->
                maintenanceViewModel.updateMaintenanceRecord(updatedRecord)
                recordToEdit = null
            }
        )
    }

    // Dialog de confirmação de exclusão
    if (showDeleteConfirmation && recordToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmation = false
                recordToDelete = null
            },
            title = {
                Text(stringResource(R.string.maintenance_confirm_delete_title))
            },
            text = {
                Text(stringResource(R.string.maintenance_confirm_delete_message))
            },
            confirmButton = {
                Button(
                    onClick = {
                        recordToDelete?.let { record ->
                            maintenanceViewModel.deleteMaintenanceRecord(record)
                        }
                        showDeleteConfirmation = false
                        recordToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.maintenance_confirm_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        recordToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.maintenance_confirm_delete_cancel))
                }
            }
        )
    }
}

/**
 * Header da tela com informações do veículo
 */
@Composable
private fun VehicleMaintenanceHeader(
    vehicle: Vehicle,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_back)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = vehicle.plate,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "${vehicle.brand} ${vehicle.model} ${vehicle.year}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Card carrossel com informações da próxima revisão
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MaintenanceCarouselCard(
    records: List<MaintenanceRecord>,
    lastMileage: Long?,
    pagerState: androidx.compose.foundation.pager.PagerState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Título com indicador de página
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (records.isEmpty())
                        stringResource(R.string.maintenance_carousel_no_records)
                    else
                        stringResource(R.string.maintenance_carousel_record_count),
                    // stringResource(R.string.maintenance_carousel_record_count, pagerState.currentPage + 1, records.size),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Indicadores de página
                if (records.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(records.size) { index ->
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (index == pagerState.currentPage)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Carrossel com HorizontalPager
            if (records.isNotEmpty()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    val record = records[page]

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Data do registro
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(
                                    R.string.maintenance_carousel_date,
                                    record.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Quilometragem da revisão
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(
                                    R.string.maintenance_carousel_mileage,
                                    record.mileage
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Próxima revisão (km)
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Build,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(
                                    R.string.maintenance_carousel_next_revision,
                                    record.nextMaintenanceMileage?.let {
                                        stringResource(
                                            R.string.maintenance_carousel_km_format,
                                            it
                                        )
                                    } ?: stringResource(R.string.maintenance_carousel_na)
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Distância até concessionária
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(
                                    R.string.maintenance_carousel_distance_to_dealer,
                                    record.distanceToDealer?.let {
                                        stringResource(
                                            R.string.maintenance_carousel_km_format,
                                            it
                                        )
                                    } ?: stringResource(R.string.maintenance_carousel_na)
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // KM atual (do diário)
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(
                                    R.string.maintenance_carousel_current_mileage,
                                    lastMileage?.toString()
                                        ?: stringResource(R.string.maintenance_carousel_na)
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Data mínima de revisão
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(
                                    R.string.maintenance_carousel_min_date,
                                    record.minRevisionDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                        ?: stringResource(R.string.maintenance_carousel_na)
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Data máxima de revisão
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(
                                    R.string.maintenance_carousel_max_date,
                                    record.maxRevisionDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                        ?: stringResource(R.string.maintenance_carousel_na)
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Observações
                        if (!record.notes.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Build,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(
                                        R.string.maintenance_carousel_observations,
                                        record.notes
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                // Estado vazio
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.maintenance_carousel_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Item da lista de registros de manutenção
 */
@Composable
private fun MaintenanceRecordItem(
    record: MaintenanceRecord,
    onEditClick: (MaintenanceRecord) -> Unit,
    onDeleteClick: (MaintenanceRecord) -> Unit,
    modifier: Modifier = Modifier,
    canEdit: Boolean = true
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Data
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = record.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            // Quilometragem
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${record.mileage} km",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Observações
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = record.notes
                        ?: stringResource(R.string.maintenance_history_no_observations),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Botões de ação
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicador de origem
                val isLocal = record.organizationId.isNullOrBlank()
                if (isLocal) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = "Local",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = "Nuvem",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(2.dp))
                if (canEdit) {
                    IconButton(
                        onClick = { onEditClick(record) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.cd_edit_maintenance),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { onDeleteClick(record) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.cd_delete_maintenance),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

/**
 * Estado vazio quando não há registros de manutenção
 */
@Composable
private fun EmptyMaintenanceRecordsState(
    vehicle: Vehicle,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
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
            text = stringResource(R.string.maintenance_empty_records),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.maintenance_empty_records_subtitle, vehicle.plate),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VehicleMaintenanceScreenPreview() {
    FleetCtrlTheme {
        VehicleMaintenanceScreen(
            vehicle = Vehicle(
                id = 1,
                vehicleNumber = "001",
                plate = "ABC1234",
                model = "Civic",
                brand = "Honda",
                year = 2020,
                color = "Branco",
                driver = "João Silva",
                showInDiary = true,
                engineType = "Flex",
                fuelCapacity = 50.0,
                averageConsumption = 12.0,
                currentMileage = 50000
            ),
            onBackClick = {}
        )
    }
}
