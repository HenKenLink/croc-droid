package com.henkenlink.crocdroid.ui.send

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.henkenlink.crocdroid.service.TransferService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.henkenlink.crocdroid.data.croc.CrocEngine
import com.henkenlink.crocdroid.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import com.henkenlink.crocdroid.domain.model.HistoryEntry
import com.henkenlink.crocdroid.domain.model.TransferType
import com.henkenlink.crocdroid.domain.model.TransferState
import com.henkenlink.crocdroid.data.util.FileUtil
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.collect
import java.io.File
import java.io.FileOutputStream

class SendViewModel(
    private val crocEngine: CrocEngine,
    private val settingsRepository: SettingsRepository,
    private val context: Context,
) : ViewModel() {

    val transferState = crocEngine.transferState

    // Persisted UI state — survives navigation
    private val _customCode = MutableStateFlow("")
    val customCode: StateFlow<String> = _customCode.asStateFlow()

    private val _selectedFileUris = MutableStateFlow<List<Uri>>(emptyList())
    val selectedFileUris: StateFlow<List<Uri>> = _selectedFileUris.asStateFlow()
    
    private var isMyTransfer = false
    private var currentTempDir: File? = null

    init {
        // Correct history recording: observe state transitions
        viewModelScope.launch {
            crocEngine.transferState.collect { state ->
                // Only record history if this ViewModel initiated the transfer
                if (!isMyTransfer) return@collect
                
                when (state) {
                    is TransferState.Success -> {
                        val uris = _selectedFileUris.value
                        settingsRepository.addHistoryEntry(HistoryEntry(
                            id = UUID.randomUUID().toString(),
                            type = TransferType.SEND,
                            timestamp = System.currentTimeMillis(),
                            fileName = if (uris.size == 1) "File/Folder" else "${uris.size} items",
                            fileSize = 0,
                            fileCount = uris.size,
                            success = true
                        ))
                        cleanupTempDir()
                        isMyTransfer = false
                    }
                    is TransferState.Error -> {
                        settingsRepository.addHistoryEntry(HistoryEntry(
                            id = UUID.randomUUID().toString(),
                            type = TransferType.SEND,
                            timestamp = System.currentTimeMillis(),
                            fileName = "Transfer Failed",
                            fileSize = 0,
                            fileCount = _selectedFileUris.value.size,
                            success = false,
                            errorMessage = state.message
                        ))
                        cleanupTempDir()
                        isMyTransfer = false
                    }
                    else -> {}
                }
            }
        }
    }

    private fun cleanupTempDir() {
        currentTempDir?.let {
            if (it.exists()) it.deleteRecursively()
        }
        currentTempDir = null
    }

    fun updateCustomCode(code: String) {
        _customCode.value = code
    }

    fun addFiles(uris: List<Uri>) {
        _selectedFileUris.value = _selectedFileUris.value + uris
    }

    fun removeFile(index: Int) {
        _selectedFileUris.value = _selectedFileUris.value.toMutableList().apply {
            if (index in indices) removeAt(index)
        }
    }

    fun clearFiles() {
        _selectedFileUris.value = emptyList()
    }

    fun sendSelectedFiles() {
        val uris = _selectedFileUris.value
        if (uris.isEmpty()) return

        val tempDir = File(context.cacheDir, "temp_send_${System.currentTimeMillis()}")
        if (!tempDir.exists()) tempDir.mkdirs()
        currentTempDir = tempDir

        viewModelScope.launch {
            crocEngine.resetState()
            isMyTransfer = true
            try {
                val filePaths = withContext(Dispatchers.IO) {
                    uris.mapNotNull { uri ->
                        val isDirectory = try {
                            context.contentResolver.getType(uri) == null && DocumentFile.fromTreeUri(context, uri)?.isDirectory == true
                        } catch (e: Exception) { false }

                        if (isDirectory) {
                            val dirName = DocumentFile.fromTreeUri(context, uri)?.name ?: "folder"
                            val subDir = File(tempDir, dirName).apply { mkdirs() }
                            if (FileUtil.copyDirectoryToFolder(context, uri, subDir)) {
                                subDir.absolutePath
                            } else null
                        } else {
                            val fileName = DocumentFile.fromSingleUri(context, uri)?.name ?: "file_${System.currentTimeMillis()}"
                            val destFile = File(tempDir, fileName)
                            if (FileUtil.copyUriToFile(context, uri, destFile)) {
                                destFile.absolutePath
                            } else null
                        }
                    }
                }

                if (filePaths.isEmpty()) {
                    crocEngine.resetState()
                    return@launch
                }

                // Code resolution priority: UI input → fixedSendCode from settings → auto-generated
                val settings = settingsRepository.settingsState.value
                val customCodeVal = _customCode.value
                val code = customCodeVal.takeIf { it.isNotBlank() }
                    ?: settings.fixedSendCode.takeIf { it.isNotBlank() }
                    ?: crocEngine.generateCode()

                context.startService(Intent(context, TransferService::class.java))
                crocEngine.sendFiles(filePaths, code, settings)
            } catch (e: Exception) {
                // actual history recorded via collector
            }
        }
    }

    fun cancelTransfer() {
        crocEngine.cancelTransfer()
        cleanupTempDir()
        isMyTransfer = false
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
                return SendViewModel(crocEngine, settingsRepository, context.applicationContext) as T
            }
        }
    }
}
