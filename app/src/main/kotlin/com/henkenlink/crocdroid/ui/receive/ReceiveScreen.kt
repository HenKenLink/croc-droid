package com.henkenlink.crocdroid.ui.receive

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.henkenlink.crocdroid.data.util.FileUtil
import com.henkenlink.crocdroid.domain.model.TransferState
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReceiveScreen(
    viewModel: ReceiveViewModel,
    onNavigateToHistory: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val transferState by viewModel.transferState.collectAsStateWithLifecycle()
    val code by viewModel.receiveCode.collectAsStateWithLifecycle()

    val scanLauncher = rememberLauncherForActivityResult(
        contract = ScanContract(),
    ) { result ->
        result.contents?.let { scannedCode ->
            val sanitized = scannedCode.trim().replace(" ", "-")
            viewModel.updateReceiveCode(sanitized)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (transferState is TransferState.Idle) {
                TopAppBar(
                    title = { },
                    actions = {
                        IconButton(onClick = onNavigateToHistory) {
                            Icon(Icons.Default.History, contentDescription = "View History")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = transferState,
                contentKey = { it::class },
                transitionSpec = {
                    (fadeIn(animationSpec = tween(150)) +
                        slideInVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)) { it / 6 }
                    ) togetherWith (
                        fadeOut(animationSpec = tween(100)) +
                        slideOutVertically(animationSpec = tween(100)) { -it / 6 }
                    )
                },
                label = "ReceiveTransferStateAnimation"
            ) { state ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = if (state is TransferState.Idle) Arrangement.Top else Arrangement.Center
                ) {
                    when (state) {
                        is TransferState.Idle -> {
                            Spacer(modifier = Modifier.height(32.dp))
                            ReceiveHeroSection(
                                icon = Icons.Outlined.CloudDownload,
                                title = "Receive Files",
                                subtitle = "Enter code or scan QR to connect"
                            )
                            Spacer(modifier = Modifier.height(32.dp))

                            // Input Area
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                                    OutlinedTextField(
                                        value = code,
                                        onValueChange = {
                                            val sanitized = it.replace(" ", "-")
                                            viewModel.updateReceiveCode(sanitized)
                                        },
                                        label = { Text("Transfer Code") },
                                        singleLine = true,
                                        textStyle = LocalTextStyle.current.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 18.sp,
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        trailingIcon = {
                                            Row(modifier = Modifier.padding(end = 4.dp)) {
                                                if (code.isNotEmpty()) {
                                                    IconButton(onClick = { viewModel.updateReceiveCode("") }) {
                                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                                    }
                                                }
                                                IconButton(onClick = {
                                                    clipboardManager.getText()?.text?.let {
                                                        viewModel.updateReceiveCode(it.trim().replace(" ", "-"))
                                                    }
                                                }) {
                                                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                                                }
                                            }
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        FilledTonalButton(
                                            onClick = {
                                                val options = ScanOptions().apply {
                                                    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                                    setPrompt("Scan a croc transfer QR code")
                                                    setBeepEnabled(false)
                                                    setBarcodeImageEnabled(true)
                                                    setOrientationLocked(true)
                                                    setCaptureActivity(PortraitCaptureActivity::class.java)
                                                }
                                                scanLauncher.launch(options)
                                            },
                                            modifier = Modifier.weight(1f).height(50.dp)
                                        ) {
                                            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Scan QR")
                                        }

                                        Button(
                                            onClick = { viewModel.receiveFile(code) },
                                            enabled = code.isNotBlank(),
                                            modifier = Modifier.weight(1f).height(50.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Receive")
                                        }
                                    }
                                }
                            }
                        }
                        is TransferState.FileOffer -> {
                            ReceiveHeroSection(
                                icon = Icons.Default.HelpOutline,
                                title = "Incoming Transfer",
                                subtitle = "Someone wants to send you files"
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.InsertDriveFile,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = state.fileName,
                                        style = MaterialTheme.typography.titleMedium,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${FileUtil.formatSize(state.fileSize)} • ${state.fileCount} file(s)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(48.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.rejectTransfer() },
                                    modifier = Modifier.weight(1f).height(56.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Decline")
                                }
                                Button(
                                    onClick = { viewModel.acceptTransfer() },
                                    modifier = Modifier.weight(1f).height(56.dp)
                                ) {
                                    Text("Accept")
                                }
                            }
                        }
                        is TransferState.Loading, is TransferState.WaitingForRecipient -> {
                            ReceiveHeroSection(
                                icon = Icons.Default.Sync,
                                title = "Connecting...",
                                subtitle = "Finding sender and negotiating secure connection"
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            CircularProgressIndicator(modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(48.dp))
                            OutlinedButton(onClick = { viewModel.cancelTransfer() }) {
                                Text("Cancel")
                            }
                        }
                        is TransferState.Transferring -> {
                            ReceiveHeroSection(
                                icon = Icons.Outlined.CloudDownload,
                                title = "Receiving Files",
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
                        is TransferState.Success -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(96.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Received Successfully!", style = MaterialTheme.typography.headlineMedium)
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            if (state.receivedFiles.isNotEmpty()) {
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(state.receivedFiles.size) { index ->
                                        val fileName = state.receivedFiles[index]
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = fileName,
                                                    modifier = Modifier.weight(1f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                IconButton(onClick = { viewModel.openFile(fileName) }) {
                                                    Icon(Icons.Default.OpenInNew, contentDescription = "Open")
                                                }
                                                IconButton(onClick = { viewModel.shareFile(fileName) }) {
                                                    Icon(Icons.Default.Share, contentDescription = "Share")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.resetState() }, modifier = Modifier.fillMaxWidth()) {
                                Text("Done")
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
private fun ReceiveHeroSection(icon: ImageVector, title: String, subtitle: String) {
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
        Text(text = title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
