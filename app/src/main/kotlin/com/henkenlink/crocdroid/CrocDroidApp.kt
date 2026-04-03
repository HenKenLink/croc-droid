package com.henkenlink.crocdroid

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.henkenlink.crocdroid.data.croc.CrocEngine
import com.henkenlink.crocdroid.data.settings.SettingsRepository

class CrocDroidApp : Application() {

    // Singletons to be shared across Activity and Service
    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var crocEngine: CrocEngine
        private set

    override fun onCreate() {
        super.onCreate()
        
        settingsRepository = SettingsRepository(this)
        crocEngine = CrocEngine()

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "File Transfer Progress"
            val descriptionText = "Shows progress for ongoing croc file transfers"
            val importance = NotificationManager.IMPORTANCE_LOW 
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "transfer_channel"
    }
}
