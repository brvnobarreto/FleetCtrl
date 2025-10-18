package dev.barreto.fleetctrl.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.barreto.fleetctrl.R
import dev.barreto.fleetctrl.data.models.UserOrganization
import dev.barreto.fleetctrl.viewmodels.OrganizationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectivityScreen(
    onBack: () -> Unit,
    organizationViewModel: OrganizationViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val userOrganizations by organizationViewModel.userOrganizations.collectAsState()
    val currentOrganization by organizationViewModel.currentOrganization.collectAsState()
    val isLoading by organizationViewModel.isLoading.collectAsState()
    val message by organizationViewModel.message.collectAsState()
    val error by organizationViewModel.error.collectAsState()
    val isRefreshing by organizationViewModel.isRefreshing.collectAsState()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var selectedOrganization by remember { mutableStateOf<dev.barreto.fleetctrl.data.models.Organization?>(null) }
    
    LaunchedEffect(Unit) {
        organizationViewModel.loadUserOrganizations()
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
            }
            Text(
                text = "Organizações",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { organizationViewModel.loadUserOrganizations() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Mensagens
        message?.let { msg ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = msg,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
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
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Botões de ação
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showCreateDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Criar")
            }
            
            Button(
                onClick = { showJoinDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Login, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Entrar")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Lista de organizações
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (userOrganizations.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Business,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Nenhuma organização",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Crie uma organização ou entre em uma existente",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(userOrganizations) { userOrg ->
                    // Buscar dados completos da organização
                    var organization by remember(userOrg.organizationId) { 
                        mutableStateOf<dev.barreto.fleetctrl.data.models.Organization?>(null) 
                    }
                    
                    LaunchedEffect(userOrg.organizationId) {
                        organization = organizationViewModel.getOrganizationById(userOrg.organizationId)
                    }
                    
                    OrganizationCard(
                        userOrganization = userOrg,
                        organization = organization,
                        isSelected = userOrg.organizationId == currentOrganization?.id,
                        canEdit = organizationViewModel.canEditOrganization(userOrg.organizationId),
                        organizationViewModel = organizationViewModel,
                        onSelect = {
                            organization?.let { orgModel ->
                                // Converter model para entity compatível com currentOrganization
                                val now = java.time.LocalDateTime.now()
                                val entity = dev.barreto.fleetctrl.data.database.entities.Organization(
                                    id = orgModel.id,
                                    code = orgModel.code,
                                    name = orgModel.name,
                                    description = orgModel.description,
                                    ownerId = orgModel.ownerId,
                                    ownerEmail = "",
                                    isActive = true,
                                    maxMembers = null,
                                    requiresApproval = true,
                                    createdAt = now,
                                    updatedAt = now,
                                    isSynced = true,
                                    lastSyncAt = null
                                )
                                organizationViewModel.selectOrganization(entity)
                            }
                        },
                        onLeave = { 
                            selectedOrganization = organization
                            showLeaveDialog = true 
                        },
                        onEdit = { 
                            selectedOrganization = organization
                            showEditDialog = true 
                        }
                    )
                }
            }
        }
        
        // Dialog para criar organização
        if (showCreateDialog) {
            CreateOrganizationDialog(
                onDismiss = { showCreateDialog = false },
                onCreateOrganization = { name, description ->
                    organizationViewModel.createOrganization(name, description)
                    showCreateDialog = false
                }
            )
        }
        
        // Dialog para entrar em organização
        if (showJoinDialog) {
            JoinOrganizationDialog(
                onDismiss = { showJoinDialog = false },
                onJoinOrganization = { code ->
                    organizationViewModel.joinOrganization(code)
                    showJoinDialog = false
                }
            )
        }
        
        // Dialog para editar organização
        if (showEditDialog && selectedOrganization != null) {
            EditOrganizationDialog(
                organization = selectedOrganization!!,
                onDismiss = { 
                    showEditDialog = false
                    selectedOrganization = null
                },
                onUpdateOrganization = { name, description ->
                    organizationViewModel.updateOrganization(selectedOrganization!!.id, name, description)
                    showEditDialog = false
                    selectedOrganization = null
                }
            )
        }
        
        // Dialog para sair da organização
        if (showLeaveDialog && selectedOrganization != null) {
            LeaveOrganizationDialog(
                organization = selectedOrganization!!,
                onDismiss = { 
                    showLeaveDialog = false
                    selectedOrganization = null
                },
                onLeaveOrganization = {
                    organizationViewModel.leaveOrganization(selectedOrganization!!.id)
                    showLeaveDialog = false
                    selectedOrganization = null
                }
            )
        }
    }
}

@Composable
private fun OrganizationCard(
    userOrganization: UserOrganization,
    organization: dev.barreto.fleetctrl.data.models.Organization?,
    isSelected: Boolean,
    canEdit: Boolean,
    organizationViewModel: OrganizationViewModel,
    onSelect: () -> Unit,
    onLeave: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Nome da organização
            Text(
                text = organization?.name ?: "Carregando...",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            // Descrição (se tiver)
            if (!organization?.description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = organization?.description ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Código da organização
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Código: ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = organization?.code ?: "N/A",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { 
                        // Mostrar mensagem de código copiado
                        organizationViewModel.showMessage("Código: ${organization?.code ?: "N/A"}")
                    },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copiar código",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Informações do usuário
            Text(
                text = "Função: ${userOrganization.role}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (userOrganization.userEmail.isNotEmpty()) {
                Text(
                    text = "Email: ${userOrganization.userEmail}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Botões de ação (apenas ícones)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (!isSelected) {
                    IconButton(
                        onClick = onSelect,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Check, 
                            contentDescription = "Selecionar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Botão editar - apenas para owner/editor
                if (canEdit) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit, 
                            contentDescription = "Editar",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                
                // Botão copiar código - sempre visível
                IconButton(
                    onClick = { 
                        // Mostrar mensagem de código copiado
                        organizationViewModel.showMessage("Código: ${organization?.code ?: "N/A"}")
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy, 
                        contentDescription = "Copiar código",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Botão sair - sempre visível
                IconButton(
                    onClick = onLeave,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.ExitToApp, 
                        contentDescription = "Sair",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateOrganizationDialog(
    onDismiss: () -> Unit,
    onCreateOrganization: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Criar Organização") },
        text = {
            Column {
                Text("Digite as informações da organização:")
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreateOrganization(name.trim(), description.trim())
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Criar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun JoinOrganizationDialog(
    onDismiss: () -> Unit,
    onJoinOrganization: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Entrar em Organização") },
        text = {
            Column {
                Text("Digite o código da organização:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Código") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (code.isNotBlank()) {
                        onJoinOrganization(code.trim())
                    }
                },
                enabled = code.isNotBlank()
            ) {
                Text("Entrar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun EditOrganizationDialog(
    organization: dev.barreto.fleetctrl.data.models.Organization,
    onDismiss: () -> Unit,
    onUpdateOrganization: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(organization.name) }
    var description by remember { mutableStateOf(organization.description) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Organização") },
        text = {
            Column {
                Text("Edite as informações da organização:")
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onUpdateOrganization(name.trim(), description.trim())
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun LeaveOrganizationDialog(
    organization: dev.barreto.fleetctrl.data.models.Organization,
    onDismiss: () -> Unit,
    onLeaveOrganization: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sair da Organização") },
        text = {
            Text("Tem certeza que deseja sair da organização \"${organization.name}\"?")
        },
        confirmButton = {
            TextButton(
                onClick = onLeaveOrganization,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Sair")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}