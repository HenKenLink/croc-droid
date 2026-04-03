package com.henkenlink.crocdroid.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.henkenlink.crocdroid.CrocDroidApp
import com.henkenlink.crocdroid.domain.model.TransferState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TransferService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    
    private var isForeground = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isForeground) {
            val notification = NotificationHelper.buildProgressNotification(
                this, "Croc Transfer", 0, "Initializing..."
            )
            startForeground(NOTIFICATION_ID, notification)
            isForeground = true
            observeTransferState()
        }
        return START_NOT_STICKY
    }

    private fun observeTransferState() {
        val app = application as CrocDroidApp
        val crocEngine = app.crocEngine

        serviceScope.launch {
            crocEngine.transferState.collectLatest { state ->
                when (state) {
                    is TransferState.Transferring -> {
                        val progress = if (state.totalBytes > 0) {
                            ((state.sentBytes.toDouble() / state.totalBytes.toDouble()) * 100).toInt()
                        } else 0
                        val content = "${formatSize(state.sentBytes)} / ${formatSize(state.totalBytes)}"
                        updateNotification("Transferring...", progress, content)
                    }
                    is TransferState.WaitingForRecipient -> {
                        updateNotification("Waiting for recipient...", 0, "Code: ${state.code}")
                    }
                    is TransferState.Loading -> {
                        updateNotification("Connecting...", 0, "Connecting to relay...")
                    }
                    is TransferState.Success -> {
                        showResultNotification("Transfer Complete", "Files transferred successfully", true)
                        stopService()
                    }
                    is TransferState.Error -> {
                        showResultNotification("Transfer Failed", state.message, false)
                        stopService()
                    }
                    is TransferState.Idle -> {
                        if (isForeground) stopService()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun updateNotification(title: String, progress: Int, content: String) {
        val notification = NotificationHelper.buildProgressNotification(this, title, progress, content)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showResultNotification(title: String, message: String, isSuccess: Boolean) {
        val notification = NotificationHelper.buildResultNotification(this, title, message, isSuccess)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(RESULT_NOTIFICATION_ID, notification)
    }

    private fun formatSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1) String.format("%.2f MB", mb) else String.format("%.1f KB", kb)
    }

    private fun stopService() {
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 101
        const val RESULT_NOTIFICATION_ID = 102
    }
}
