package com.henkenlink.crocdroid.domain.model

sealed interface TransferState {
    data object Idle : TransferState
    data object Loading : TransferState
    data class WaitingForRecipient(val code: String) : TransferState
    data class Transferring(val sentBytes: Long, val totalBytes: Long) : TransferState
    data object Success : TransferState
    data class Error(val message: String) : TransferState
    data class FileOffer(val fileName: String, val fileSize: Long, val fileCount: Int) : TransferState
}
