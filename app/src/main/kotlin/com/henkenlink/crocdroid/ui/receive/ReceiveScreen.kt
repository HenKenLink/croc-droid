package com.henkenlink.crocdroid.ui.receive

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.henkenlink.crocdroid.domain.model.TransferState

@Composable
fun ReceiveScreen(
    viewModel: ReceiveViewModel,
    modifier: Modifier = Modifier,
) {
    val transferState by viewModel.transferState.collectAsStateWithLifecycle()
    val code by viewModel.receiveCode.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (val state = transferState) {
            is TransferState.Idle -> {
                OutlinedTextField(
                    value = code,
                    onValueChange = { viewModel.updateReceiveCode(it) },
                    label = { Text("Code") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.receiveFile(code) },
                    enabled = code.isNotBlank(),
                ) {
                    Text("Receive")
                }
            }
            is TransferState.FileOffer -> {
                AlertDialog(
                    onDismissRequest = { viewModel.rejectTransfer() },
                    title = { Text("Incoming Transfer") },
                    text = {
                        Column {
                            Text("File: ${state.fileName}")
                            Text("Size: ${state.fileSize} bytes")
                            Text("Total Files: ${state.fileCount}")
                        }
                    },
                    confirmButton = {
                        Button(onClick = { viewModel.acceptTransfer() }) {
                            Text("Accept")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.rejectTransfer() }) {
                            Text("Reject")
                        }
                    }
                )
                // Also show a loading indicator in background
                CircularProgressIndicator()
                Text("Waiting for your confirmation...")
            }
            is TransferState.Loading, is TransferState.WaitingForRecipient -> {
                CircularProgressIndicator()
                Text("Connecting...")
                Button(onClick = { viewModel.cancelTransfer() }) {
                    Text("Cancel")
                }
            }
            is TransferState.Transferring -> {
                Text("Receiving...")
                LinearProgressIndicator(
                    progress = { if (state.totalBytes > 0) state.sentBytes.toFloat() / state.totalBytes else 0f },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                )
                Text("${state.sentBytes} / ${state.totalBytes} bytes")
                Button(onClick = { viewModel.cancelTransfer() }) {
                    Text("Cancel")
                }
            }
            is TransferState.Success -> {
                Text("Received Successfully!", style = MaterialTheme.typography.headlineMedium)
                Button(onClick = { viewModel.resetState() }) {
                    Text("Receive Another")
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
