package com.henkenlink.crocdroid.ui.send

import android.content.Context
import android.net.Uri
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
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import com.henkenlink.crocdroid.domain.model.HistoryEntry
import com.henkenlink.crocdroid.domain.model.TransferType
import com.henkenlink.crocdroid.domain.model.TransferState

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

        viewModelScope.launch {
            crocEngine.resetState()
            try {
                val filePaths = withContext(Dispatchers.IO) {
                    uris.mapNotNull { uri ->
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            var fileName = "transfer_file_${System.currentTimeMillis()}"
                            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    val displayNameIndex = cursor.getColumnIndex("_display_name")
                                    if (displayNameIndex != -1) {
                                        val name = cursor.getString(displayNameIndex)
                                        if (!name.isNullOrBlank()) fileName = name
                                    }
                                }
                            }

                            val destFile = File(tempDir, fileName)
                            FileOutputStream(destFile).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                            destFile.absolutePath
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

                crocEngine.sendFiles(filePaths, code, settings)
                
                // Add to history on success
                settingsRepository.addHistoryEntry(HistoryEntry(
                    id = UUID.randomUUID().toString(),
                    type = TransferType.SEND,
                    timestamp = System.currentTimeMillis(),
                    fileName = if (uris.size == 1) filePaths[0].substringAfterLast("/") else "${uris.size} items",
                    fileSize = filePaths.sumOf { File(it).length() },
                    fileCount = filePaths.size,
                    success = true
                ))
            } catch (e: Exception) {
                // Add to history on error
                settingsRepository.addHistoryEntry(HistoryEntry(
                    id = UUID.randomUUID().toString(),
                    type = TransferType.SEND,
                    timestamp = System.currentTimeMillis(),
                    fileName = "Unknown",
                    fileSize = 0,
                    fileCount = uris.size,
                    success = false,
                    errorMessage = e.message
                ))
            }
        }
    }

    fun cancelTransfer() {
        crocEngine.cancelTransfer()
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
                return SendViewModel(crocEngine, settingsRepository, context) as T
            }
        }
    }
}
