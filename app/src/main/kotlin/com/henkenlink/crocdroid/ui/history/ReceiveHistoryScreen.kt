package com.henkenlink.crocdroid.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.henkenlink.crocdroid.domain.model.ReceiveHistoryEntry
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveHistoryScreen(
    viewModel: ReceiveHistoryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val historyState by viewModel.receiveHistoryState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Receive History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (historyState.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearHistory() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All")
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        if (historyState.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "No receive history",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(historyState, key = { it.id }) { entry ->
                    ReceiveHistoryCard(
                        entry = entry,
                        onOpen = { viewModel.openHistoryFile(it) },
                        onShare = { viewModel.shareHistoryFile(it) },
                        onDelete = { viewModel.deleteHistoryEntry(entry.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ReceiveHistoryCard(
    entry: ReceiveHistoryEntry,
    onOpen: (String) -> Unit,
    onShare: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filesExist = entry.filePaths.any { File(it).exists() }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dateFormat.format(Date(entry.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatFileSize(entry.fileSize),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${entry.fileCount} file${if (entry.fileCount > 1) "s" else ""}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            if (!filesExist) {
                Text(
                    text = "File missing or deleted",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (filesExist) {
                    IconButton(onClick = { entry.filePaths.firstOrNull()?.let(onOpen) }) {
                        Icon(Icons.Default.OpenInNew, contentDescription = "Open")
                    }
                    IconButton(onClick = { entry.filePaths.firstOrNull()?.let(onShare) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}
