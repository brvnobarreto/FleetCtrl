package dev.barreto.fleetctrl.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseUser
import dev.barreto.fleetctrl.R
import dev.barreto.fleetctrl.viewmodels.AuthViewModel

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by authViewModel.currentUser.collectAsState(initial = null)
    val isLoading by authViewModel.isLoading.collectAsState()
    
    // Estados para edição
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    
    // Inicializar dados do usuário
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            val displayName = user.displayName ?: ""
            val nameParts = displayName.split(" ")
            firstName = if (nameParts.isNotEmpty()) nameParts[0] else ""
            lastName = if (nameParts.size > 1) nameParts.drop(1).joinToString(" ") else ""
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Text(
            text = stringResource(R.string.menu_profile),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        // Card do Perfil
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Avatar e Nome
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                    ) {
                        val photoUrl = currentUser?.photoUrl
                        if (photoUrl != null) {
                            // Foto do Google
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(photoUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = stringResource(R.string.profile_avatar),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Avatar padrão quando não há foto
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = stringResource(R.string.profile_avatar),
                                    modifier = Modifier.size(40.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                    
                    // Nome do usuário
                    Column {
                        Text(
                            text = currentUser?.displayName ?: stringResource(R.string.profile_no_name),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.profile_google_signin),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Divider()
                
                // Informações do usuário
                UserInfoSection(
                    currentUser = currentUser,
                    firstName = firstName,
                    lastName = lastName,
                    isEditing = isEditing,
                    onFirstNameChange = { firstName = it },
                    onLastNameChange = { lastName = it },
                    onEditToggle = { isEditing = !isEditing }
                )
            }
        }
        
        // Botão de Logout
        Button(
            onClick = onLogout,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onError
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(
                Icons.Default.ExitToApp,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.auth_logout))
        }
    }
}

@Composable
private fun UserInfoSection(
    currentUser: FirebaseUser?,
    firstName: String,
    lastName: String,
    isEditing: Boolean,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onEditToggle: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Email (não editável)
        InfoRow(
            icon = Icons.Default.Email,
            label = stringResource(R.string.profile_email),
            value = currentUser?.email ?: stringResource(R.string.profile_no_email),
            isEditable = false
        )
        
        // Nome (editável)
        InfoRow(
            icon = Icons.Default.Person,
            label = stringResource(R.string.profile_first_name),
            value = firstName,
            isEditable = isEditing,
            onValueChange = onFirstNameChange
        )
        
        // Sobrenome (editável)
        InfoRow(
            icon = Icons.Default.Person,
            label = stringResource(R.string.profile_last_name),
            value = lastName,
            isEditable = isEditing,
            onValueChange = onLastNameChange
        )
        
        // Botão de editar/salvar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (isEditing) {
                Button(
                    onClick = onEditToggle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.profile_save))
                }
            } else {
                OutlinedButton(
                    onClick = onEditToggle
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.profile_edit))
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isEditable: Boolean,
    onValueChange: (String) -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (isEditable) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}