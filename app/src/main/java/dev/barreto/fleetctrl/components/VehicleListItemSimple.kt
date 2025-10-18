package dev.barreto.fleetctrl.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import dev.barreto.fleetctrl.ui.theme.FleetCtrlTheme

/**
 * Item da lista de veículos simplificado (sem botões de edição/remoção)
 * Usado nas telas de Diário de Bordo e Abastecimentos
 */
@Composable
fun VehicleListItemSimple(
    vehicle: Vehicle,
    onVehicleClick: (Vehicle) -> Unit,
    modifier: Modifier = Modifier,
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
                VehicleThumbnailSimple(
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

                // Ícone de origem (local/nuvem)
                if (isLocal) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = "Local",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = "Nuvem",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VehicleThumbnailSimple(
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
            if (photoPath != null) {
                // Mostrar foto do veículo
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

@Preview(showBackground = true)
@Composable
private fun VehicleListItemSimplePreview() {
    FleetCtrlTheme {
        VehicleListItemSimple(
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
            onVehicleClick = {}
        )
    }
}
