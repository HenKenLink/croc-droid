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
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
private data class TransferConfig(
    val relayAddress: String,
    val relayPassword: String,
    val relayPorts: String,
    val curve: String,
    val hashAlgorithm: String,
    val disableLocal: Boolean,
    val forceLocal: Boolean,
    val multicastAddress: String,
    val disableMultiplexing: Boolean,
    val disableCompression: Boolean,
    val uploadThrottle: String,
    val overwrite: Boolean,
    val debugMode: Boolean,
    val isTempZip: Boolean = false,
    val noPromptReceive: Boolean = false,
    val peerIp: String = ""
)

class CrocEngine {

    private val _transferState = MutableStateFlow<TransferState>(TransferState.Idle)
    val transferState: StateFlow<TransferState> = _transferState.asStateFlow()

    private var currentTransferId: String? = null

    /**
     * Send multiple files or folders using croc
     */
    suspend fun sendFiles(
        filePaths: List<String>,
        code: String,
        settings: CrocSettings,
        isTempZip: Boolean = false
    ) {
        withContext(Dispatchers.IO) {
            _transferState.value = TransferState.Loading
            
            val config = TransferConfig(
                relayAddress = settings.relayAddress,
                relayPassword = settings.relayPassword,
                relayPorts = settings.relayPorts,
                curve = settings.curve,
                hashAlgorithm = settings.hashAlgorithm,
                disableLocal = settings.disableLocal,
                forceLocal = settings.forceLocal,
                multicastAddress = settings.multicastAddress,
                disableMultiplexing = settings.disableMultiplexing,
                disableCompression = settings.disableCompression,
                uploadThrottle = settings.uploadThrottle,
                overwrite = settings.overwrite,
                debugMode = settings.debugMode,
                isTempZip = isTempZip,
                noPromptReceive = settings.noPromptReceive,
                peerIp = settings.peerIp
            )
            
            val configJson = Json.encodeToString(config)
            val filePathsJson = Json.encodeToString(filePaths)
            val transferId = UUID.randomUUID().toString()
            currentTransferId = transferId

            Crocbridge.sendFiles(transferId, filePathsJson, code, configJson, object : CrocCallback {
                override fun onReady(p0: String?) {
                    val actualCode = p0 ?: code
                    _transferState.value = TransferState.WaitingForRecipient(actualCode)
                }

                override fun onFileOffer(p0: String?, p1: Long, p2: Long): Boolean {
                    return true
                }

                override fun onProgress(sent: Long, total: Long) {}

                override fun onFileProgress(
                    fileName: String?,
                    currentNum: Long,
                    totalFiles: Long,
                    sent: Long,
                    total: Long
                ) {
                    _transferState.value = TransferState.Transferring(
                        sentBytes = sent,
                        totalBytes = total,
                        currentFileName = fileName ?: "",
                        currentFileIndex = currentNum,
                        totalFiles = totalFiles
                    )
                }

                override fun onSuccess() {
                    _transferState.value = TransferState.Success()
                    currentTransferId = null
                }

                override fun onSuccessWithFiles(fileListJson: String?) {
                    val files = try {
                        fileListJson?.let { Json.decodeFromString<List<String>>(it) } ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                    _transferState.value = TransferState.Success(files)
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
            
            val config = TransferConfig(
                relayAddress = settings.relayAddress,
                relayPassword = settings.relayPassword,
                relayPorts = settings.relayPorts,
                curve = settings.curve,
                hashAlgorithm = settings.hashAlgorithm,
                disableLocal = settings.disableLocal,
                forceLocal = settings.forceLocal,
                multicastAddress = settings.multicastAddress,
                disableMultiplexing = settings.disableMultiplexing,
                disableCompression = settings.disableCompression,
                uploadThrottle = settings.uploadThrottle,
                overwrite = settings.overwrite,
                debugMode = settings.debugMode,
                isTempZip = false,
                noPromptReceive = settings.noPromptReceive,
                peerIp = settings.peerIp
            )
            
            val configJson = Json.encodeToString(config)
            val transferId = UUID.randomUUID().toString()
            currentTransferId = transferId

            Crocbridge.receiveFile(transferId, code, saveDir, configJson, object : CrocCallback {
                override fun onReady(p0: String?) {
                    _transferState.value = TransferState.Loading
                }

                override fun onFileOffer(p0: String?, p1: Long, p2: Long): Boolean {
                    _transferState.value = TransferState.FileOffer(
                        fileName = p0 ?: "Unknown",
                        fileSize = p1,
                        fileCount = p2
                    )
                    return true // Callback handled
                }

                override fun onProgress(sent: Long, total: Long) {
                    // We prefer onFileProgress but keep this for basic updates
                }

                override fun onFileProgress(
                    fileName: String?,
                    currentNum: Long,
                    totalFiles: Long,
                    sent: Long,
                    total: Long
                ) {
                    _transferState.value = TransferState.Transferring(
                        sentBytes = sent,
                        totalBytes = total,
                        currentFileName = fileName ?: "",
                        currentFileIndex = currentNum,
                        totalFiles = totalFiles
                    )
                }

                override fun onSuccess() {
                    _transferState.value = TransferState.Success()
                    currentTransferId = null
                }

                override fun onSuccessWithFiles(fileListJson: String?) {
                    val files = try {
                        fileListJson?.let { Json.decodeFromString<List<String>>(it) } ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                    _transferState.value = TransferState.Success(files)
                    currentTransferId = null
                }

                override fun onError(errStr: String?) {
                    _transferState.value = TransferState.Error(errStr ?: "Unknown error occurred")
                    currentTransferId = null
                }
            })
        }
    }

    fun getDebugLog(): String = Crocbridge.getDebugLog()
    fun clearDebugLog() = Crocbridge.clearDebugLog()

    fun cancelTransfer() {
        currentTransferId?.let {
            Crocbridge.cancelTransfer(it)
            _transferState.value = TransferState.Idle
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

