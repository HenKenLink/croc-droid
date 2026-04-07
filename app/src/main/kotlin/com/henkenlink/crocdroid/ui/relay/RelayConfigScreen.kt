package com.henkenlink.crocdroid.ui.relay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.henkenlink.crocdroid.domain.model.RelayConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelayConfigScreen(
    viewModel: RelayConfigViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configs by viewModel.relayConfigsState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingConfig by remember { mutableStateOf<RelayConfig?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Relay Configs") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingConfig = null
                    showDialog = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Config")
            }
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(configs, key = { it.id }) { config ->
                RelayConfigCard(
                    config = config,
                    onEdit = {
                        editingConfig = config
                        showDialog = true
                    },
                    onDelete = { viewModel.deleteConfig(config.id) }
                )
            }
        }
    }
    
    if (showDialog) {
        RelayConfigDialog(
            config = editingConfig,
            onDismiss = { showDialog = false },
            onSave = { name, address, ports, password ->
                if (editingConfig != null) {
                    viewModel.updateConfig(editingConfig!!.id, name, address, ports, password)
                } else {
                    viewModel.addConfig(name, address, ports, password)
                }
                showDialog = false
            }
        )
    }
}

@Composable
fun RelayConfigCard(
    config: RelayConfig,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = config.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit ${config.name}")
                    }
                    if (config.id != "default") {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete ${config.name}")
                        }
                    }
                }
            }
            
            Text(
                text = "Address: ${config.relayAddress}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Ports: ${config.relayPorts}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Password: ${"•".repeat(config.relayPassword.length)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RelayConfigDialog(
    config: RelayConfig?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(config?.name ?: "") }
    var address by remember { mutableStateOf(config?.relayAddress ?: "") }
    var ports by remember { mutableStateOf(config?.relayPorts ?: "") }
    var password by remember { mutableStateOf(config?.relayPassword ?: "") }
    var showPassword by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (config != null) "Edit Config" else "Add Config") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Relay Address") },
                    placeholder = { Text("croc.schollz.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = ports,
                    onValueChange = { ports = it },
                    label = { Text("Relay Ports") },
                    placeholder = { Text("9009,9010,9011") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Relay Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (showPassword) "Hide password" else "Show password"
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && address.isNotBlank() && ports.isNotBlank() && password.isNotBlank()) {
                        onSave(name, address, ports, password)
                    }
                },
                enabled = name.isNotBlank() && address.isNotBlank() && ports.isNotBlank() && password.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
