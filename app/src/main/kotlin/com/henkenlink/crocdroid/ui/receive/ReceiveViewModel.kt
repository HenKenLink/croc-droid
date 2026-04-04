package com.henkenlink.crocdroid.ui.receive

import android.content.Context
import android.content.Intent
import com.henkenlink.crocdroid.service.TransferService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.henkenlink.crocdroid.data.croc.CrocEngine
import com.henkenlink.crocdroid.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.henkenlink.crocdroid.domain.model.TransferState
import com.henkenlink.crocdroid.domain.model.HistoryEntry
import com.henkenlink.crocdroid.domain.model.TransferType
import com.henkenlink.crocdroid.data.util.FileUtil
import java.util.UUID
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import android.webkit.MimeTypeMap

class ReceiveViewModel(
    private val crocEngine: CrocEngine,
    private val settingsRepository: SettingsRepository,
    private val context: Context,
) : ViewModel() {

    val transferState = crocEngine.transferState

    // Persisted UI state — survives navigation
    private val _receiveCode = MutableStateFlow("")
    val receiveCode: StateFlow<String> = _receiveCode.asStateFlow()
    
    private var isMyTransfer = false
    private var lastFileOffer: TransferState.FileOffer? = null

    init {
        // Pre-fill from fixed receive code if configured
        val fixedCode = settingsRepository.settingsState.value.fixedReceiveCode
        if (fixedCode.isNotBlank()) {
            _receiveCode.value = fixedCode
        }

        // Correct history recording: observe state transitions
        viewModelScope.launch {
            crocEngine.transferState.collect { state ->
                // Only record history if this ViewModel initiated the transfer
                if (!isMyTransfer) return@collect

                when (state) {
                    is TransferState.FileOffer -> {
                        lastFileOffer = state
                    }
                    is TransferState.Success -> {
                        val offer = lastFileOffer
                        settingsRepository.addHistoryEntry(HistoryEntry(
                            id = UUID.randomUUID().toString(),
                            type = TransferType.RECEIVE,
                            timestamp = System.currentTimeMillis(),
                            fileName = if (state.receivedFiles.size > 1) {
                                "${state.receivedFiles.size} files: ${state.receivedFiles.first()}..."
                            } else {
                                state.receivedFiles.firstOrNull() ?: offer?.fileName ?: "Inbound Files"
                            }, 
                            fileSize = offer?.fileSize ?: 0,
                            fileCount = state.receivedFiles.size.toLong().takeIf { it > 0 } ?: offer?.fileCount?.toLong() ?: 1L,
                            success = true
                        ))
                        lastFileOffer = null

                        // Handle Scoped Storage copy if custom path is set
                        val settings = settingsRepository.settingsState.value
                        if (settings.downloadPath.isNotBlank()) {
                            // MOVE TO IO THREAD
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                val cacheDir = File(context.cacheDir, "receives")
                                val downloadUri = Uri.parse(settings.downloadPath)
                                cacheDir.listFiles()?.forEach { file ->
                                    FileUtil.copyFilesToUri(context, file, downloadUri)
                                    // Removed immediate deletion to allow Open/Share in success screen
                                }
                            }
                        }
                        isMyTransfer = false
                    }
                    is TransferState.Error -> {
                        settingsRepository.addHistoryEntry(HistoryEntry(
                            id = UUID.randomUUID().toString(),
                            type = TransferType.RECEIVE,
                            timestamp = System.currentTimeMillis(),
                            fileName = "Inbound Failed",
                            fileSize = 0,
                            fileCount = 0,
                            success = false,
                            errorMessage = state.message
                        ))
                        isMyTransfer = false
                    }
                    else -> {}
                }
            }
        }
    }

    fun updateReceiveCode(code: String) {
        _receiveCode.value = code
    }

    fun receiveFile(code: String) {
        viewModelScope.launch {
            crocEngine.resetState()
            isMyTransfer = true
            try {
                // Always receive to internal cache first because croc needs direct File access
                val saveDir = File(context.cacheDir, "receives")
                if (!saveDir.exists()) {
                    saveDir.mkdirs()
                } else {
                    saveDir.deleteRecursively()
                    saveDir.mkdirs()
                }

                context.startService(Intent(context, TransferService::class.java))
                crocEngine.receiveFile(code, saveDir.absolutePath, settingsRepository.settingsState.value)
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun cancelTransfer() {
        crocEngine.cancelTransfer()
        isMyTransfer = false
    }

    fun acceptTransfer() {
        crocEngine.acceptReceive()
    }

    fun rejectTransfer() {
        crocEngine.rejectReceive()
    }

    fun openFile(fileName: String) {
        val file = File(File(context.cacheDir, "receives"), fileName)
        if (!file.exists()) return
        
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Open with...").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun shareFile(fileName: String) {
        val file = File(File(context.cacheDir, "receives"), fileName)
        if (!file.exists()) return
        
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share file via...").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
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
                return ReceiveViewModel(crocEngine, settingsRepository, context.applicationContext) as T
            }
        }
    }
}
