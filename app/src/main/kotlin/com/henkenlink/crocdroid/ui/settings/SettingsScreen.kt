package com.henkenlink.crocdroid.ui.settings

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
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val history by viewModel.historyState.collectAsStateWithLifecycle()

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            // Ideally we'd convert this to a real path, but for simplicity we'll use the path-like part if possible
            // or just store the URI and handle it in ViewModel.
            // Note: Simplification for now, using the path string.
            val path = it.path ?: ""
            // On some devices, the path is "/tree/primary:Documents". We want the real path "/storage/emulated/0/Documents"
            // This is a complex conversion. For now, let's just use the URI string or a placeholder.
            viewModel.updateSettings(settings.copy(downloadPath = it.toString()))
        }
    }

    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        // --- Connection Settings ---
        Text("Connection", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = settings.relayAddress,
            onValueChange = { viewModel.updateSettings(settings.copy(relayAddress = it)) },
            label = { Text("Relay Address") },
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = settings.relayPassword,
            onValueChange = { viewModel.updateSettings(settings.copy(relayPassword = it)) },
            label = { Text("Relay Password") },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Disable Local Discovery")
            Switch(
                checked = settings.disableLocal,
                onCheckedChange = { viewModel.updateSettings(settings.copy(disableLocal = it)) },
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Overwrite Existing Files")
            Switch(
                checked = settings.overwrite,
                onCheckedChange = { viewModel.updateSettings(settings.copy(overwrite = it)) },
            )
        }

        HorizontalDivider()

        // --- Fixed Code Settings ---
        Text("Fixed Codes", style = MaterialTheme.typography.titleMedium)
        Text(
            "When set, these codes will be used by default instead of auto-generated ones.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = settings.fixedSendCode,
            onValueChange = { viewModel.updateSettings(settings.copy(fixedSendCode = it)) },
            label = { Text("Fixed Send Code") },
            placeholder = { Text("Leave empty for auto-generated") },
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = settings.fixedReceiveCode,
            onValueChange = { viewModel.updateSettings(settings.copy(fixedReceiveCode = it)) },
            label = { Text("Fixed Receive Code") },
            placeholder = { Text("Leave empty to enter manually") },
            modifier = Modifier.fillMaxWidth(),
        )

        HorizontalDivider()

        // --- Transfer Settings ---
        Text("Transfer", style = MaterialTheme.typography.titleMedium)
        
        OutlinedTextField(
            value = settings.downloadPath.ifBlank { "Default (Internal)" },
            onValueChange = {},
            readOnly = true,
            label = { Text("Download Path") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { folderLauncher.launch(null) }) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Select Folder")
                }
            }
        )

        HorizontalDivider()

        // --- History ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Transfer History", style = MaterialTheme.typography.titleMedium)
            if (history.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearHistory() }) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (history.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("No history yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            history.forEach { entry ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        overlineContent = {
                            Text("${entry.type} • ${dateFormatter.format(Date(entry.timestamp))}")
                        },
                        headlineContent = {
                            Text(entry.fileName, style = MaterialTheme.typography.bodyLarge)
                        },
                        supportingContent = {
                            val sizeText = if (entry.fileSize > 0) {
                                String.format("%.2f MB", entry.fileSize / (1024.0 * 1024.0))
                            } else {
                                "${entry.fileCount} files"
                            }
                            Text(
                                text = if (entry.success) "Success • $sizeText" else "Failed • ${entry.errorMessage ?: "Unknown error"}",
                                color = if (entry.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = { viewModel.deleteHistoryEntry(entry.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                }
            }
        }
    }
}
