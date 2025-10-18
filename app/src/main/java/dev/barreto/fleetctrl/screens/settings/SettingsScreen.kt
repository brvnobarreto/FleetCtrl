package dev.barreto.fleetctrl.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.barreto.fleetctrl.R
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import dev.barreto.fleetctrl.ui.theme.FleetCtrlTheme

@Composable
fun SettingsScreen(
    onNavigateToTheme: () -> Unit = {},
    onNavigateToConnectivity: () -> Unit = {},
    onNavigateToDataManagement: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dataVm: dev.barreto.fleetctrl.viewmodels.DataManagementViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val orgVm: dev.barreto.fleetctrl.viewmodels.OrganizationViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val syncVm: dev.barreto.fleetctrl.viewmodels.SyncViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val uploadEnabled by dataVm.uploadLocalToCloud.collectAsState(initial = true)
    val autoSync by dataVm.autoSyncEnabled.collectAsState(initial = false)
    var expandedAppSettings by remember { mutableStateOf(false) }
    var expandedConnectivitySettings by remember { mutableStateOf(false) }
    var expandedDataSettings by remember { mutableStateOf(false) }
    
    val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
    val contentBottom = navBottomPadding + if (screenHeight < 640) 56.dp else 96.dp

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = contentBottom)
    ) {
        item {
            ExpandableSettingSection(
                title = stringResource(R.string.settings_app_title),
                icon = Icons.Default.Settings,
                isExpanded = expandedAppSettings,
                onToggle = { expandedAppSettings = !expandedAppSettings },
                settings = getAppSettings(
                    onNavigateToTheme = onNavigateToTheme,
                    onNavigateToNotifications = onNavigateToNotifications,
                    onNavigateToDataManagement = onNavigateToDataManagement,
                    uploadEnabled = uploadEnabled,
                    onToggleUpload = { enabled -> dataVm.setUploadLocalToCloud(enabled) },
                    onToggleAutoSync = { enabled ->
                        dataVm.setAutoSyncEnabled(enabled)
                        orgVm.getCurrentOrganizationId()?.let { orgId ->
                            if (enabled) syncVm.startRealtime(orgId) else syncVm.stopRealtime()
                        }
                    }
                )
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        item {
            ExpandableSettingSection(
                title = stringResource(R.string.settings_connectivity_title),
                icon = Icons.Default.Wifi,
                isExpanded = expandedConnectivitySettings,
                onToggle = { expandedConnectivitySettings = !expandedConnectivitySettings },
                settings = getConnectivitySettings(onNavigateToConnectivity)
            )
        }
        
        // Data management moved under "Armazenamento" in App settings
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ExpandableSettingSection(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    settings: List<Setting>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Header clicável
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Recolher" else "Expandir",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Conteúdo expansível
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(300))
            ) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    
                    settings.forEach { setting ->
                        SettingItem(
                            icon = setting.icon,
                            title = setting.title,
                            subtitle = setting.subtitle,
                            onClick = setting.onClick,
                            trailing = {
                                when (setting.type) {
                                    SettingType.SWITCH -> {
                                        Switch(
                                            checked = setting.value as Boolean,
                                            onCheckedChange = setting.onToggle
                                        )
                                    }
                                    SettingType.ARROW -> {
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    SettingType.TEXT -> {
                                        Text(
                                            text = setting.value as String,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing()
    }
}

private data class Setting(
    val icon: ImageVector,
    val title: String,
    val subtitle: String? = null,
    val type: SettingType,
    val value: Any,
    val onClick: (() -> Unit)? = null,
    val onToggle: ((Boolean) -> Unit)? = null
)

private enum class SettingType {
    SWITCH, ARROW, TEXT
}

private fun getAppSettings(
    onNavigateToTheme: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToDataManagement: () -> Unit,
    uploadEnabled: Boolean,
    onToggleUpload: (Boolean) -> Unit,
    onToggleAutoSync: (Boolean) -> Unit
): List<Setting> {
    var autoSyncEnabled by mutableStateOf(false)
    return listOf(
        Setting(
            icon = Icons.Default.Palette,
            title = "Tema",
            subtitle = "Claro, Escuro ou Automático",
            type = SettingType.ARROW,
            value = "Automático",
            onClick = onNavigateToTheme
        ),
        Setting(
            icon = Icons.Default.Language,
            title = "Idioma",
            subtitle = "Português (Brasil)",
            type = SettingType.ARROW,
            value = "Português"
        ),
        Setting(
            icon = Icons.Default.Notifications,
            title = "Notificações",
            subtitle = "Gerenciar notificações do app",
            type = SettingType.ARROW,
            value = "Gerenciar",
            onClick = onNavigateToNotifications
        ),
        Setting(
            icon = Icons.Default.CloudSync,
            title = "Backup Automático",
            subtitle = "Sincronizar dados na nuvem",
            type = SettingType.SWITCH,
            value = uploadEnabled,
            onToggle = onToggleUpload
        ),
        Setting(
            icon = Icons.Default.Sync,
            title = "Sincronização",
            subtitle = "Sincronizar automaticamente",
            type = SettingType.SWITCH,
            value = autoSyncEnabled,
            onToggle = { enabled ->
                autoSyncEnabled = enabled
                onToggleAutoSync(enabled)
            }
        ),
        Setting(
            icon = Icons.Default.Storage,
            title = "Armazenamento",
            subtitle = "Gerenciar dados locais",
            type = SettingType.ARROW,
            value = "2.3 MB",
            onClick = onNavigateToDataManagement
        )
    )
}

@Composable
private fun getConnectivitySettings(onNavigateToConnectivity: () -> Unit): List<Setting> {
    val dataVm: dev.barreto.fleetctrl.viewmodels.DataManagementViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val wifiOnly by dataVm.syncWifiOnly.collectAsState(initial = true)
    val mobileData by dataVm.allowMobileData.collectAsState(initial = false)
    val offlineMode by dataVm.offlineMode.collectAsState(initial = false)
    val quality by dataVm.uploadQuality.collectAsState(initial = "Alta")
    return listOf(
        Setting(
            icon = Icons.Default.Group,
            title = stringResource(R.string.settings_organizations),
            subtitle = stringResource(R.string.settings_organizations_subtitle),
            type = SettingType.ARROW,
            value = "",
            onClick = onNavigateToConnectivity
        ),
        Setting(
            icon = Icons.Default.Wifi,
            title = "WiFi Preferencial",
            subtitle = "Usar apenas WiFi para sincronização",
            type = SettingType.SWITCH,
            value = wifiOnly,
            onToggle = { enabled -> dataVm.setSyncWifiOnly(enabled) }
        ),
        Setting(
            icon = Icons.Default.SignalCellular4Bar,
            title = "Dados Móveis",
            subtitle = "Permitir uso de dados móveis",
            type = SettingType.SWITCH,
            value = mobileData,
            onToggle = { enabled -> dataVm.setAllowMobileData(enabled) }
        ),
        Setting(
            icon = Icons.Default.CloudOff,
            title = "Modo Offline",
            subtitle = "Trabalhar sem conexão",
            type = SettingType.SWITCH,
            value = offlineMode,
            onToggle = { enabled -> dataVm.setOfflineMode(enabled) }
        ),
        Setting(
            icon = Icons.Default.Speed,
            title = "Qualidade de Upload",
            subtitle = "Qualidade das fotos enviadas",
            type = SettingType.ARROW,
            value = quality,
            onClick = {
                // Seleciona entre Alta/Média/Baixa
                // Simplificado: alterna ciclicamente
                val next = when (quality) { "Alta" -> "Média"; "Média" -> "Baixa"; else -> "Alta" }
                dataVm.setUploadQuality(next)
            }
        ),
        Setting(
            icon = Icons.Default.SyncProblem,
            title = "Status da Conexão",
            subtitle = buildString {
                append("WiFi-only: "); append(if (wifiOnly) "Sim" else "Não"); append("  •  ")
                append("Dados móveis: "); append(if (mobileData) "Sim" else "Não"); append("  •  ")
                append("Offline: "); append(if (offlineMode) "Sim" else "Não")
            },
            type = SettingType.ARROW,
            value = "Conectado",
            onClick = { /* Poderia abrir um dialog detalhado com ping e status de listeners */ }
        )
    )
}

@Composable
private fun getDataManagementSettings(onNavigateToDataManagement: () -> Unit): List<Setting> = listOf(
    Setting(
        icon = Icons.Default.DeleteForever,
        title = stringResource(R.string.settings_clear_data),
        subtitle = stringResource(R.string.settings_clear_data_subtitle),
        type = SettingType.ARROW,
        value = "",
        onClick = onNavigateToDataManagement
    )
)

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    FleetCtrlTheme {
        SettingsScreen()
    }
}
