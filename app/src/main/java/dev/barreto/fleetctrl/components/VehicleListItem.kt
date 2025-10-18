package dev.barreto.fleetctrl.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import dev.barreto.fleetctrl.utils.ImageUtils

/**
 * Item da lista de veículos
 */
@Composable
fun VehicleListItem(
    vehicle: Vehicle,
    onVehicleClick: (Vehicle) -> Unit,
    onEditClick: (Vehicle) -> Unit,
    onDeleteClick: (Vehicle) -> Unit,
    modifier: Modifier = Modifier,
    canEdit: Boolean = true,
    isLocal: Boolean = false
) {
    // Usar key para evitar recomposições desnecessárias
    key(vehicle.id) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            onClick = { onVehicleClick(vehicle) }
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Miniatura da foto
            VehicleThumbnail(
                photoPath = vehicle.photoPath,
                modifier = Modifier.size(60.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Informações do veículo
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Placa em destaque
                Text(
                    text = vehicle.plate,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Número do veículo
                Text(
                    text = vehicle.vehicleNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Modelo e ano
                Text(
                    text = "${vehicle.model} ${vehicle.year}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Condutor
                Text(
                    text = vehicle.driver,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Indicador de origem + botões de ação
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Ícone de origem (Local/Nuvem)
                if (isLocal) {
                    Icon(
                        Icons.Default.PhoneAndroid,
                        contentDescription = "Local",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.Cloud,
                        contentDescription = "Nuvem",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (canEdit) {
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { onEditClick(vehicle) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { onDeleteClick(vehicle) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Excluir",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun VehicleThumbnail(
    photoPath: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                        contentDescription = "Foto do veículo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Trata como Base64
                    val bmp = remember(photoPath) { ImageUtils.base64ToBitmap(photoPath) }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Foto do veículo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.DirectionsCar,
                            contentDescription = "Veículo",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                // Placeholder quando não há foto
                Icon(
                    Icons.Default.DirectionsCar,
                    contentDescription = "Veículo",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
