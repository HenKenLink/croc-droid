package com.henkenlink.crocdroid.ui.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToLogs: () -> Unit,
    onNavigateToRelayConfig: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val selectedConfig by viewModel.selectedRelayConfig.collectAsStateWithLifecycle()
    
    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val contentResolver = viewModel.getContext().contentResolver
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(it, takeFlags)
            viewModel.updateSettings(settings.copy(downloadPath = it.toString()))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Settings", 
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Connection / Relay ---
            SettingsCard(title = "Relay & Connection", icon = Icons.Default.Router) {
                // Relay configuration selector
                Text(
                    text = "Active Relay Configuration",
                    style = MaterialTheme.typography.titleSmall
                )
                
                Spacer(Modifier.height(12.dp))
                
                // Dropdown selector for relay configs
                val relayConfigs by viewModel.relayConfigsState.collectAsStateWithLifecycle()
                var expanded by remember { mutableStateOf(false) }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    onClick = { expanded = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedConfig?.name ?: "None",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = selectedConfig?.relayAddress ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select config",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    relayConfigs.forEach { config ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = config.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = config.relayAddress,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                viewModel.selectRelayConfig(config.id)
                                expanded = false
                            },
                            leadingIcon = {
                                if (config.id == selectedConfig?.id) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Manage Configs") },
                        onClick = {
                            expanded = false
                            onNavigateToRelayConfig()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Settings, contentDescription = null)
                        }
                    )
                }
                
                Spacer(Modifier.height(12.dp))
                
                // Display selected config details
                selectedConfig?.let { config ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ConfigDetailRow("Relay Ports", config.relayPorts)
                            ConfigDetailRow("Password", "•".repeat(config.relayPassword.length))
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                
                // Keep Peer IP field as-is
                OutlinedTextField(
                    value = settings.peerIp,
                    onValueChange = { viewModel.updateSettings(settings.copy(peerIp = it)) },
                    label = { Text("Peer IP (Direct Connect)") },
                    placeholder = { Text("e.g. 192.168.1.10:9009") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Bypass relay and securely connect directly to an IP address.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }

            // --- Automation ---
            SettingsCard(title = "Automation", icon = Icons.Default.AutoFixHigh) {
                SettingRow(
                    label = "Auto-Zip Folders",
                    description = "Automatically compress folders into a standard zip before sending."
                ) {
                    Switch(
                        checked = settings.autoZipFolders,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(autoZipFolders = it)) },
                    )
                }
                SettingRow(
                    label = "Auto-Accept Inbound Files",
                    description = "Skip the confirmation prompt when receiving a file (trusted networks only)."
                ) {
                    Switch(
                        checked = settings.noPromptReceive,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(noPromptReceive = it)) },
                    )
                }
            }

            // --- Network ---
            SettingsCard(title = "Network", icon = Icons.Default.Wifi) {
                SettingRow(
                    label = "Disable Local Discovery",
                    description = "Don't broadcast to or search for local network devices."
                ) {
                    Switch(
                        checked = settings.disableLocal,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(disableLocal = it)) },
                    )
                }
                SettingRow(
                    label = "Force Local Only",
                    description = "Never fallback to the external relay server."
                ) {
                    Switch(
                        checked = settings.forceLocal,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(forceLocal = it)) },
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = settings.multicastAddress,
                    onValueChange = { viewModel.updateSettings(settings.copy(multicastAddress = it)) },
                    label = { Text("Multicast Address") },
                    placeholder = { Text("Leave empty for default") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // --- Transfer Options ---
            SettingsCard(title = "Transfer Features", icon = Icons.Default.Tune) {
                SettingRowOptions(
                    label = "PAKE Curve",
                    currentValue = settings.curve,
                    options = listOf("p256", "p384", "p521", "siec", "ed25519")
                ) { value -> viewModel.updateSettings(settings.copy(curve = value)) }
                
                SettingRowOptions(
                    label = "Hash Algorithm",
                    currentValue = settings.hashAlgorithm,
                    options = listOf("xxhash", "imohash", "md5", "highway")
                ) { value -> viewModel.updateSettings(settings.copy(hashAlgorithm = value)) }
                
                SettingRow(
                    label = "Disable Multiplexing",
                    description = "Turn off parallel connections."
                ) {
                    Switch(
                        checked = settings.disableMultiplexing,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(disableMultiplexing = it)) },
                    )
                }
                SettingRow(
                    label = "Disable Compression",
                    description = "Send data as-is without protocol compression."
                ) {
                    Switch(
                        checked = settings.disableCompression,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(disableCompression = it)) },
                    )
                }
                SettingRow(
                    label = "Overwrite Existing Files",
                    description = "Replace local files automatically if they exist."
                ) {
                    Switch(
                        checked = settings.overwrite,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(overwrite = it)) },
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = settings.uploadThrottle,
                    onValueChange = { viewModel.updateSettings(settings.copy(uploadThrottle = it)) },
                    label = { Text("Upload Throttle") },
                    placeholder = { Text("e.g. 1MB, 500KB") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // --- Fixed Codes ---
            SettingsCard(title = "Fixed Codes", icon = Icons.Default.VpnKey) {
                Text(
                    "When set, these codes will be used by default instead of auto-generated ones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = settings.fixedSendCode,
                    onValueChange = { viewModel.updateSettings(settings.copy(fixedSendCode = it)) },
                    label = { Text("Static Send Code") },
                    placeholder = { Text("Leave empty for auto") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = settings.fixedReceiveCode,
                    onValueChange = { viewModel.updateSettings(settings.copy(fixedReceiveCode = it)) },
                    label = { Text("Static Receive Code") },
                    placeholder = { Text("Leave empty for manual entry") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // --- Download Path ---
            SettingsCard(title = "Storage", icon = Icons.Default.FolderOpen) {
                OutlinedTextField(
                    value = settings.downloadPath.ifBlank { "Default (Internal Downloads)" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Save Destination") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { folderLauncher.launch(null) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Change Folder")
                        }
                    }
                )
            }

            // --- Appearance ---
            SettingsCard(title = "Appearance", icon = Icons.Default.Palette) {
                SettingRowOptions(
                    label = "Theme Mode",
                    currentValue = settings.themeMode.replaceFirstChar { it.uppercase() },
                    options = listOf("System", "Light", "Dark")
                ) { value -> viewModel.updateSettings(settings.copy(themeMode = value.lowercase())) }
            }

            // --- Debug ---
            SettingsCard(title = "Diagnostics", icon = Icons.Default.BugReport) {
                SettingRow(
                    label = "Enable Engine Logs",
                    description = "Record internal croc protocol events for debugging."
                ) {
                    Switch(
                        checked = settings.debugMode,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(debugMode = it)) },
                    )
                }
                if (settings.debugMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onNavigateToLogs,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("View Live Logs")
                    }
                }
            }


            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun ConfigDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun SettingsCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun SettingRow(label: String, description: String? = null, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        content()
    }
}

@Composable
fun SettingRowOptions(label: String, currentValue: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(currentValue)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
