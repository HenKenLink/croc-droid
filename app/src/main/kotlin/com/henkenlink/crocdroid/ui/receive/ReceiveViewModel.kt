package com.henkenlink.crocdroid.ui.receive

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.henkenlink.crocdroid.data.croc.CrocEngine
import com.henkenlink.crocdroid.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import com.henkenlink.crocdroid.domain.model.HistoryEntry
import com.henkenlink.crocdroid.domain.model.TransferType
import com.henkenlink.crocdroid.domain.model.TransferState
import java.util.UUID

class ReceiveViewModel(
    private val crocEngine: CrocEngine,
    private val settingsRepository: SettingsRepository,
    private val context: Context,
) : ViewModel() {

    val transferState = crocEngine.transferState

    // Persisted UI state — survives navigation
    private val _receiveCode = MutableStateFlow("")
    val receiveCode: StateFlow<String> = _receiveCode.asStateFlow()

    init {
        // Pre-fill from fixed receive code if configured
        val fixedCode = settingsRepository.settingsState.value.fixedReceiveCode
        if (fixedCode.isNotBlank()) {
            _receiveCode.value = fixedCode
        }
    }

    fun updateReceiveCode(code: String) {
        _receiveCode.value = code
    }

    fun receiveFile(code: String) {
        viewModelScope.launch {
            crocEngine.resetState()
            try {
                val settings = settingsRepository.settingsState.value
                val customPath = settings.downloadPath
                val saveDir = if (customPath.isNotBlank()) {
                    File(customPath)
                } else {
                    File(context.filesDir, "downloads")
                }
                
                if (!saveDir.exists()) {
                    saveDir.mkdirs()
                }

                crocEngine.receiveFile(code, saveDir.absolutePath, settings)
            } catch (e: Exception) {
                // handle error
            }
        }
        
        // History listener
        viewModelScope.launch {
            crocEngine.transferState.collect { state ->
                when (state) {
                    is TransferState.Success -> {
                        // Success history
                        settingsRepository.addHistoryEntry(HistoryEntry(
                            id = UUID.randomUUID().toString(),
                            type = TransferType.RECEIVE,
                            timestamp = System.currentTimeMillis(),
                            fileName = "Inbound Files", // Should ideally be from state
                            fileSize = 0,
                            fileCount = 1,
                            success = true
                        ))
                    }
                    is TransferState.Error -> {
                        // Error history
                        settingsRepository.addHistoryEntry(HistoryEntry(
                            id = UUID.randomUUID().toString(),
                            type = TransferType.RECEIVE,
                            timestamp = System.currentTimeMillis(),
                            fileName = "Failed Inbound",
                            fileSize = 0,
                            fileCount = 0,
                            success = false,
                            errorMessage = state.message
                        ))
                    }
                    else -> {}
                }
            }
        }
    }

    fun cancelTransfer() {
        crocEngine.cancelTransfer()
    }

    fun acceptTransfer() {
        crocEngine.acceptReceive()
    }

    fun rejectTransfer() {
        crocEngine.rejectReceive()
    }

    fun resetState() {
        crocEngine.resetState()
    }

    companion object {
        fun provideFactory(
            crocEngine: CrocEngine,
            settingsRepository: SettingsRepository,
            context: Context,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ReceiveViewModel(crocEngine, settingsRepository, context) as T
            }
        }
    }
}
