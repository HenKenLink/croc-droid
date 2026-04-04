package com.henkenlink.crocdroid.ui.debug

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.henkenlink.crocdroid.data.croc.CrocEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DebugLogViewModel(
    private val crocEngine: CrocEngine,
    private val context: Context,
) : ViewModel() {

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private var isPolling = false

    init {
        startPolling()
    }

    private fun startPolling() {
        if (isPolling) return
        isPolling = true
        viewModelScope.launch {
            while (isPolling) {
                val newLogs = crocEngine.getDebugLog()
                if (newLogs.isNotEmpty()) {
                    val lines = newLogs.split("\n").filter { it.isNotBlank() }
                    // Simple logic: if buffer is huge, just show last 1000 lines
                    _logs.value = lines.takeLast(1000)
                }
                delay(1000)
            }
        }
    }

    fun clearLogs() {
        crocEngine.clearDebugLog()
        _logs.value = emptyList()
    }

    fun exportLogs() {
        val logText = _logs.value.joinToString("\n")
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, logText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Export Logs")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    override fun onCleared() {
        super.onCleared()
        isPolling = false
    }

    companion object {
        fun provideFactory(
            crocEngine: CrocEngine,
            context: Context,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DebugLogViewModel(crocEngine, context.applicationContext) as T
            }
        }
    }
}
