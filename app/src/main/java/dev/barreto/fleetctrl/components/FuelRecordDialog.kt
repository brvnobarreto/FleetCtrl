package dev.barreto.fleetctrl.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.barreto.fleetctrl.R
import dev.barreto.fleetctrl.data.database.entities.FuelRecord
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Dialog para adicionar ou editar registros de abastecimento
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelRecordDialog(
    vehicle: Vehicle,
    fuelRecord: FuelRecord? = null,
    fuelTypes: List<String>,
    onDismiss: () -> Unit,
    onSave: (FuelRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEditing = fuelRecord != null
    
    // Estado do formulário
    var selectedDate by remember { mutableStateOf(fuelRecord?.date ?: LocalDateTime.now()) }
    var supplier by remember { mutableStateOf(fuelRecord?.gasStation ?: "") }
    var mileage by remember { mutableStateOf(fuelRecord?.mileage?.toString() ?: vehicle.currentMileage.toString()) }
    var fuelType by remember { mutableStateOf(fuelRecord?.fuelType ?: fuelTypes.firstOrNull() ?: "") }
    var quantity by remember { mutableStateOf(fuelRecord?.quantity?.toString() ?: "") }
    var pricePerLiter by remember { mutableStateOf(fuelRecord?.pricePerLiter?.toString() ?: "") }
    var location by remember { mutableStateOf(fuelRecord?.location ?: "") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var notes by remember { mutableStateOf(fuelRecord?.notes ?: "") }
    
    // Função para converter vírgula em ponto para cálculos
    fun normalizeDecimalInput(input: String): String {
        return input.replace(',', '.')
    }
    
    // Cálculo automático do custo total
    val totalCost = remember(quantity, pricePerLiter) {
        try {
            val normalizedQuantity = normalizeDecimalInput(quantity)
            val normalizedPrice = normalizeDecimalInput(pricePerLiter)
            
            val qty = normalizedQuantity.toDoubleOrNull() ?: 0.0
            val price = normalizedPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO
            if (qty > 0 && price > BigDecimal.ZERO) {
                price.multiply(BigDecimal.valueOf(qty))
            } else {
                BigDecimal.ZERO
            }
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }
    
    // Estado para controlar se a localização foi capturada
    var isLocationCaptured by remember { mutableStateOf(location.isNotBlank()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) stringResource(R.string.fuel_dialog_edit_title) else stringResource(R.string.fuel_dialog_title),
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
                // Data e Hora
                OutlinedTextField(
                    value = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    onValueChange = { },
                    label = { Text(stringResource(R.string.fuel_dialog_date)) },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { /* TODO: Implementar seletor de data */ }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Selecionar data")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Fornecedor
                OutlinedTextField(
                    value = supplier,
                    onValueChange = { supplier = it },
                    label = { Text(stringResource(R.string.fuel_dialog_supplier)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                
                // Quilometragem Atual
                OutlinedTextField(
                    value = mileage,
                    onValueChange = { mileage = it },
                    label = { Text(stringResource(R.string.fuel_dialog_mileage)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Tipo de Combustível
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = fuelType,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(stringResource(R.string.fuel_dialog_fuel_type)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        fuelTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    fuelType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                // Quantidade (Litros)
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text(stringResource(R.string.fuel_dialog_quantity)) },
                    placeholder = { Text(stringResource(R.string.fuel_dialog_quantity_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Preço por Litro
                OutlinedTextField(
                    value = pricePerLiter,
                    onValueChange = { pricePerLiter = it },
                    label = { Text(stringResource(R.string.fuel_dialog_price_per_liter)) },
                    placeholder = { Text(stringResource(R.string.fuel_dialog_price_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Custo Total (calculado automaticamente)
                OutlinedTextField(
                    value = if (totalCost > BigDecimal.ZERO) totalCost.toString() else "",
                    onValueChange = { },
                    label = { Text(stringResource(R.string.fuel_dialog_total_cost)) },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (totalCost > BigDecimal.ZERO) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Calculado automaticamente",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
                
                // Localização com GPS
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.fuel_dialog_location),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Button(
                        onClick = {
                            // TODO: Implementar captura de GPS
                            isLocationCaptured = true
                            location = "GPS: -23.5505, -46.6333" // Coordenadas de exemplo (São Paulo)
                            latitude = -23.5505
                            longitude = -46.6333
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = if (isLocationCaptured) {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            ButtonDefaults.buttonColors()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = stringResource(R.string.fuel_dialog_location_button),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isLocationCaptured) 
                                stringResource(R.string.fuel_dialog_location_captured) 
                            else 
                                stringResource(R.string.fuel_dialog_location_button)
                        )
                    }
                }
                
                // Observações
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.fuel_dialog_notes)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newFuelRecord = FuelRecord(
                        id = fuelRecord?.id ?: 0,
                        vehicleId = vehicle.id,
                        date = selectedDate,
                        fuelType = fuelType,
                        quantity = normalizeDecimalInput(quantity).toDoubleOrNull() ?: 0.0,
                        pricePerLiter = normalizeDecimalInput(pricePerLiter).toBigDecimalOrNull() ?: BigDecimal.ZERO,
                        totalCost = totalCost,
                        mileage = mileage.toLongOrNull() ?: 0L,
                        gasStation = supplier.ifBlank { null },
                        location = if (isLocationCaptured) location else null,
                        receiptNumber = null, // Campo removido
                        notes = notes.ifBlank { null },
                        organizationId = fuelRecord?.organizationId,
                        createdAt = fuelRecord?.createdAt ?: LocalDateTime.now(),
                        updatedAt = LocalDateTime.now()
                    )
                    onSave(newFuelRecord)
                },
                enabled = quantity.isNotBlank() && pricePerLiter.isNotBlank() && fuelType.isNotBlank()
            ) {
                Text(if (isEditing) stringResource(R.string.fuel_dialog_update) else stringResource(R.string.fuel_dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.fuel_dialog_cancel))
            }
        }
    )
}
