package dev.barreto.fleetctrl.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.barreto.fleetctrl.R
import dev.barreto.fleetctrl.data.database.entities.ActivityRecord
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Dialog para cadastro e edição de registros de atividade
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityRecordDialog(
    vehicle: Vehicle,
    lastMileage: Long? = null,
    activityRecord: ActivityRecord? = null, // Para edição
    onDismiss: () -> Unit,
    onSave: (ActivityRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    // Usar keys para evitar recomposições desnecessárias
    val vehicleId = vehicle.id
    val recordId = activityRecord?.id ?: 0L
    
    var driver by remember(recordId) { 
        mutableStateOf(activityRecord?.driver ?: vehicle.driver) 
    }
    var startMileage by remember(recordId) { 
        mutableStateOf(activityRecord?.startMileage?.toString() ?: lastMileage?.toString() ?: "") 
    }
    var endMileage by remember(recordId) { 
        mutableStateOf(activityRecord?.endMileage?.toString() ?: "") 
    }
    var observation by remember(recordId) { 
        mutableStateOf(activityRecord?.observation ?: "") 
    }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { 
        mutableStateOf(activityRecord?.date ?: LocalDateTime.now()) 
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (activityRecord != null) {
                    stringResource(R.string.activity_dialog_edit_title)
                } else {
                    stringResource(R.string.activity_dialog_title)
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Informações do veículo
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.activity_dialog_vehicle_info),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${vehicle.plate} - ${vehicle.brand} ${vehicle.model}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                // Data e hora
                OutlinedTextField(
                    value = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    onValueChange = { },
                    label = { Text(stringResource(R.string.activity_dialog_date)) },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = stringResource(R.string.cd_date_select))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true
                )
                
                // Condutor
                OutlinedTextField(
                    value = driver,
                    onValueChange = { driver = it },
                    label = { Text(stringResource(R.string.activity_dialog_driver)) },
                    placeholder = { Text(stringResource(R.string.activity_dialog_driver_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Quilômetro de saída
                OutlinedTextField(
                    value = startMileage,
                    onValueChange = { startMileage = it },
                    label = { Text(stringResource(R.string.activity_dialog_start_mileage)) },
                    placeholder = { Text(stringResource(R.string.activity_dialog_start_mileage_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Quilômetro de chegada
                OutlinedTextField(
                    value = endMileage,
                    onValueChange = { endMileage = it },
                    label = { Text(stringResource(R.string.activity_dialog_end_mileage)) },
                    placeholder = { Text(stringResource(R.string.activity_dialog_end_mileage_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Observação
                OutlinedTextField(
                    value = observation,
                    onValueChange = { observation = it },
                    label = { Text(stringResource(R.string.activity_dialog_observation)) },
                    placeholder = { Text(stringResource(R.string.activity_dialog_observation_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val startMileageLong = startMileage.toLongOrNull() ?: 0L
                    val endMileageLong = endMileage.toLongOrNull() ?: startMileageLong
                    
                    val activityRecord = ActivityRecord(
                        id = activityRecord?.id ?: 0L, // Usar ID existente para edição
                        vehicleId = vehicle.id,
                        plate = vehicle.plate,
                        driver = driver,
                        date = selectedDate,
                        startMileage = startMileageLong,
                        endMileage = endMileageLong,
                        observation = observation.takeIf { it.isNotBlank() },
                        organizationId = activityRecord?.organizationId,
                        createdAt = activityRecord?.createdAt ?: LocalDateTime.now(),
                        updatedAt = LocalDateTime.now()
                    )
                    
                    onSave(activityRecord)
                },
                enabled = driver.isNotBlank() && startMileage.isNotBlank()
            ) {
                Text(
                    if (activityRecord != null) {
                        stringResource(R.string.activity_dialog_update)
                    } else {
                        stringResource(R.string.activity_dialog_save)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.activity_dialog_cancel))
            }
        }
    )
    
    // Dialog de seleção de data
    if (showDatePicker) {
        DatePickerDialog(
            selectedDate = selectedDate,
            onDateSelected = { newDate ->
                selectedDate = newDate
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

/**
 * Dialog para seleção de data e hora
 */
@Composable
private fun DatePickerDialog(
    selectedDate: LocalDateTime,
    onDateSelected: (LocalDateTime) -> Unit,
    onDismiss: () -> Unit
) {
    var date by remember { mutableStateOf(selectedDate.toLocalDate()) }
    var hour by remember { mutableStateOf(selectedDate.hour) }
    var minute by remember { mutableStateOf(selectedDate.minute) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.activity_dialog_date_select)) },
        text = {
            Column {
                // Seletor de data (simplificado)
                Text(
                    text = stringResource(R.string.activity_dialog_date_label),
                    style = MaterialTheme.typography.labelMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Seletor de hora
                Text(
                    text = stringResource(R.string.activity_dialog_time_label),
                    style = MaterialTheme.typography.labelMedium
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = hour.toString().padStart(2, '0'),
                        onValueChange = { 
                            val newHour = it.toIntOrNull()?.coerceIn(0, 23) ?: hour
                            hour = newHour
                        },
                        label = { Text("Hora") },
                        modifier = Modifier.weight(1f)
                    )
                    
                    Text(":", modifier = Modifier.padding(horizontal = 8.dp))
                    
                    OutlinedTextField(
                        value = minute.toString().padStart(2, '0'),
                        onValueChange = { 
                            val newMinute = it.toIntOrNull()?.coerceIn(0, 59) ?: minute
                            minute = newMinute
                        },
                        label = { Text("Min") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newDateTime = date.atTime(hour, minute, 0)
                    onDateSelected(newDateTime)
                }
            ) {
                Text(stringResource(R.string.activity_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.activity_dialog_cancel))
            }
        }
    )
}
