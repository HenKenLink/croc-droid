package com.henkenlink.crocdroid.ui.history

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.henkenlink.crocdroid.data.settings.SettingsRepository
import com.henkenlink.crocdroid.domain.model.ReceiveHistoryEntry
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class ReceiveHistoryViewModel(
    private val settingsRepository: SettingsRepository,
    private val context: Context
) : ViewModel() {
    
    val receiveHistoryState: StateFlow<List<ReceiveHistoryEntry>> = 
        settingsRepository.receiveHistoryState
    
    fun deleteHistoryEntry(id: String) {
        viewModelScope.launch {
            settingsRepository.removeReceiveHistoryEntry(id)
        }
    }
    
    fun clearHistory() {
        viewModelScope.launch {
            settingsRepository.clearReceiveHistory()
        }
    }
    
    fun openHistoryFile(filePath: String) {
        viewModelScope.launch {
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    return@launch
                }
                
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, context.contentResolver.getType(uri))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                
                context.startActivity(Intent.createChooser(intent, "Open with"))
            } catch (e: Exception) {
                Log.e("ReceiveHistoryViewModel", "Failed to open file", e)
            }
        }
    }
    
    fun shareHistoryFile(filePath: String) {
        viewModelScope.launch {
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    return@launch
                }
                
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = context.contentResolver.getType(uri)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                
                context.startActivity(Intent.createChooser(intent, "Share with"))
            } catch (e: Exception) {
                Log.e("ReceiveHistoryViewModel", "Failed to share file", e)
            }
        }
    }
    
    companion object {
        fun provideFactory(
            settingsRepository: SettingsRepository,
            context: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ReceiveHistoryViewModel(settingsRepository, context) as T
            }
        }
    }
}
