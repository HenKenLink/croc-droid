package com.henkenlink.crocdroid.domain.model

sealed interface TransferState {
    data object Idle : TransferState
    data object Loading : TransferState
    data class WaitingForRecipient(val code: String) : TransferState
    data class Transferring(
        val sentBytes: Long,
        val totalBytes: Long,
        val currentFileName: String = "",
        val currentFileIndex: Long = 0L,
        val totalFiles: Long = 0L,
    ) : TransferState
    data class Success(val receivedFiles: List<String> = emptyList()) : TransferState
    data class Error(val message: String) : TransferState
    data class FileOffer(val fileName: String, val fileSize: Long, val fileCount: Long) : TransferState
}
