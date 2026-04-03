package com.henkenlink.crocdroid.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.henkenlink.crocdroid.data.settings.SettingsRepository
import com.henkenlink.crocdroid.domain.model.CrocSettings

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val context: android.content.Context
) : ViewModel() {

    fun getContext() = context

    val settingsState = settingsRepository.settingsState
    val historyState = settingsRepository.historyState

    fun updateSettings(settings: CrocSettings) {
        settingsRepository.updateSettings(settings)
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
