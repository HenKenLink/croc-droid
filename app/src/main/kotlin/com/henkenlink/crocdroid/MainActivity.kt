package com.henkenlink.crocdroid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.henkenlink.crocdroid.data.croc.CrocEngine
import com.henkenlink.crocdroid.data.settings.SettingsRepository
import com.henkenlink.crocdroid.navigation.*
import com.henkenlink.crocdroid.ui.receive.ReceiveScreen
import com.henkenlink.crocdroid.ui.receive.ReceiveViewModel
import com.henkenlink.crocdroid.ui.relay.RelayScreen
import com.henkenlink.crocdroid.ui.relay.RelayViewModel
import com.henkenlink.crocdroid.ui.send.SendScreen
import com.henkenlink.crocdroid.ui.send.SendViewModel
import com.henkenlink.crocdroid.ui.settings.SettingsScreen
import com.henkenlink.crocdroid.ui.settings.SettingsViewModel
import com.henkenlink.crocdroid.ui.debug.DebugLogScreen
import com.henkenlink.crocdroid.ui.debug.DebugLogViewModel
import com.henkenlink.crocdroid.ui.theme.CrocDroidTheme

class MainActivity : ComponentActivity() {
    
    private val sendViewModel: SendViewModel by viewModels {
        val app = application as CrocDroidApp
        SendViewModel.provideFactory(app.crocEngine, app.settingsRepository, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as CrocDroidApp
        val settingsRepository = app.settingsRepository
        val crocEngine = app.crocEngine

        requestNotificationPermission()

        setContent {
            val settings by settingsRepository.settingsState.collectAsStateWithLifecycle()
            
            CrocDroidTheme(themeMode = settings.themeMode) {
                val navController = rememberNavController()

                // Handle initial intent and future intents
                LaunchedEffect(Unit) {
                    handleIntent(intent, navController)
                }
                
                DisposableEffect(Unit) {
                    val listener = androidx.core.util.Consumer<Intent> { intent ->
                        handleIntent(intent, navController)
                    }
                    addOnNewIntentListener(listener)
                    onDispose { removeOnNewIntentListener(listener) }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination
                        val showBottomBar = currentDestination?.route?.contains("DebugLogRoute") != true

                        if (showBottomBar) {
                            NavigationBar {
                                NavigationBarItem(
                                    icon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send") },
                                    label = { Text("Send") },
                                    selected = currentDestination?.hierarchy?.any { it.route?.contains("SendRoute") == true } == true,
                                    onClick = {
                                        navController.navigate(SendRoute) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.CallReceived, contentDescription = "Receive") },
                                    label = { Text("Receive") },
                                    selected = currentDestination?.hierarchy?.any { it.route?.contains("ReceiveRoute") == true } == true,
                                    onClick = {
                                        navController.navigate(ReceiveRoute) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Router, contentDescription = "Relay") },
                                    label = { Text("Relay") },
                                    selected = currentDestination?.hierarchy?.any { it.route?.contains("RelayRoute") == true } == true,
                                    onClick = {
                                        navController.navigate(RelayRoute) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                    label = { Text("Settings") },
                                    selected = currentDestination?.hierarchy?.any { it.route?.contains("SettingsRoute") == true } == true,
                                    onClick = {
                                        navController.navigate(SettingsRoute) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = SendRoute,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable<SendRoute> {
                            SendScreen(sendViewModel)
                        }
                        composable<ReceiveRoute> {
                            val viewModel: ReceiveViewModel = viewModel(
                                factory = ReceiveViewModel.provideFactory(crocEngine, settingsRepository, LocalContext.current)
                            )
                            ReceiveScreen(viewModel)
                        }
                        composable<RelayRoute> {
                            val viewModel: RelayViewModel = viewModel(
                                factory = RelayViewModel.provideFactory(crocEngine)
                            )
                            RelayScreen(viewModel)
                        }
                        composable<SettingsRoute> {
                            val viewModel: SettingsViewModel = viewModel(
                                factory = SettingsViewModel.provideFactory(settingsRepository, LocalContext.current)
                            )
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateToLogs = { navController.navigate(DebugLogRoute) }
                            )
                        }
                        composable<DebugLogRoute> {
                            val debugLogViewModel: DebugLogViewModel = viewModel(
                                factory = DebugLogViewModel.provideFactory(crocEngine, LocalContext.current)
                            )
                            DebugLogScreen(
                                viewModel = debugLogViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(permission)
            }
        }
    }

    private fun handleIntent(intent: Intent?, navController: androidx.navigation.NavController) {
        if (intent == null) return
        
        val action = intent.action
        val type = intent.type
        
        if ((Intent.ACTION_SEND == action || Intent.ACTION_SEND_MULTIPLE == action) && type != null) {
            val uris = when (action) {
                Intent.ACTION_SEND -> {
                    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    }
                    uri?.let { listOf(it) } ?: emptyList()
                }
                Intent.ACTION_SEND_MULTIPLE -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                    } ?: emptyList()
                }
                else -> emptyList()
            }
            
            if (uris.isNotEmpty()) {
                sendViewModel.addFiles(uris)
                // Jump to Send screen
                navController.navigate(SendRoute) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }
}
