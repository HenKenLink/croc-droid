package com.henkenlink.crocdroid.ui.send

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.henkenlink.crocdroid.domain.model.TransferState

@Composable
fun SendScreen(
    viewModel: SendViewModel,
    modifier: Modifier = Modifier,
) {
    val transferState by viewModel.transferState.collectAsStateWithLifecycle()
    val customCode by viewModel.customCode.collectAsStateWithLifecycle()
    val uris by viewModel.selectedFileUris.collectAsStateWithLifecycle()

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { selectedUris: List<Uri> ->
        if (selectedUris.isNotEmpty()) {
            viewModel.addFiles(selectedUris)
        }
    }

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { folderUri: Uri? ->
        folderUri?.let {
            viewModel.addFiles(listOf(it))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (val state = transferState) {
            is TransferState.Idle, is TransferState.FileOffer -> {
                OutlinedTextField(
                    value = customCode,
                    onValueChange = { viewModel.updateCustomCode(it) },
                    label = { Text("Custom Code (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(onClick = { fileLauncher.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Files")
                    }
                    Button(onClick = { folderLauncher.launch(null) }) {
                        Icon(Icons.Default.DriveFileMove, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Folder")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (uris.isNotEmpty()) {
                    Text(
                        text = "Selected Items (${uris.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(vertical = 8.dp)
                    ) {
                        LazyColumn(modifier = Modifier.padding(8.dp)) {
                            itemsIndexed(uris) { index, uri ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = uri.path ?: "Unknown path",
                                        modifier = Modifier.weight(1f),
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    IconButton(onClick = { viewModel.removeFile(index) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { viewModel.sendSelectedFiles() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Send Now")
                    }
                    
                    TextButton(onClick = { viewModel.clearFiles() }) {
                        Text("Clear All", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("No files selected", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
            is TransferState.WaitingForRecipient -> {
                Text("Waiting for recipient to connect...")
                Text("Code: ${state.code}", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.cancelTransfer() }) {
                    Text("Cancel")
                }
            }
            is TransferState.Transferring -> {
                Text("Transferring...")
                LinearProgressIndicator(
                    progress = { if (state.totalBytes > 0) state.sentBytes.toFloat() / state.totalBytes else 0f },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                )
                Text("${state.sentBytes} / ${state.totalBytes} bytes")
                Button(onClick = { viewModel.cancelTransfer() }) {
                    Text("Cancel")
                }
            }
            is TransferState.Loading -> {
                CircularProgressIndicator()
            }
            is TransferState.Success -> {
                Text("Success!", style = MaterialTheme.typography.headlineMedium)
                Button(onClick = { viewModel.resetState() }) {
                    Text("Send Another")
                }
            }
            is TransferState.Error -> {
                Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                Button(onClick = { viewModel.resetState() }) {
                    Text("Back")
                }
            }
        }
    }
}
