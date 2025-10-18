package dev.barreto.fleetctrl.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.barreto.fleetctrl.R
import dev.barreto.fleetctrl.data.database.entities.ActivityRecord
import dev.barreto.fleetctrl.ui.theme.FleetCtrlTheme
import java.time.format.DateTimeFormatter

/**
 * Item da lista de registros de atividade
 * Layout otimizado para o diário de bordo
 */
@Composable
fun ActivityRecordItem(
    record: ActivityRecord,
    onEditClick: (ActivityRecord) -> Unit,
    onDeleteClick: (ActivityRecord) -> Unit,
    modifier: Modifier = Modifier,
    isLocal: Boolean = record.organizationId.isNullOrBlank(),
    canEdit: Boolean = true
) {
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
                    // Primeira linha: Quilometragem em negrito
                    Text(
                        text = "${record.startMileage} → ${record.endMileage} km",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    // Segunda linha: Data
                    Text(
                        text = record.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Terceira linha: Condutor
                    Text(
                        text = record.driver,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Quarta linha: Distância
                    Text(
                        text = stringResource(R.string.activity_record_distance, record.distance),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Quinta linha: Observação (se existir)
                    record.observation?.let { observation ->
                        if (observation.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = observation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }
                }
                
                // Botões no canto superior direito
                Row(
                    modifier = Modifier.align(Alignment.TopEnd),
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
                    if (canEdit) {
                        Spacer(modifier = Modifier.width(4.dp))
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
private fun ActivityRecordItemPreview() {
    FleetCtrlTheme {
        ActivityRecordItem(
            record = ActivityRecord(
                id = 1,
                vehicleId = 1,
                plate = "ABC1234",
                driver = "João Silva",
                date = java.time.LocalDateTime.now(),
                startMileage = 10000,
                endMileage = 10050,
                observation = "Viagem para São Paulo"
            ),
            onEditClick = {},
            onDeleteClick = {}
        )
    }
}
