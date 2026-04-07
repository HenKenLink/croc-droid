package com.henkenlink.crocdroid.ui.relay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.henkenlink.crocdroid.data.settings.SettingsRepository
import com.henkenlink.crocdroid.domain.model.RelayConfig
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RelayConfigViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    val relayConfigsState: StateFlow<List<RelayConfig>> = 
        settingsRepository.relayConfigsState
    
    fun addConfig(name: String, address: String, ports: String, password: String) {
        viewModelScope.launch {
            val config = RelayConfig(
                name = name,
                relayAddress = address,
                relayPorts = ports,
                relayPassword = password
            )
            settingsRepository.addRelayConfig(config)
        }
    }
    
    fun updateConfig(id: String, name: String, address: String, ports: String, password: String) {
        viewModelScope.launch {
            val config = RelayConfig(
                id = id,
                name = name,
                relayAddress = address,
                relayPorts = ports,
                relayPassword = password
            )
            settingsRepository.updateRelayConfig(config)
        }
    }
    
    fun deleteConfig(id: String) {
        viewModelScope.launch {
            settingsRepository.removeRelayConfig(id)
        }
    }
    
    companion object {
        fun provideFactory(
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RelayConfigViewModel(settingsRepository) as T
            }
        }
    }
}
