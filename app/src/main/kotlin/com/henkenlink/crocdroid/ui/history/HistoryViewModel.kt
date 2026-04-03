package com.henkenlink.crocdroid.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.henkenlink.crocdroid.data.settings.SettingsRepository
import com.henkenlink.crocdroid.domain.model.HistoryEntry
import kotlinx.coroutines.flow.StateFlow

class HistoryViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {
    val historyState: StateFlow<List<HistoryEntry>> = settingsRepository.historyState

    fun removeEntry(id: String) {
        settingsRepository.removeHistoryEntry(id)
    }

    fun clearAll() {
        settingsRepository.clearHistory()
    }

    companion object {
        fun provideFactory(settingsRepository: SettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HistoryViewModel(settingsRepository) as T
                }
            }
    }
}
