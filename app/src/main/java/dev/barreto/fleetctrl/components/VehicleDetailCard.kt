package dev.barreto.fleetctrl.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import dev.barreto.fleetctrl.R
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import dev.barreto.fleetctrl.utils.ImageUtils

/**
 * Card de detalhes do veículo
 */
@Composable
fun VehicleDetailCard(
    vehicle: Vehicle,
    onClose: () -> Unit,
    onEdit: (Vehicle) -> Unit,
    onDelete: (Vehicle) -> Unit,
    modifier: Modifier = Modifier,
    canEdit: Boolean = true
) {
    // Card de detalhes centralizado nos dois eixos
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable { /* Previne fechar ao clicar no card */ },
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box {
                // Botão de fechar no canto superior direito
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Header com foto e informações principais
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Foto do veículo
                        VehiclePhoto(
                            photoPath = vehicle.photoPath,
                            modifier = Modifier.size(100.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // Informações principais
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
                                text = vehicle.vehicleNumber,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Text(
                                text = "${vehicle.brand} ${vehicle.model} ${vehicle.year}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Detalhes do veículo
                    VehicleDetails(vehicle = vehicle)
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Botões de ação
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (canEdit) {
                            OutlinedButton(
                                onClick = { onEdit(vehicle) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.vehicle_detail_edit))
                            }

                            OutlinedButton(
                                onClick = { onDelete(vehicle) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.vehicle_detail_delete))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VehiclePhoto(
    photoPath: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (!photoPath.isNullOrBlank()) {
                val looksLikeUri = remember(photoPath) {
                    photoPath.startsWith("content://") ||
                            photoPath.startsWith("file://") ||
                            photoPath.startsWith("/") ||
                            photoPath.startsWith("http://") ||
                            photoPath.startsWith("https://") ||
                            photoPath.startsWith("android.resource://")
                }

                if (looksLikeUri) {
                    val painter = rememberAsyncImagePainter(
                        model = Uri.parse(photoPath),
                        placeholder = painterResource(android.R.drawable.ic_menu_gallery),
                        error = painterResource(android.R.drawable.ic_menu_gallery)
                    )
                    Image(
                        painter = painter,
                        contentDescription = stringResource(R.string.vehicle_detail_photo_description),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Trata como Base64
                    val bmp = remember(photoPath) { ImageUtils.base64ToBitmap(photoPath) }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = stringResource(R.string.vehicle_detail_photo_description),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.DirectionsCar,
                            contentDescription = stringResource(R.string.vehicle_detail_car_description),
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                // Placeholder quando não há foto
                Icon(
                    Icons.Default.DirectionsCar,
                    contentDescription = stringResource(R.string.vehicle_detail_car_description),
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun VehicleDetails(vehicle: Vehicle) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Informações básicas
        DetailRow(
            icon = Icons.Default.Person,
            label = stringResource(R.string.vehicle_detail_driver_label),
            value = vehicle.driver
        )
        
        // Status
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (vehicle.showInDiary) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (vehicle.showInDiary) stringResource(R.string.vehicle_detail_visible_diary) else stringResource(R.string.vehicle_detail_hidden_diary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (vehicle.isActive) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (vehicle.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (vehicle.isActive) stringResource(R.string.vehicle_detail_active) else stringResource(R.string.vehicle_detail_inactive),
                style = MaterialTheme.typography.bodyMedium,
                color = if (vehicle.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.vehicle_detail_label_format, label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
