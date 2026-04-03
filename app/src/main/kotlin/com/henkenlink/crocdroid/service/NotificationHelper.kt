package com.henkenlink.crocdroid.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.henkenlink.crocdroid.CrocDroidApp
import com.henkenlink.crocdroid.MainActivity
import com.henkenlink.crocdroid.R

object NotificationHelper {

    private const val PROGRESS_MAX = 100

    fun buildProgressNotification(
        context: Context,
        title: String,
        progress: Int,
        contentText: String
    ): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CrocDroidApp.CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download) // Standard icon
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(PROGRESS_MAX, progress, progress == 0 && contentText.contains("Connecting"))
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    fun buildResultNotification(
        context: Context,
        title: String,
        message: String,
        isSuccess: Boolean
    ): Notification {
        return NotificationCompat.Builder(context, CrocDroidApp.CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(if (isSuccess) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .build()
    }
}
