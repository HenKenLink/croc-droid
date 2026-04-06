package com.henkenlink.crocdroid.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class TransferType {
    SEND, RECEIVE
}

@Serializable
data class HistoryEntry(
    val id: String,
    val type: TransferType,
    val timestamp: Long,
    val fileName: String,
    val fileSize: Long,
    val fileCount: Long,
    val success: Boolean,
    val errorMessage: String? = null
)

@Serializable
data class ReceiveHistoryEntry(
    val id: String,
    val timestamp: Long,
    val fileName: String,
    val filePaths: List<String>,
    val fileSize: Long,
    val fileCount: Long
)
