package com.henkenlink.crocdroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.henkenlink.crocdroid.ui.theme.CrocDroidTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsRepository = SettingsRepository(this)
        val crocEngine = CrocEngine()

        setContent {
            CrocDroidTheme {
                val navController = rememberNavController()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentDestination = navBackStackEntry?.destination

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
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = SendRoute,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable<SendRoute> {
                            val viewModel: SendViewModel = viewModel(
                                factory = SendViewModel.provideFactory(crocEngine, settingsRepository, LocalContext.current)
                            )
                            SendScreen(viewModel)
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
                                factory = SettingsViewModel.provideFactory(settingsRepository)
                            )
                            SettingsScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}
