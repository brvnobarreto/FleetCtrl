package dev.barreto.fleetctrl.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.barreto.fleetctrl.R
import dev.barreto.fleetctrl.viewmodels.DataManagementViewModel

@Composable
fun DataManagementScreen(
    onBack: () -> Unit,
    dataManagementViewModel: DataManagementViewModel,
    isInOrganization: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isLoading by dataManagementViewModel.isLoading.collectAsState()
    val message by dataManagementViewModel.message.collectAsState()
    val error by dataManagementViewModel.error.collectAsState()
    val uploadEnabledPref by dataManagementViewModel.uploadLocalToCloud.collectAsState(initial = true)
    
    var showClearDataDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        dataManagementViewModel.clearMessages()
    }

    // Se não estiver em organização, force salvar local (desativa toggle)
    LaunchedEffect(isInOrganization, uploadEnabledPref) {
        if (!isInOrganization && uploadEnabledPref) {
            dataManagementViewModel.setUploadLocalToCloud(false)
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back)
                )
            }
            Text(
                text = stringResource(R.string.settings_data_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Warning Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "⚠️ ATENÇÃO: Esta ação remove TODOS os dados do app permanentemente!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // Switch: Enviar dados locais para a nuvem
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_upload_local_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (isInOrganization) {
                            stringResource(R.string.settings_upload_local_subtitle)
                        } else {
                            "Sem organização selecionada: salvando apenas no dispositivo."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uploadEnabledPref,
                    onCheckedChange = { enabled ->
                        if (isInOrganization) {
                            dataManagementViewModel.setUploadLocalToCloud(enabled)
                        }
                    },
                    enabled = !isLoading && isInOrganization
                )
            }
        }

        // Check Data Button
        OutlinedButton(
            onClick = { 
                dataManagementViewModel.showMessage("📊 Verificando dados...\n\nUse o botão de limpeza para remover todos os dados.")
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Icon(
                Icons.Default.Analytics,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Verificar Dados Atuais")
        }
        
        // Clear Data Button
        Button(
            onClick = { showClearDataDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onError
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(stringResource(R.string.settings_clear_data))
        }
        
        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "O que será removido:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                val items = listOf(
                    "🚗 Todos os veículos cadastrados",
                    "⛽ Todos os registros de combustível",
                    "📝 Todas as atividades do diário",
                    "🔧 Todos os registros de manutenção",
                    "🏢 Todas as organizações",
                    "📸 Todas as fotos dos veículos"
                )
                
                items.forEach { item ->
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Messages
        message?.let { msg ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    if (msg.contains("removidos com sucesso")) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "💡 Para ver as mudanças:",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "• Navegue para a tela de Frota\n• Volte para esta tela\n• Os dados devem estar vazios",
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            
                            Button(
                                onClick = { 
                                    onBack()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Voltar para Configurações")
                            }
                        }
                    }
                }
            }
        }
        
        error?.let { err ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = err,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
    
    // Clear Data Confirmation Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.settings_clear_data_confirm_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(R.string.settings_clear_data_confirm_message))
            },
            confirmButton = {
                Button(
                    onClick = {
                        dataManagementViewModel.clearAllData(context)
                        showClearDataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.settings_clear_data_confirm_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text(stringResource(R.string.settings_clear_data_confirm_no))
                }
            }
        )
    }
}
