package dev.barreto.fleetctrl.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import android.app.DatePickerDialog
import java.time.LocalDate
import java.time.ZoneId
import dev.barreto.fleetctrl.R
import dev.barreto.fleetctrl.data.database.entities.MaintenanceRecord
import dev.barreto.fleetctrl.data.database.entities.MaintenanceType
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Dialog para adicionar/editar registros de manutenção
 */
@Composable
fun MaintenanceRecordDialog(
    vehicle: Vehicle,
    maintenanceRecord: MaintenanceRecord? = null,
    onDismiss: () -> Unit,
    onSave: (MaintenanceRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEditing = maintenanceRecord != null
    val context = LocalContext.current
    
    // Estado do formulário
    var date by remember { mutableStateOf(maintenanceRecord?.date ?: LocalDateTime.now()) }
    var minRevisionDate by remember { mutableStateOf(maintenanceRecord?.minRevisionDate ?: LocalDateTime.now()) }
    var maxRevisionDate by remember { mutableStateOf(maintenanceRecord?.maxRevisionDate ?: LocalDateTime.now()) }
    var revisionMileage by remember { mutableStateOf(maintenanceRecord?.mileage?.toString() ?: "") }
    var nextRevision by remember { mutableStateOf(maintenanceRecord?.nextMaintenanceMileage?.toString() ?: "") }
    var distanceToDealer by remember { mutableStateOf(maintenanceRecord?.distanceToDealer?.toString() ?: "") }
    var notes by remember { mutableStateOf(maintenanceRecord?.notes ?: "") }
    
    // Estado dos seletores de data
    var showMinDatePicker by remember { mutableStateOf(false) }
    var showMaxDatePicker by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) stringResource(R.string.maintenance_dialog_edit_title) else stringResource(R.string.maintenance_dialog_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cd_close)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Campos do formulário
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Data
                    OutlinedTextField(
                        value = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                        onValueChange = { },
                        label = { Text(stringResource(R.string.maintenance_dialog_date)) },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true
                    )
                    
                    // Data mínima da revisão
                    OutlinedTextField(
                        value = minRevisionDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        onValueChange = { },
                        label = { Text(stringResource(R.string.maintenance_dialog_min_revision_date)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMinDatePicker = true },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showMinDatePicker = true }) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = stringResource(R.string.cd_date_select)
                                )
                            }
                        }
                    )
                    
                    // Data máxima da revisão
                    OutlinedTextField(
                        value = maxRevisionDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        onValueChange = { },
                        label = { Text("Data Máxima da Revisão") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMaxDatePicker = true },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showMaxDatePicker = true }) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Selecionar data"
                                )
                            }
                        }
                    )
                    
                    // Quilometragem da revisão
                    OutlinedTextField(
                        value = revisionMileage,
                        onValueChange = { revisionMileage = it },
                        label = { Text("Quilometragem da Revisão") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    
                    // Próxima revisão
                    OutlinedTextField(
                        value = nextRevision,
                        onValueChange = { nextRevision = it },
                        label = { Text("Próxima Revisão (km)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("Ex: 60000") }
                    )
                    
                    // Distância até concessionária
                    OutlinedTextField(
                        value = distanceToDealer,
                        onValueChange = { distanceToDealer = it },
                        label = { Text("Distância até Concessionária (km)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    
                    // Observações
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Observações") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Botões
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                    
                    Button(
                        onClick = {
                            val newRecord = MaintenanceRecord(
                                id = maintenanceRecord?.id ?: 0,
                                vehicleId = vehicle.id,
                                date = date,
                                type = MaintenanceType.PREVENTIVE,
                                description = "Revisão",
                                mileage = revisionMileage.toLongOrNull() ?: 0L,
                                totalCost = BigDecimal.ZERO,
                                nextMaintenanceMileage = nextRevision.toLongOrNull(),
                                minRevisionDate = minRevisionDate,
                                maxRevisionDate = maxRevisionDate,
                                distanceToDealer = distanceToDealer.toLongOrNull(),
                                notes = notes.takeIf { it.isNotBlank() },
                                organizationId = maintenanceRecord?.organizationId,
                                createdAt = maintenanceRecord?.createdAt ?: LocalDateTime.now(),
                                updatedAt = LocalDateTime.now()
                            )
                            onSave(newRecord)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isEditing) "Atualizar" else "Salvar")
                    }
                }
            }
        }
    }
    
    // Seletor de data mínima
    if (showMinDatePicker) {
        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                minRevisionDate = LocalDate.of(year, month + 1, dayOfMonth)
                    .atStartOfDay()
                showMinDatePicker = false
            },
            minRevisionDate.year,
            minRevisionDate.monthValue - 1,
            minRevisionDate.dayOfMonth
        )
        datePicker.show()
    }
    
    // Seletor de data máxima
    if (showMaxDatePicker) {
        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                maxRevisionDate = LocalDate.of(year, month + 1, dayOfMonth)
                    .atStartOfDay()
                showMaxDatePicker = false
            },
            maxRevisionDate.year,
            maxRevisionDate.monthValue - 1,
            maxRevisionDate.dayOfMonth
        )
        datePicker.show()
    }
}
