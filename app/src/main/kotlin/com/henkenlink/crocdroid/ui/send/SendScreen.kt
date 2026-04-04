package com.henkenlink.crocdroid.ui.send

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.henkenlink.crocdroid.data.util.FileUtil
import com.henkenlink.crocdroid.domain.model.TransferState

@OptIn(ExperimentalAnimationApi::class)
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

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = transferState,
                transitionSpec = {
                    fadeIn() + slideInVertically(initialOffsetY = { it / 4 }) togetherWith
                            fadeOut() + slideOutVertically(targetOffsetY = { -it / 4 })
                },
                label = "TransferStateAnimation"
            ) { state ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = if (state is TransferState.Idle || state is TransferState.FileOffer) 
                        Arrangement.Top else Arrangement.Center
                ) {
                    when (state) {
                        is TransferState.Idle, is TransferState.FileOffer -> {
                            Spacer(modifier = Modifier.height(32.dp))
                            HeroSection(
                                icon = Icons.Outlined.InsertDriveFile,
                                title = "Send Files",
                                subtitle = "Select files or folders to transfer securely"
                            )
                            Spacer(modifier = Modifier.height(32.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                ActionCard(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Outlined.InsertDriveFile,
                                    title = "Add Files",
                                    onClick = { fileLauncher.launch(arrayOf("*/*")) }
                                )
                                ActionCard(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Outlined.Folder,
                                    title = "Add Folder",
                                    onClick = { folderLauncher.launch(null) }
                                )
                            }

                            var showCustomCode by remember { mutableStateOf(customCode.isNotBlank()) }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (showCustomCode) {
                                OutlinedTextField(
                                    value = customCode,
                                    onValueChange = { viewModel.updateCustomCode(it) },
                                    label = { Text("Custom Code (Optional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                TextButton(onClick = { showCustomCode = true }) {
                                    Text("Advanced: Custom Code")
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            if (uris.isNotEmpty()) {
                                Text(
                                    text = "Selected Items (${uris.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    itemsIndexed(uris) { index, uri ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.InsertDriveFile,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                val uriString = uri.path ?: ""
                                                val displayName = uriString.substringAfterLast("/")
                                                Text(
                                                    text = displayName,
                                                    modifier = Modifier.weight(1f),
                                                    overflow = TextOverflow.Ellipsis,
                                                    maxLines = 1,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                IconButton(
                                                    onClick = { viewModel.removeFile(index) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Remove",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    TextButton(onClick = { viewModel.clearFiles() }) {
                                        Text("Clear All", color = MaterialTheme.colorScheme.error)
                                    }
                                    Button(
                                        onClick = { viewModel.sendSelectedFiles() },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Send Now")
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                        is TransferState.WaitingForRecipient -> {
                            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                            HeroSection(
                                icon = Icons.Default.Sync,
                                title = "Waiting for Recipient",
                                subtitle = "Share this code with the recipient to start the transfer"
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = state.code,
                                        style = MaterialTheme.typography.displaySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    IconButton(
                                        onClick = { clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(state.code)) },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.onPrimary)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(48.dp))
                            OutlinedButton(onClick = { viewModel.cancelTransfer() }) {
                                Text("Cancel Transfer")
                            }
                        }
                        is TransferState.Transferring -> {
                            HeroSection(
                                icon = Icons.Default.CloudUpload,
                                title = "Sending Files",
                                subtitle = if (state.totalFiles > 1) {
                                    "File ${state.currentFileIndex + 1} of ${state.totalFiles}"
                                } else {
                                    "Transfer in progress"
                                }
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            val progress = if (state.totalBytes > 0) state.sentBytes.toFloat() / state.totalBytes else 0f
                            
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.size(160.dp),
                                    strokeWidth = 12.dp,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.headlineLarge
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Text(
                                text = state.currentFileName,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${FileUtil.formatSize(state.sentBytes)} / ${FileUtil.formatSize(state.totalBytes)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            
                            Spacer(modifier = Modifier.height(48.dp))
                            OutlinedButton(
                                onClick = { viewModel.cancelTransfer() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Cancel")
                            }
                        }
                        is TransferState.Loading -> {
                            CircularProgressIndicator(modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Initializing...", style = MaterialTheme.typography.titleMedium)
                        }
                        is TransferState.Success -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(96.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Transfer Complete!", style = MaterialTheme.typography.headlineMedium)
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(onClick = { viewModel.resetState() }, modifier = Modifier.fillMaxWidth()) {
                                Text("Send More Files")
                            }
                        }
                        is TransferState.Error -> {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                modifier = Modifier.size(96.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Transfer Failed", style = MaterialTheme.typography.headlineMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(onClick = { viewModel.resetState() }, modifier = Modifier.fillMaxWidth()) {
                                Text("Try Again")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeroSection(icon: ImageVector, title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = title, style = MaterialTheme.typography.headlineSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ActionCard(modifier: Modifier = Modifier, icon: ImageVector, title: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = title, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
    }
}
