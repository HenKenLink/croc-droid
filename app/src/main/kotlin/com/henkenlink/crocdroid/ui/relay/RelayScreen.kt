package com.henkenlink.crocdroid.ui.relay

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun RelayScreen(
    viewModel: RelayViewModel,
    modifier: Modifier = Modifier
) {
    val isRunning by viewModel.relayRunning.collectAsStateWithLifecycle()
    var host by remember { mutableStateOf("0.0.0.0") }
    var port by remember { mutableStateOf("9009") }
    var password by remember { mutableStateOf("pass123") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Local Relay Server", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            label = { Text("Bind Host") },
            enabled = !isRunning,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = port,
            onValueChange = { port = it },
            label = { Text("Base Port") },
            enabled = !isRunning,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            enabled = !isRunning,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isRunning) {
            Text("Relay is running on $host:$port", color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.stopRelay() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Stop Relay")
            }
        } else {
            Button(onClick = { viewModel.startRelay(host, port, password) }) {
                Text("Start Relay")
            }
        }
    }
}
