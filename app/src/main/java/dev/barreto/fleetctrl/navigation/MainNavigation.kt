package dev.barreto.fleetctrl.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.barreto.fleetctrl.R
import dev.barreto.fleetctrl.components.AppBottomNavigation
import dev.barreto.fleetctrl.components.AppHeader
import dev.barreto.fleetctrl.components.FloatingFab
import dev.barreto.fleetctrl.screens.fleet.FleetScreen
import dev.barreto.fleetctrl.screens.diary.DiaryScreen
import dev.barreto.fleetctrl.screens.diary.VehicleActivityScreen
import dev.barreto.fleetctrl.screens.fuel.FuelScreen
import dev.barreto.fleetctrl.screens.fuel.VehicleFuelScreen
import dev.barreto.fleetctrl.screens.maintenance.MaintenanceScreen
import dev.barreto.fleetctrl.screens.maintenance.VehicleMaintenanceScreen
import dev.barreto.fleetctrl.screens.profile.ProfileScreen
import dev.barreto.fleetctrl.screens.settings.SettingsScreen
import dev.barreto.fleetctrl.screens.settings.ConnectivityScreen
import dev.barreto.fleetctrl.screens.settings.DataManagementScreen
import dev.barreto.fleetctrl.screens.settings.ThemeScreen
import dev.barreto.fleetctrl.screens.settings.NotificationsScreen
import dev.barreto.fleetctrl.ui.theme.FleetCtrlTheme
import dev.barreto.fleetctrl.viewmodels.MainViewModel
import dev.barreto.fleetctrl.viewmodels.ThemeViewModel
import dev.barreto.fleetctrl.viewmodels.FleetViewModel
import dev.barreto.fleetctrl.viewmodels.DiaryViewModel
import dev.barreto.fleetctrl.viewmodels.MaintenanceViewModel
import dev.barreto.fleetctrl.viewmodels.FuelViewModel
import dev.barreto.fleetctrl.viewmodels.OrganizationViewModel
import dev.barreto.fleetctrl.viewmodels.DataManagementViewModel
import dev.barreto.fleetctrl.components.VehicleDialog
import dev.barreto.fleetctrl.components.ActivityRecordDialog
import dev.barreto.fleetctrl.components.FuelRecordDialog
import dev.barreto.fleetctrl.components.MaintenanceRecordDialog
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import dev.barreto.fleetctrl.data.database.entities.FuelRecord
import kotlinx.coroutines.launch
import dev.barreto.fleetctrl.utils.DataRefreshNotifier
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.ExperimentalMaterialApi
import dev.barreto.fleetctrl.screens.home.HomeScreen
import dev.barreto.fleetctrl.viewmodels.HomeViewModel
import dev.barreto.fleetctrl.viewmodels.HomeUiState

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MainNavigation(
    viewModel: MainViewModel,
    themeViewModel: ThemeViewModel,
    fleetViewModel: FleetViewModel,
    diaryViewModel: DiaryViewModel,
    fuelViewModel: FuelViewModel,
    maintenanceViewModel: MaintenanceViewModel,
    homeViewModel: HomeViewModel,
    authViewModel: dev.barreto.fleetctrl.viewmodels.AuthViewModel,
    organizationViewModel: OrganizationViewModel,
    dataManagementViewModel: DataManagementViewModel,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentScreen = viewModel.currentScreen
    val isProfileMenuExpanded = viewModel.isProfileMenuExpanded
    val currentOrganization by organizationViewModel.currentOrganization.collectAsState()
    val currentRole by organizationViewModel.currentRole.collectAsState()
    val homeUiState by homeViewModel.uiState.collectAsState()
    // Mensagens observadas para feedback no header
    val fleetSuccess by fleetViewModel.successMessage.collectAsState()
    val fleetError by fleetViewModel.errorMessage.collectAsState()
    val diarySuccess by diaryViewModel.successMessage.collectAsState()
    val diaryError by diaryViewModel.errorMessage.collectAsState()
    val fuelMsg by fuelViewModel.fuelRecordsMessage.collectAsState()
    val maintenanceMsg by maintenanceViewModel.maintenanceRecordsMessage.collectAsState()
    
    // Restaurar organização selecionada
    val appPrefs = dev.barreto.fleetctrl.data.preferences.AppPreferences(androidx.compose.ui.platform.LocalContext.current)
    val prefsSelectedOrgId by appPrefs.selectedOrganizationId.collectAsState(initial = null)
    val autoSync by appPrefs.autoSyncEnabled.collectAsState(initial = false)
    LaunchedEffect(prefsSelectedOrgId) {
        val orgId = prefsSelectedOrgId
        if (!orgId.isNullOrBlank()) {
            organizationViewModel.selectOrganizationId(orgId)
            fleetViewModel.setCurrentOrganization(orgId)
        }
    }

    // Inicia/para realtime conforme preferência
    val syncVm: dev.barreto.fleetctrl.viewmodels.SyncViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    LaunchedEffect(autoSync, currentOrganization?.id) {
        val orgId = currentOrganization?.id
        if (autoSync && !orgId.isNullOrBlank()) syncVm.startRealtime(orgId) else syncVm.stopRealtime()
    }
    
    // Estados para FAB e navegação
    var showAddDialog by remember { mutableStateOf(false) }
    var vehicleToEdit by remember { mutableStateOf<Vehicle?>(null) }
    var selectedVehicleForActivity by remember { mutableStateOf<Vehicle?>(null) }
    var showActivityRecordDialog by remember { mutableStateOf(false) }
    var selectedVehicleForFuel by remember { mutableStateOf<Vehicle?>(null) }
    var showFuelRecordDialog by remember { mutableStateOf(false) }
    var fuelRecordToEdit by remember { mutableStateOf<FuelRecord?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var fuelRecordToDelete by remember { mutableStateOf<FuelRecord?>(null) }
    // Estados para Maintenance
    var selectedVehicleForMaintenance by remember { mutableStateOf<Vehicle?>(null) }
    var showMaintenanceRecordDialog by remember { mutableStateOf(false) }
    var maintenanceRecordToEdit by remember { mutableStateOf<dev.barreto.fleetctrl.data.database.entities.MaintenanceRecord?>(null) }
    // Permissões de escrita baseadas no papel atual
    val canWrite = when (currentRole?.lowercase()) {
        "owner", "editor" -> true
        else -> false
    }

    // Ao receber notificação global de refresh (limpeza/clear all), limpa seleções
    LaunchedEffect(Unit) {
        dev.barreto.fleetctrl.utils.DataRefreshNotifier.refreshTrigger.collect {
            selectedVehicleForActivity = null
            selectedVehicleForFuel = null
            selectedVehicleForMaintenance = null
            homeViewModel.refreshSummary(organizationViewModel.getCurrentOrganizationId())
        }
    }

    // Snackbar host para mensagens globais
    val snackbarHostState = remember { SnackbarHostState() }

    Box(modifier = Modifier.fillMaxSize()) {
        // ===== CONTEÚDO PRINCIPAL =====
        Column(modifier = Modifier.fillMaxSize()) {
            // ===== HEADER FIXO =====
            val roleLabel = when (currentRole?.lowercase()) {
                "owner" -> "Owner"
                "editor" -> "Editor"
                "viewer" -> "Viewer"
                else -> null
            }

            // Estado do subtítulo do header (base + status transitório)
            var baseSubtitle by remember(currentScreen, currentOrganization, currentRole) { mutableStateOf<String?>(null) }
            var transientSubtitle by remember { mutableStateOf<String?>(null) }

            // Labels estáticos obtidos via Compose em contexto composable
            val localDataLabel = stringResource(id = R.string.header_local_data)
            val savedLocalLabel = stringResource(id = R.string.header_saved_local)
            val context = LocalContext.current

            // Constrói label de org/role ou dados locais (sem remember para evitar uso composable em lambda)
            val orgOrLocalLabel = currentOrganization?.let { org ->
                listOfNotNull(org.name, roleLabel).joinToString(" • ")
            } ?: localDataLabel

            // Dica/descrição da tela (após alguns segundos)
            val screenHint = when (currentScreen) {
                Screen.HOME -> stringResource(id = R.string.header_hint_home)
                Screen.DIARY -> stringResource(id = R.string.header_hint_diary)
                Screen.FUEL -> stringResource(id = R.string.header_hint_fuel)
                Screen.MAINTENANCE -> stringResource(id = R.string.header_hint_maintenance)
                Screen.FLEET -> stringResource(id = R.string.header_hint_fleet)
                else -> null
            }

            // Ao entrar na tela ou trocar org/role, mantém sempre visível a organização/role; adiciona a dica da tela ao lado
            LaunchedEffect(currentScreen, orgOrLocalLabel) {
                baseSubtitle = if (!screenHint.isNullOrBlank()) {
                    orgOrLocalLabel + " • " + screenHint
                } else orgOrLocalLabel
            }

            // Observa mensagens de sucesso para atualizar subtítulo temporariamente
            LaunchedEffect(fleetSuccess) {
                val msg = fleetSuccess
                if (!msg.isNullOrBlank()) {
                    // Feedback visual imediato
                    snackbarHostState.showSnackbar(msg)
                    // E também um destaque no header por alguns segundos
                    transientSubtitle = currentOrganization?.let { org ->
                        context.getString(R.string.header_saved_org, org.name)
                    } ?: savedLocalLabel
                    kotlinx.coroutines.delay(2500)
                    transientSubtitle = null
                }
            }
            LaunchedEffect(diarySuccess) {
                val msg = diarySuccess
                if (!msg.isNullOrBlank()) {
                    transientSubtitle = currentOrganization?.let { org ->
                        context.getString(R.string.header_saved_org, org.name)
                    } ?: savedLocalLabel
                    kotlinx.coroutines.delay(2500)
                    transientSubtitle = null
                }
            }
            // Erros devem aparecer como Snackbar, não no Header
            LaunchedEffect(fuelMsg) {
                val msg = fuelMsg
                if (!msg.isNullOrBlank() && !msg.startsWith("Erro")) {
                    transientSubtitle = currentOrganization?.let { org ->
                        context.getString(R.string.header_saved_org, org.name)
                    } ?: savedLocalLabel
                    kotlinx.coroutines.delay(2500)
                    transientSubtitle = null
                }
            }
            LaunchedEffect(maintenanceMsg) {
                val msg = maintenanceMsg
                if (!msg.isNullOrBlank() && !msg.startsWith("Erro")) {
                    transientSubtitle = currentOrganization?.let { org ->
                        context.getString(R.string.header_saved_org, org.name)
                    } ?: savedLocalLabel
                    kotlinx.coroutines.delay(2500)
                    transientSubtitle = null
                }
            }

            val uploadEnabled by dataManagementViewModel.uploadLocalToCloud.collectAsState(initial = true)
            val alertPrefix = if (!uploadEnabled) stringResource(id = R.string.header_upload_disabled) else null

            val base = transientSubtitle ?: baseSubtitle
            val subtitle = when {
                !alertPrefix.isNullOrBlank() && !base.isNullOrBlank() -> "$alertPrefix • $base"
                !alertPrefix.isNullOrBlank() -> alertPrefix
                else -> base
            }

            AppHeader(
                title = stringResource(currentScreen.titleRes),
                viewModel = viewModel,
                onLogout = onLogout,
                onSyncToCloud = { fleetViewModel.syncLocalDataToCloud() },
                subtitle = subtitle
            )
            
            // ===== ÁREA DE WINDOW INSETS COM COR DO HEADER =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    //.windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                    .background(MaterialTheme.colorScheme.surface)
            )

            // ===== ÁREA DE CONTEÚDO =====
            val surfaceColor = MaterialTheme.colorScheme.surface
            // Exibir erros em Snackbar em vez de header
            LaunchedEffect(fleetError) {
                val msg = fleetError
                if (!msg.isNullOrBlank()) {
                    snackbarHostState.showSnackbar(msg)
                }
            }
            LaunchedEffect(diaryError) {
                val msg = diaryError
                if (!msg.isNullOrBlank()) {
                    snackbarHostState.showSnackbar(msg)
                }
            }
            // Erros de Fuel/Maintenance também via Snackbar
            LaunchedEffect(fuelMsg) {
                val msg = fuelMsg
                if (!msg.isNullOrBlank() && (msg.startsWith("Erro") || msg.contains("Erro") || msg.startsWith("❌"))) {
                    snackbarHostState.showSnackbar(msg)
                }
            }
            LaunchedEffect(maintenanceMsg) {
                val msg = maintenanceMsg
                if (!msg.isNullOrBlank() && (msg.startsWith("Erro") || msg.contains("Erro") || msg.startsWith("❌"))) {
                    snackbarHostState.showSnackbar(msg)
                }
            }
            LaunchedEffect(homeUiState.errorMessage) {
                val msg = homeUiState.errorMessage
                if (!msg.isNullOrBlank()) {
                    snackbarHostState.showSnackbar(msg)
                    homeViewModel.clearError()
                }
            }
            LaunchedEffect(currentScreen, currentOrganization?.id) {
                if (currentScreen == Screen.HOME) {
                    homeViewModel.refreshSummary(currentOrganization?.id)
                }
                // Inicia listener em tempo real de veículos quando há org selecionada
                currentOrganization?.id?.let { orgId ->
                    // já observa Room
                    fleetViewModel.observeVehicles()
                    // inicia listener via repository injetado pelo ViewModel (sem App singleton)
                    kotlin.runCatching {
                        // acessando via repos do viewmodel
                        val repoField = dev.barreto.fleetctrl.viewmodels.FleetViewModel::class.java.getDeclaredField("fleetRepository")
                        repoField.isAccessible = true
                        val fleetRepo = repoField.get(fleetViewModel) as dev.barreto.fleetctrl.data.repositories.FleetRepository
                        val syncField = dev.barreto.fleetctrl.data.repositories.FleetRepository::class.java.getDeclaredField("syncRepository")
                        syncField.isAccessible = true
                        val syncRepo = syncField.get(fleetRepo) as dev.barreto.fleetctrl.data.repositories.SyncRepository
                        syncRepo.startVehiclesRealtime(orgId)
                    }
                }
            }
            var refreshing by remember { mutableStateOf(false) }
            val pullRefreshState = rememberPullRefreshState(refreshing, {
                refreshing = true
                // Dispara sync específico da tela
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        when (currentScreen) {
                            Screen.HOME -> homeViewModel.refreshSummary(currentOrganization?.id)
                            Screen.FLEET -> organizationViewModel.syncVehiclesOnly()
                            Screen.DIARY -> organizationViewModel.syncActivityOnly()
                            Screen.FUEL -> organizationViewModel.syncFuelOnly()
                            Screen.MAINTENANCE -> organizationViewModel.syncMaintenanceOnly()
                            else -> {}
                        }
                    } catch (e: Exception) {
                        // Evita crash em erros de permissão/rede
                        android.util.Log.w("PULL_REFRESH", "Falha no sync: ${e.message}")
                    } finally {
                        refreshing = false
                    }
                }
            })

            Box(
                modifier = Modifier
                    .weight(1f)
                    .pullRefresh(pullRefreshState)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .drawWithContent {
                        drawContent()
                        // Gradiente sutil na área do gesto de navegação
                        val gradientHeight = 64.dp.toPx()
                        val brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                surfaceColor.copy(alpha = 0.8f)
                            ),
                            startY = size.height - gradientHeight,
                            endY = size.height
                        )
                        drawRect(brush = brush)
                    }
            ) {
                when (currentScreen) {
                    Screen.HOME -> {
                        HomeScreen(
                            state = homeUiState,
                            currentOrganization = currentOrganization,
                            onRefresh = { homeViewModel.refreshSummary(currentOrganization?.id) },
                            onNavigateToFleet = { viewModel.navigateToScreen(Screen.FLEET) },
                            onNavigateToDiary = { viewModel.navigateToScreen(Screen.DIARY) },
                            onNavigateToFuel = { viewModel.navigateToScreen(Screen.FUEL) },
                            onNavigateToMaintenance = { viewModel.navigateToScreen(Screen.MAINTENANCE) }
                        )
                    }
                    Screen.DIARY -> {
                        if (selectedVehicleForActivity != null) {
                            VehicleActivityScreen(
                                vehicle = selectedVehicleForActivity!!,
                                onBackClick = { selectedVehicleForActivity = null },
                                onAddRecord = { vehicle ->
                                    if (canWrite) {
                                        selectedVehicleForActivity = vehicle
                                        showActivityRecordDialog = true
                                    }
                                },
                                canEdit = canWrite
                            )
                        } else {
                            DiaryScreen(
                                diaryViewModel = diaryViewModel,
                                onVehicleClick = { vehicle -> selectedVehicleForActivity = vehicle },
                                currentOrgId = currentOrganization?.id
                            )
                        }
                    }
                    Screen.FUEL -> {
                        if (selectedVehicleForFuel != null) {
                            VehicleFuelScreen(
                                vehicle = selectedVehicleForFuel!!,
                                onBackClick = { selectedVehicleForFuel = null },
                                onEditFuelRecord = { fuelRecord ->
                                    if (canWrite) {
                                        fuelRecordToEdit = fuelRecord
                                        showFuelRecordDialog = true
                                    }
                                },
                                onDeleteFuelRecord = { fuelRecord ->
                                    if (canWrite) {
                                        fuelRecordToDelete = fuelRecord
                                        showDeleteConfirmation = true
                                    }
                                },
                                viewModel = fuelViewModel,
                                canEdit = canWrite
                            )
                        } else {
                            FuelScreen(
                                onVehicleClick = { vehicle -> selectedVehicleForFuel = vehicle },
                                viewModel = fuelViewModel,
                                currentOrgId = currentOrganization?.id
                            )
                        }
                    }
                    Screen.MAINTENANCE -> {
                        if (selectedVehicleForMaintenance != null) {
                            VehicleMaintenanceScreen(
                                vehicle = selectedVehicleForMaintenance!!,
                                onBackClick = { selectedVehicleForMaintenance = null },
                                maintenanceViewModel = maintenanceViewModel,
                                canEdit = canWrite
                            )
                        } else {
                            MaintenanceScreen(
                                maintenanceViewModel = maintenanceViewModel,
                                onVehicleClick = { vehicle -> selectedVehicleForMaintenance = vehicle },
                                currentOrgId = currentOrganization?.id
                            )
                        }
                    }
                    Screen.FLEET -> FleetScreen(
                        fleetViewModel = fleetViewModel,
                        onEditVehicle = { vehicle -> vehicleToEdit = vehicle },
                        canEdit = (currentOrganization == null) || when (currentRole?.lowercase()) {
                            "owner", "editor" -> true
                            else -> false
                        },
                        currentOrgId = currentOrganization?.id
                    )
                    Screen.PROFILE -> ProfileScreen(
                        onLogout = onLogout,
                        authViewModel = authViewModel
                    )
                Screen.SETTINGS -> SettingsScreen(
                    onNavigateToTheme = { viewModel.navigateToScreen(Screen.THEME) },
                    onNavigateToConnectivity = { viewModel.navigateToScreen(Screen.CONNECTIVITY) },
                    onNavigateToDataManagement = { viewModel.navigateToScreen(Screen.DATA_MANAGEMENT) },
                    onNavigateToNotifications = { viewModel.navigateToScreen(Screen.NOTIFICATIONS) }
                )
                    Screen.THEME -> ThemeScreen(
                        themeViewModel = themeViewModel
                    )
                Screen.CONNECTIVITY -> ConnectivityScreen(
                    onBack = { viewModel.navigateToScreen(Screen.SETTINGS) },
                    organizationViewModel = organizationViewModel
                )
                Screen.DATA_MANAGEMENT -> DataManagementScreen(
                    onBack = { viewModel.navigateToScreen(Screen.SETTINGS) },
                    dataManagementViewModel = dataManagementViewModel,
                    isInOrganization = currentOrganization != null
                )
                Screen.NOTIFICATIONS -> NotificationsScreen()
                }
                PullRefreshIndicator(refreshing, pullRefreshState, Modifier.align(Alignment.TopCenter))
            }
        }

        // ===== FOOTER FLUTUANTE =====
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            AppBottomNavigation(
                currentScreen = currentScreen,
                onNavigate = { screen -> viewModel.navigateToScreen(screen) }
            )
        }
        
        // FAB flutuante (lógica original + regra: se estiver em org como viewer, some do UI)
        val isInOrg = currentOrganization != null
        val showFabBase = (currentScreen == Screen.FLEET) ||
            (currentScreen == Screen.DIARY && selectedVehicleForActivity != null) ||
            (currentScreen == Screen.FUEL && selectedVehicleForFuel != null) ||
            (currentScreen == Screen.MAINTENANCE && selectedVehicleForMaintenance != null)
        val showFab = showFabBase && (!isInOrg || canWrite)

        if (showFab) {
            // Espaçamento inferior responsivo: considera barras de navegação e altura de tela
            val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
            val baseFabOffset = if (screenHeight < 640) 56.dp else 96.dp
            val fabBottomPadding = navBottomPadding + baseFabOffset

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = fabBottomPadding)
            ) {
                FloatingFab(
                    onClick = {
                        val allowWrite = (!isInOrg || canWrite)
                        when {
                            currentScreen == Screen.FLEET -> {
                                if (allowWrite) showAddDialog = true
                            }
                            currentScreen == Screen.DIARY && selectedVehicleForActivity != null -> {
                                if (allowWrite) showActivityRecordDialog = true
                            }
                            currentScreen == Screen.FUEL && selectedVehicleForFuel != null -> {
                                if (allowWrite) showFuelRecordDialog = true
                            }
                            currentScreen == Screen.MAINTENANCE && selectedVehicleForMaintenance != null -> {
                                if (allowWrite) showMaintenanceRecordDialog = true
                            }
                        }
                    }
                )
            }
        }
        
        // Dialog de adicionar/editar veículo
        if (showAddDialog || vehicleToEdit != null) {
            VehicleDialog(
                vehicle = vehicleToEdit,
                onDismiss = {
                    showAddDialog = false
                    vehicleToEdit = null
                },
                onSave = { vehicle ->
                    val orgId = currentOrganization?.id
                    val vehicleWithOrg = if (!orgId.isNullOrBlank()) vehicle.copy(organizationId = orgId) else vehicle
                    val success = if (vehicleToEdit == null) {
                        fleetViewModel.addVehicle(vehicleWithOrg)
                    } else {
                        fleetViewModel.updateVehicle(vehicleWithOrg)
                    }
                    success
                }
            )
        }
        
        // Dialog de registro de atividade
        if (showActivityRecordDialog && selectedVehicleForActivity != null) {
            val lastMileage = diaryViewModel.lastMileage.value
            ActivityRecordDialog(
                vehicle = selectedVehicleForActivity!!,
                lastMileage = lastMileage,
                onDismiss = { showActivityRecordDialog = false },
                onSave = { activityRecord ->
                    val orgId = currentOrganization?.id
                    val recordWithOrg = if (!orgId.isNullOrBlank()) activityRecord.copy(organizationId = orgId) else activityRecord
                    diaryViewModel.addActivityRecord(recordWithOrg)
                    showActivityRecordDialog = false
                }
            )
        }
        
        // Dialog de registro de abastecimento
        if (showFuelRecordDialog && selectedVehicleForFuel != null) {
            FuelRecordDialog(
                vehicle = selectedVehicleForFuel!!,
                fuelRecord = fuelRecordToEdit,
                fuelTypes = fuelViewModel.getFuelTypes(),
                onDismiss = { 
                    showFuelRecordDialog = false
                    fuelRecordToEdit = null
                },
                onSave = { fuelRecord ->
                    // Usar coroutine scope para chamar suspend functions
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        val orgId = currentOrganization?.id
                        val recordWithOrg = if (!orgId.isNullOrBlank()) fuelRecord.copy(organizationId = orgId) else fuelRecord
                        if (fuelRecordToEdit == null) {
                            fuelViewModel.addFuelRecord(recordWithOrg)
                        } else {
                            fuelViewModel.updateFuelRecord(recordWithOrg)
                        }
                        showFuelRecordDialog = false
                        fuelRecordToEdit = null
                    }
                }
            )
        }
        
        // Dialog de confirmação de exclusão de abastecimento
        if (showDeleteConfirmation && fuelRecordToDelete != null) {
            AlertDialog(
                onDismissRequest = { 
                    showDeleteConfirmation = false
                    fuelRecordToDelete = null
                },
                title = {
                    Text("Confirmar Exclusão")
                },
                text = {
                    Text("Tem certeza que deseja excluir este registro de abastecimento? Esta ação não pode ser desfeita.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            fuelRecordToDelete?.let { record ->
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                    fuelViewModel.deleteFuelRecord(record)
                                }
                            }
                            showDeleteConfirmation = false
                            fuelRecordToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Excluir")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmation = false
                            fuelRecordToDelete = null
                        }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
        
        // Dialog de registro de manutenção
        if (showMaintenanceRecordDialog && selectedVehicleForMaintenance != null) {
            MaintenanceRecordDialog(
                vehicle = selectedVehicleForMaintenance!!,
                maintenanceRecord = maintenanceRecordToEdit,
                onDismiss = { 
                    showMaintenanceRecordDialog = false
                    maintenanceRecordToEdit = null
                },
                onSave = { maintenanceRecord ->
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        val orgId = currentOrganization?.id
                        val recordWithOrg = if (!orgId.isNullOrBlank()) maintenanceRecord.copy(organizationId = orgId) else maintenanceRecord
                        if (maintenanceRecordToEdit == null) {
                            maintenanceViewModel.addMaintenanceRecord(recordWithOrg)
                        } else {
                            maintenanceViewModel.updateMaintenanceRecord(recordWithOrg)
                        }
                        showMaintenanceRecordDialog = false
                        maintenanceRecordToEdit = null
                    }
                }
            )
        }

        // SnackbarHost ancorado ao fundo
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp)
        )
    }
}

@Preview(showBackground = true)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainNavigationPreview() {
    FleetCtrlTheme {
        // Preview simplificado sem ViewModel
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header simulado
                TopAppBar(
                    title = { Text("Diário de Bordo") }
                )
                
                // Conteúdo simulado
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Conteúdo da tela",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                
                // Footer simulado
                AppBottomNavigation(
                    currentScreen = Screen.DIARY,
                    onNavigate = { }
                )
            }
        }
    }
}
