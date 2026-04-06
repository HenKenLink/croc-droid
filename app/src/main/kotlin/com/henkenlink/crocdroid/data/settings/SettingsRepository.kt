package com.henkenlink.crocdroid.data.settings

import android.content.Context
import androidx.core.content.edit
import com.henkenlink.crocdroid.domain.model.CrocSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.henkenlink.crocdroid.domain.model.HistoryEntry

class SettingsRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("croc_settings", Context.MODE_PRIVATE)
    private val _settingsState = MutableStateFlow(loadSettings())
    
    val settingsState: StateFlow<CrocSettings> = _settingsState.asStateFlow()

    private fun loadSettings(): CrocSettings {
        val jsonString = prefs.getString("settings_json", null)
        return if (jsonString != null) {
            try {
                Json.decodeFromString(jsonString)
            } catch (e: Exception) {
                CrocSettings()
            }
        } else {
            CrocSettings()
        }
    }

    fun updateSettings(settings: CrocSettings) {
        val jsonString = Json.encodeToString(settings)
        prefs.edit {
            putString("settings_json", jsonString)
        }
        _settingsState.value = settings
    }

    // History management
    private val _historyState = MutableStateFlow(loadHistory())
    val historyState: StateFlow<List<HistoryEntry>> = _historyState.asStateFlow()

    private fun loadHistory(): List<HistoryEntry> {
        val jsonString = prefs.getString("history_json", null)
        return if (jsonString != null) {
            try {
                Json.decodeFromString(jsonString)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    fun addHistoryEntry(entry: HistoryEntry) {
        val currentHistory = _historyState.value.toMutableList()
        currentHistory.add(0, entry) // Newest first
        // Limit history to 100 entries
        val limitedHistory = currentHistory.take(100)
        
        val jsonString = Json.encodeToString(limitedHistory)
        prefs.edit {
            putString("history_json", jsonString)
        }
        _historyState.value = limitedHistory
    }

    fun removeHistoryEntry(id: String) {
        val updatedHistory = _historyState.value.filter { it.id != id }
        val jsonString = Json.encodeToString(updatedHistory)
        prefs.edit {
            putString("history_json", jsonString)
        }
        _historyState.value = updatedHistory
    }

    fun clearHistory() {
        prefs.edit {
            remove("history_json")
        }
        _historyState.value = emptyList()
    }

    // Receive History management
    private val _receiveHistoryState = MutableStateFlow(loadReceiveHistory())
    val receiveHistoryState: StateFlow<List<com.henkenlink.crocdroid.domain.model.ReceiveHistoryEntry>> = _receiveHistoryState.asStateFlow()

    private fun loadReceiveHistory(): List<com.henkenlink.crocdroid.domain.model.ReceiveHistoryEntry> {
        val jsonString = prefs.getString("receive_history_json", null)
        return if (jsonString != null) {
            try {
                Json.decodeFromString(jsonString)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    fun addReceiveHistoryEntry(entry: com.henkenlink.crocdroid.domain.model.ReceiveHistoryEntry) {
        val currentHistory = _receiveHistoryState.value.toMutableList()
        currentHistory.add(0, entry) // Newest first
        // Limit history to 200 entries
        val limitedHistory = currentHistory.take(200)
        
        val jsonString = Json.encodeToString(limitedHistory)
        prefs.edit {
            putString("receive_history_json", jsonString)
        }
        _receiveHistoryState.value = limitedHistory
    }

    fun removeReceiveHistoryEntry(id: String) {
        val entry = _receiveHistoryState.value.find { it.id == id }
        entry?.filePaths?.forEach { filePath ->
            val file = java.io.File(filePath)
            // Only delete if it's in our app's cache or data directory
            if (file.exists() && (file.absolutePath.startsWith(context.cacheDir.absolutePath) || file.absolutePath.startsWith(context.filesDir.absolutePath))) {
                file.delete()
            }
        }

        val updatedHistory = _receiveHistoryState.value.filter { it.id != id }
        val jsonString = Json.encodeToString(updatedHistory)
        prefs.edit {
            putString("receive_history_json", jsonString)
        }
        _receiveHistoryState.value = updatedHistory
    }

    fun clearReceiveHistory() {
        _receiveHistoryState.value.forEach { entry ->
            entry.filePaths.forEach { filePath ->
                val file = java.io.File(filePath)
                if (file.exists() && (file.absolutePath.startsWith(context.cacheDir.absolutePath) || file.absolutePath.startsWith(context.filesDir.absolutePath))) {
                    file.delete()
                }
            }
        }
        prefs.edit {
            remove("receive_history_json")
        }
        _receiveHistoryState.value = emptyList()
    }

    fun pruneReceiveHistory() {
        val currentHistory = _receiveHistoryState.value
        val validHistory = currentHistory.filter { entry ->
            entry.filePaths.any { java.io.File(it).exists() }
        }
        if (currentHistory.size != validHistory.size) {
            val jsonString = Json.encodeToString(validHistory)
            prefs.edit {
                putString("receive_history_json", jsonString)
            }
            _receiveHistoryState.value = validHistory
        }
    }
}

