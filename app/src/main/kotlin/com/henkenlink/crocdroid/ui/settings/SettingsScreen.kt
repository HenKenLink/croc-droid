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
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    
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

        // --- History Support ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("History", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onNavigateToHistory) {
                Text("View Full History")
            }
        }
        Text(
            "Transfer history is stored locally and can be managed in the history screen.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
