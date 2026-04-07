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
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
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
    private var currentSendJob: Job? = null

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
                            fileCount = uris.size.toLong(),
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
                            fileCount = _selectedFileUris.value.size.toLong(),
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
        
        // Prevent duplicate sends
        if (currentSendJob?.isActive == true) {
            android.util.Log.d("SendViewModel", "sendSelectedFiles: currentSendJob is still active, ignoring")
            return
        }
        
        android.util.Log.d("SendViewModel", "sendSelectedFiles: starting new send job")

        val tempDir = File(context.cacheDir, "temp_send_${System.currentTimeMillis()}")
        if (!tempDir.exists()) tempDir.mkdirs()
        currentTempDir = tempDir

        currentSendJob = viewModelScope.launch {
            isMyTransfer = true
            android.util.Log.d("SendViewModel", "sendSelectedFiles: job started, setting loading")
            crocEngine.setLoading()
            
            try {
                android.util.Log.d("SendViewModel", "sendSelectedFiles: resolving code")
                // Code resolution priority
                val settings = settingsRepository.settingsState.value
                val customCodeVal = _customCode.value
                val code = customCodeVal.takeIf { it.isNotBlank() }
                    ?: settings.fixedSendCode.takeIf { it.isNotBlank() }
                    ?: crocEngine.generateCode()

                android.util.Log.d("SendViewModel", "sendSelectedFiles: copying files to temp dir")
                val filePaths = withContext(Dispatchers.IO) {
                    val paths = uris.mapNotNull { uri ->
                        ensureActive()
                        val docFile = DocumentFile.fromSingleUri(context, uri) ?: DocumentFile.fromTreeUri(context, uri)
                        val isDirectory = docFile?.isDirectory == true

                        if (isDirectory) {
                            val dirName = docFile?.name ?: "folder"
                            val subDir = File(tempDir, dirName).apply { mkdirs() }
                            if (FileUtil.copyDirectoryToFolder(context, uri, subDir)) {
                                subDir.absolutePath
                            } else null
                        } else {
                            val fileName = docFile?.name ?: "file_${System.currentTimeMillis()}"
                            val destFile = File(tempDir, fileName)
                            if (FileUtil.copyUriToFile(context, uri, destFile)) {
                                destFile.absolutePath
                            } else null
                        }
                    }
                    paths
                }

                android.util.Log.d("SendViewModel", "sendSelectedFiles: copied ${filePaths.size} files")
                
                if (filePaths.isEmpty()) {
                    android.util.Log.d("SendViewModel", "sendSelectedFiles: no files copied, aborting")
                    crocEngine.resetState()
                    isMyTransfer = false
                    cleanupTempDir()
                    return@launch
                }

                var finalPaths = filePaths
                var isZip = false

                android.util.Log.d("SendViewModel", "sendSelectedFiles: checking if need to zip")
                // If any is a directory or multiple files, and auto-zip is on, zip them!
                val hasDirectory = filePaths.any { File(it).isDirectory }
                if (settings.autoZipFolders && (hasDirectory || filePaths.size > 1)) {
                    android.util.Log.d("SendViewModel", "sendSelectedFiles: zipping files")
                    withContext(Dispatchers.IO) {
                        val zipName = if (filePaths.size == 1) {
                            File(filePaths.first()).name + ".zip"
                        } else {
                            "archive_${System.currentTimeMillis()}.zip"
                        }
                        val zipFile = File(tempDir, zipName)
                        com.henkenlink.crocdroid.data.util.CompressionUtil.zipFiles(
                            filePaths.map { File(it) },
                            zipFile
                        )
                        finalPaths = listOf(zipFile.absolutePath)
                        isZip = true
                    }
                    android.util.Log.d("SendViewModel", "sendSelectedFiles: zipped to ${finalPaths.first()}")
                }

                android.util.Log.d("SendViewModel", "sendSelectedFiles: starting transfer service")
                context.startService(Intent(context, TransferService::class.java))
                android.util.Log.d("SendViewModel", "sendSelectedFiles: calling crocEngine.sendFiles")
                crocEngine.sendFiles(finalPaths, code, settings, isTempZip = isZip)
                android.util.Log.d("SendViewModel", "sendSelectedFiles: crocEngine.sendFiles returned")
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    // Job was cancelled, clean up
                    isMyTransfer = false
                    cleanupTempDir()
                    throw e
                }
                // Other errors handled by collector
                isMyTransfer = false
                cleanupTempDir()
            }
        }
    }

    fun cancelTransfer() {
        android.util.Log.d("SendViewModel", "cancelTransfer: cancelling job, isActive=${currentSendJob?.isActive}")
        isMyTransfer = false
        currentSendJob?.cancel()
        currentSendJob = null
        cleanupTempDir()
        crocEngine.cancelTransfer()
        // Reset state to allow new transfers
        crocEngine.resetState()
        android.util.Log.d("SendViewModel", "cancelTransfer: done")
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
