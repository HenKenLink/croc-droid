package com.henkenlink.crocdroid.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.henkenlink.crocdroid.data.settings.SettingsRepository
import com.henkenlink.crocdroid.domain.model.CrocSettings
import com.henkenlink.crocdroid.domain.model.RelayConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val context: android.content.Context
) : ViewModel() {

    fun getContext() = context

    val settingsState = settingsRepository.settingsState
    val historyState = settingsRepository.historyState
    val relayConfigsState = settingsRepository.relayConfigsState

    val selectedRelayConfig: StateFlow<RelayConfig?> = 
        settingsRepository.settingsState.map { settings ->
            settingsRepository.relayConfigsState.value.find { 
                it.id == settings.selectedRelayConfigId 
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateSettings(settings: CrocSettings) {
        settingsRepository.updateSettings(settings)
    }

    fun selectRelayConfig(configId: String) {
        settingsRepository.selectRelayConfig(configId)
    }

    fun deleteHistoryEntry(id: String) {
        settingsRepository.removeHistoryEntry(id)
    }

    fun clearHistory() {
        settingsRepository.clearHistory()
    }

    companion object {
        fun provideFactory(
            settingsRepository: SettingsRepository,
            context: android.content.Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(settingsRepository, context.applicationContext) as T
            }
        }
    }
}
