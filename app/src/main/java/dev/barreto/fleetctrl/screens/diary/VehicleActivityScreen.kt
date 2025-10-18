package dev.barreto.fleetctrl.screens.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.barreto.fleetctrl.R
import dev.barreto.fleetctrl.components.ActivityRecordItem
import dev.barreto.fleetctrl.data.database.entities.ActivityRecord
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import dev.barreto.fleetctrl.screens.SortType
import dev.barreto.fleetctrl.ui.theme.FleetCtrlTheme
import dev.barreto.fleetctrl.viewmodels.DiaryViewModel


/**
 * Tela de registros de atividade de um veículo específico
 */
@Composable
fun VehicleActivityScreen(
    vehicle: Vehicle,
    onBackClick: () -> Unit,
    onAddRecord: (Vehicle) -> Unit,
    modifier: Modifier = Modifier,
    diaryViewModel: DiaryViewModel = viewModel(),
    canEdit: Boolean = true
) {
    val activityRecords by diaryViewModel.activityRecords.collectAsState()
    val isLoading by diaryViewModel.isLoading.collectAsState()
    val errorMessage by diaryViewModel.errorMessage.collectAsState()
    val successMessage by diaryViewModel.successMessage.collectAsState()
    
    // Estado para edição
    var recordToEdit by remember { mutableStateOf<ActivityRecord?>(null) }
    
    // Estado para ordenação
    var sortType by remember { mutableStateOf(SortType.DATE_DESC) }
    var showSortMenu by remember { mutableStateOf(false) }
    
    // Ordenar registros baseado no tipo selecionado
    val sortedRecords = remember(activityRecords, sortType) {
        when (sortType) {
            SortType.DATE_DESC -> activityRecords.sortedByDescending { it.date }
            SortType.DATE_ASC -> activityRecords.sortedBy { it.date }
            SortType.DISTANCE_DESC -> activityRecords.sortedByDescending { it.distance }
            SortType.DISTANCE_ASC -> activityRecords.sortedBy { it.distance }
            SortType.MILEAGE_DESC -> activityRecords.sortedByDescending { it.endMileage }
            SortType.MILEAGE_ASC -> activityRecords.sortedBy { it.endMileage }
        }
    }
    
    // Selecionar o veículo no ViewModel
    LaunchedEffect(vehicle) {
        diaryViewModel.selectVehicle(vehicle)
    }
    
    // Limpeza automática de mensagens
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            kotlinx.coroutines.delay(3000)
            diaryViewModel.clearError()
        }
    }
    
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            kotlinx.coroutines.delay(3000)
            diaryViewModel.clearSuccessMessage()
        }
    }
    
    Box(
        modifier = modifier.fillMaxSize()
            //.background()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header com informações do veículo
            VehicleActivityHeader(
                vehicle = vehicle,
                onBackClick = onBackClick,
                onSortClick = { showSortMenu = true },
                showSortMenu = showSortMenu,
                sortType = sortType,
                onSortSelected = { newSort ->
                    sortType = newSort
                    showSortMenu = false
                },
                onDismissSortMenu = { showSortMenu = false },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Conteúdo principal
            when {
                isLoading -> {
                    LoadingContent()
                }
                sortedRecords.isEmpty() -> {
                    EmptyActivityState(
                        vehicle = vehicle,
                        modifier = Modifier.weight(1f),
                        onSyncRequest = { diaryViewModel.triggerRefresh() }
                    )
                }
                else -> {
                    // Lista de registros de atividade
                    val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
                    val contentBottom = navBottomPadding + if (screenHeight < 640) 56.dp else 96.dp

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = contentBottom)
                    ) {
                        items(
                            items = sortedRecords,
                            key = { record -> record.id }
                        ) { record ->
                            ActivityRecordItem(
                                record = record,
                                onEditClick = { recordToEdit = it },
                                onDeleteClick = { diaryViewModel.deleteActivityRecord(record) },
                                isLocal = record.organizationId.isNullOrBlank(),
                                canEdit = canEdit
                            )
                        }
                    }
                }
            }
        }
        
        
        // Mensagens de feedback
        errorMessage?.let { message ->
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        
        successMessage?.let { message ->
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        
        // Dialog de edição de registro
        recordToEdit?.let { record ->
            dev.barreto.fleetctrl.components.ActivityRecordDialog(
                vehicle = vehicle,
                activityRecord = record,
                onDismiss = { recordToEdit = null },
                onSave = { updatedRecord ->
                    diaryViewModel.updateActivityRecord(updatedRecord)
                    recordToEdit = null
                }
            )
        }
        
    }
}

/**
 * Header da tela com informações do veículo
 */
@Composable
private fun VehicleActivityHeader(
    vehicle: Vehicle,
    onBackClick: () -> Unit,
    onSortClick: () -> Unit,
    showSortMenu: Boolean,
    sortType: SortType,
    onSortSelected: (SortType) -> Unit,
    onDismissSortMenu: () -> Unit,
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
        
        Box {
            IconButton(onClick = onSortClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Ordenar registros",
                    tint = MaterialTheme.colorScheme.primary
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
                    SortType.DISTANCE_ASC to "Distância (Menor)",
                    SortType.DISTANCE_DESC to "Distância (Maior)",
                    SortType.MILEAGE_ASC to "Quilometragem (Menor)",
                    SortType.MILEAGE_DESC to "Quilometragem (Maior)"
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

/**
 * Estado vazio quando não há registros de atividade
 */
@Composable
private fun EmptyActivityState(
    vehicle: Vehicle,
    modifier: Modifier = Modifier,
    onSyncRequest: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.DirectionsCar,
            contentDescription = stringResource(R.string.cd_car),
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.activity_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.activity_empty_subtitle, vehicle.plate),
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

/**
 * Conteúdo de carregamento
 */
@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}


@Preview(showBackground = true)
@Composable
private fun VehicleActivityScreenPreview() {
    FleetCtrlTheme {
        VehicleActivityScreen(
            vehicle = Vehicle(
                id = 1,
                vehicleNumber = "V001",
                plate = "ABC1234",
                model = "Civic",
                brand = "Honda",
                year = 2023,
                color = "Branco",
                driver = "João Silva",
                showInDiary = true,
                isActive = true,
                engineType = "Gasolina",
                fuelCapacity = 50.0,
                averageConsumption = 12.0
            ),
            onBackClick = {},
            onAddRecord = {}
        )
    }
}
