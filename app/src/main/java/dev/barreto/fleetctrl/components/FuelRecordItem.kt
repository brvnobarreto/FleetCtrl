package dev.barreto.fleetctrl.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.PhoneAndroid
import dev.barreto.fleetctrl.R
import dev.barreto.fleetctrl.data.database.entities.FuelRecord
import dev.barreto.fleetctrl.ui.theme.FleetCtrlTheme
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

/**
 * Item da lista de registros de abastecimento
 * Layout otimizado para mostrar informações essenciais
 */

/**
 * Extrai coordenadas de uma string de localização GPS
 * Formato esperado: "GPS: -23.5505, -46.6333" ou "-23.5505, -46.6333"
 */
fun extractCoordinates(location: String): Pair<Double, Double>? {
    return try {
        val regex = """-?\d+\.?\d*,\s*-?\d+\.?\d*""".toRegex()
        val match = regex.find(location)?.value
        if (match != null) {
            val parts = match.split(",")
            if (parts.size == 2) {
                val lat = parts[0].trim().toDouble()
                val lng = parts[1].trim().toDouble()
                Pair(lat, lng)
            } else null
        } else null
    } catch (e: Exception) {
        null
    }
}

/**
 * Abre o Google Maps com as coordenadas fornecidas
 */
fun openGoogleMaps(context: android.content.Context, latitude: Double, longitude: Double) {
    val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    intent.setPackage("com.google.android.apps.maps")
    
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        // Fallback para abrir no navegador se o Google Maps não estiver instalado
        val webUri = Uri.parse("https://www.google.com/maps?q=$latitude,$longitude")
        val webIntent = Intent(Intent.ACTION_VIEW, webUri)
        context.startActivity(webIntent)
    }
}
@Composable
fun FuelRecordItem(
    record: FuelRecord,
    onEditClick: (FuelRecord) -> Unit,
    onDeleteClick: (FuelRecord) -> Unit,
    modifier: Modifier = Modifier,
    isLocal: Boolean = record.organizationId.isNullOrBlank(),
    canEdit: Boolean = true
) {
    val context = LocalContext.current
    
    key(record.id) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .then(if (canEdit) Modifier.clickable { onEditClick(record) } else Modifier),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Primeira linha: Quantidade e custo em negrito
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.fuel_record_quantity, record.quantity),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.fuel_record_cost, record.totalCost),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Segunda linha: Quilometragem
                    Text(
                        text = "${record.mileage} km",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Terceira linha: Data
                    Text(
                        text = record.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Quarta linha: Tipo de combustível e preço por litro
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = record.fuelType,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.fuel_record_price_per_liter, record.pricePerLiter),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Quinta linha: Fornecedor com pin interativo (se tiver localização)
                    record.location?.let { location ->
                        if (location.isNotBlank()) {
                            val coordinates = extractCoordinates(location)
                            if (coordinates != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        openGoogleMaps(context, coordinates.first, coordinates.second)
                                    }
                                ) {
                                    Text(
                                        text = stringResource(R.string.fuel_record_supplier, record.gasStation ?: "Não informado"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Abrir no Google Maps",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.fuel_record_supplier, record.gasStation ?: "Não informado"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.fuel_record_supplier, record.gasStation ?: "Não informado"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } ?: run {
                        Text(
                            text = stringResource(R.string.fuel_record_supplier, record.gasStation ?: "Não informado"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Sexta linha: Observação (se existir)
                    record.notes?.let { notes ->
                        if (notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }
                }
                
                // Botões no canto inferior direito
                Row(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Indicador de origem
                    if (isLocal) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.PhoneAndroid,
                            contentDescription = "Local",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Cloud,
                            contentDescription = "Nuvem",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    if (canEdit) {
                        // Botão de editar
                        IconButton(
                            onClick = { onEditClick(record) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.cd_edit),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        // Botão de deletar
                        IconButton(
                            onClick = { onDeleteClick(record) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.cd_delete),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FuelRecordItemPreview() {
    FleetCtrlTheme {
        FuelRecordItem(
            record = FuelRecord(
                id = 1,
                vehicleId = 1,
                date = java.time.LocalDateTime.now(),
                fuelType = "Gasolina",
                quantity = 45.5,
                pricePerLiter = BigDecimal("5.89"),
                totalCost = BigDecimal("268.16"),
                mileage = 50000,
                gasStation = "Posto Shell",
                location = "GPS: -23.5505, -46.6333",
                receiptNumber = null,
                notes = "Abastecimento completo"
            ),
            onEditClick = {},
            onDeleteClick = {}
        )
    }
}
