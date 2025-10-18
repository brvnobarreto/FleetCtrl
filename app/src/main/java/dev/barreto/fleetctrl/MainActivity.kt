package dev.barreto.fleetctrl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.barreto.fleetctrl.navigation.MainNavigation
import dev.barreto.fleetctrl.screens.auth.LoginScreen
import dev.barreto.fleetctrl.ui.theme.FleetCtrlTheme
import dev.barreto.fleetctrl.viewmodels.AuthViewModel
import dev.barreto.fleetctrl.viewmodels.MainViewModel
import dev.barreto.fleetctrl.viewmodels.ThemeViewModel
import dev.barreto.fleetctrl.viewmodels.FleetViewModel
import dev.barreto.fleetctrl.viewmodels.DiaryViewModel
import dev.barreto.fleetctrl.viewmodels.FuelViewModel
import dev.barreto.fleetctrl.viewmodels.MaintenanceViewModel
import dev.barreto.fleetctrl.viewmodels.OrganizationViewModel
import dev.barreto.fleetctrl.viewmodels.DataManagementViewModel
import dev.barreto.fleetctrl.viewmodels.HomeViewModel
import dev.barreto.fleetctrl.utils.DatabaseRecovery
import dev.barreto.fleetctrl.utils.DatabaseCleaner
import com.facebook.stetho.Stetho
import com.google.firebase.app

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializar Stetho para debug (apenas em debug)
        try {
            Stetho.initializeWithDefaults(this)
        } catch (e: Exception) {
            // Stetho não disponível
        }

        val opts = Firebase.app.options
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        Log.d("FB_CFG", "projectId=${opts.projectId}, appId=${opts.applicationId}, gcm=${opts.gcmSenderId}, uid=$uid")
        
        setContent {
            val authViewModel: AuthViewModel = viewModel()
            val themeViewModel: ThemeViewModel = viewModel()
            val mainViewModel: MainViewModel = viewModel()
            val fleetViewModel: FleetViewModel = viewModel()
            val diaryViewModel: DiaryViewModel = viewModel()
            val fuelViewModel: FuelViewModel = viewModel()
            val maintenanceViewModel: MaintenanceViewModel = viewModel()
            val homeViewModel: HomeViewModel = viewModel()
            val organizationViewModel: OrganizationViewModel = viewModel()
            val dataManagementViewModel: DataManagementViewModel = viewModel()
            
            // Verificar e limpar dados de exemplo se necessário
            LaunchedEffect(Unit) {
                DatabaseRecovery.checkAndRecoverData(this@MainActivity)
                DatabaseCleaner.cleanSampleData(this@MainActivity) // Limpar dados de exemplo
                // DatabaseCleaner.clearAllData(this@MainActivity) // Removido - agora é manual

                val app = com.google.firebase.Firebase.app
                val opts = app.options
                android.util.Log.d("FB_CFG", "projectId=${opts.projectId}, appId=${opts.applicationId}, gcm=${opts.gcmSenderId}")
            }
            
            // Observar o estado de autenticação
            val currentUser by authViewModel.currentUser.collectAsState(initial = null)
            
            // Observar as mudanças do ViewModel
            val selectedTheme = themeViewModel.selectedTheme
            val selectedPalette = themeViewModel.selectedPalette
            
            FleetCtrlTheme(
                darkTheme = themeViewModel.isDarkTheme(),
                colorPalette = selectedPalette
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (currentUser != null) {
                        // Usuário logado - mostrar app principal
                        MainNavigation(
                            viewModel = mainViewModel,
                            themeViewModel = themeViewModel,
                            fleetViewModel = fleetViewModel,
                            diaryViewModel = diaryViewModel,
                            fuelViewModel = fuelViewModel,
                            maintenanceViewModel = maintenanceViewModel,
                            homeViewModel = homeViewModel,
                            authViewModel = authViewModel,
                            organizationViewModel = organizationViewModel,
                            dataManagementViewModel = dataManagementViewModel,
                            onLogout = {
                                authViewModel.signOut()
                            }
                        )
                    } else {
                        // Usuário não logado - mostrar tela de login
                        LoginScreen(
                            onLoginSuccess = {
                                // A navegação será automática pelo LaunchedEffect
                            },
                            authViewModel = authViewModel
                        )
                    }
                }
            }
        }
    }
}
