package com.aspirant.focus

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.aspirant.focus.data.local.AppDatabase
import com.aspirant.focus.data.local.PreferencesManager

class AspirantApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferencesManager: PreferencesManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getDatabase(this)
        preferencesManager = PreferencesManager(this)

        createNotificationChannels()
        com.aspirant.focus.service.BlockerProtectionService.start(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timerChannel = NotificationChannel(
                CHANNEL_FOCUS_TIMER,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }

            val guardChannel = NotificationChannel(
                CHANNEL_BLOCKER_GUARD,
                "24/7 Blocker Protection Shield",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Keeps adult content blocker and curfew shield alive 24/7 in background"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(timerChannel)
            notificationManager.createNotificationChannel(guardChannel)
        }
    }

    companion object {
        const val CHANNEL_FOCUS_TIMER = "channel_focus_timer"
        const val CHANNEL_BLOCKER_GUARD = "channel_blocker_guard"
        lateinit var instance: AspirantApplication
            private set
    }
}
