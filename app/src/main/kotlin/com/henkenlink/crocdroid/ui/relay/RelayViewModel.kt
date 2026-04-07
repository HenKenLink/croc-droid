package com.henkenlink.crocdroid.ui.relay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.henkenlink.crocdroid.data.croc.CrocEngine
import com.henkenlink.crocdroid.data.settings.SettingsRepository
import com.henkenlink.crocdroid.domain.model.RelayConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class RelayViewModel(
    private val crocEngine: CrocEngine,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _relayRunning = MutableStateFlow(false)
    val relayRunning: StateFlow<Boolean> = _relayRunning.asStateFlow()

    private var currentRelayId: String? = null

    val relayConfigsState: StateFlow<List<RelayConfig>> = 
        settingsRepository.relayConfigsState

    val selectedRelayConfig: StateFlow<RelayConfig?> = 
        settingsRepository.settingsState.map { settings ->
            settingsRepository.relayConfigsState.value.find { 
                it.id == settings.selectedRelayConfigId 
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectRelayConfig(configId: String) {
        settingsRepository.selectRelayConfig(configId)
    }

    fun startRelay(host: String, port: String, password: String) {
        if (!_relayRunning.value) {
            currentRelayId = crocEngine.startRelay(host, port, password)
            _relayRunning.value = true
        }
    }

    fun stopRelay() {
        currentRelayId?.let {
            crocEngine.stopRelay(it)
            currentRelayId = null
            _relayRunning.value = false
        }
    }

    companion object {
        fun provideFactory(
            crocEngine: CrocEngine,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RelayViewModel(crocEngine, settingsRepository) as T
            }
        }
    }
}
