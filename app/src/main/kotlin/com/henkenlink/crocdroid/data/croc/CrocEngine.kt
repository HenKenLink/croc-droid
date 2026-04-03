package com.henkenlink.crocdroid.data.croc

import com.henkenlink.crocdroid.domain.model.CrocSettings
import com.henkenlink.crocdroid.domain.model.TransferState
import crocbridge.CrocCallback
import crocbridge.Crocbridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class CrocEngine {

    private val _transferState = MutableStateFlow<TransferState>(TransferState.Idle)
    val transferState: StateFlow<TransferState> = _transferState.asStateFlow()

    private var currentTransferId: String? = null

    /**
     * Send multiple files or folders using croc
     */
    suspend fun sendFiles(filePaths: List<String>, code: String, settings: CrocSettings) {
        withContext(Dispatchers.IO) {
            _transferState.value = TransferState.Loading
            val configJson = Json.encodeToString(settings)
            val filePathsJson = Json.encodeToString(filePaths)
            val transferId = UUID.randomUUID().toString()
            currentTransferId = transferId

            Crocbridge.sendFiles(transferId, filePathsJson, code, configJson, object : CrocCallback {
                override fun onReady(p0: String?) {
                    val actualCode = p0 ?: code
                    _transferState.value = TransferState.WaitingForRecipient(actualCode)
                }

                override fun onFileOffer(p0: String?, p1: Long, p2: Long): Boolean {
                    // Senders don't get file offers usually
                    return true
                }

                override fun onProgress(sent: Long, total: Long) {
                    _transferState.value = TransferState.Transferring(sent, total)
                }

                override fun onSuccess() {
                    _transferState.value = TransferState.Success
                    currentTransferId = null
                }

                override fun onError(errStr: String?) {
                    _transferState.value = TransferState.Error(errStr ?: "Unknown error occurred")
                    currentTransferId = null
                }
            })
        }
    }

    /**
     * Receive a file or folder using croc
     */
    suspend fun receiveFile(code: String, saveDir: String, settings: CrocSettings) {
        withContext(Dispatchers.IO) {
            _transferState.value = TransferState.Loading
            val configJson = Json.encodeToString(settings)
            val transferId = UUID.randomUUID().toString()
            currentTransferId = transferId

            Crocbridge.receiveFile(transferId, code, saveDir, configJson, object : CrocCallback {
                override fun onReady(p0: String?) {
                    // Start connecting
                    _transferState.value = TransferState.Loading
                }

                override fun onFileOffer(p0: String?, p1: Long, p2: Long): Boolean {
                    _transferState.value = TransferState.FileOffer(
                        fileName = p0 ?: "Unknown",
                        fileSize = p1,
                        fileCount = p2.toInt()
                    )
                    return true // Callback handled
                }

                override fun onProgress(sent: Long, total: Long) {
                    _transferState.value = TransferState.Transferring(sent, total)
                }

                override fun onSuccess() {
                    _transferState.value = TransferState.Success
                    currentTransferId = null
                }

                override fun onError(errStr: String?) {
                    _transferState.value = TransferState.Error(errStr ?: "Unknown error occurred")
                    currentTransferId = null
                }
            })
        }
    }

    fun cancelTransfer() {
        currentTransferId?.let {
            Crocbridge.cancelTransfer(it)
            _transferState.value = TransferState.Error("Transfer cancelled")
            currentTransferId = null
        }
    }

    fun acceptReceive() {
        currentTransferId?.let {
            Crocbridge.acceptReceive(it)
        }
    }

    fun rejectReceive() {
        currentTransferId?.let {
            Crocbridge.rejectReceive(it)
        }
    }

    fun startRelay(host: String, port: String, password: String): String {
        return Crocbridge.startRelay(host, port, password)
    }

    fun stopRelay(relayId: String) {
        Crocbridge.stopRelay(relayId)
    }

    fun resetState() {
        _transferState.value = TransferState.Idle
    }

    /**
     * Generate a croc-style mnemonic code (e.g. "1234-apple-banana-cherry")
     */
    fun generateCode(): String = Crocbridge.generateCode()
}

