package com.powercut.export

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Foreground service that keeps an export alive when the user backgrounds the
 * app. The actual render runs on ExportEngine's coroutine scope; this service
 * just holds a low-priority foreground notification so the OS doesn't kill us
 * mid-export (which previously left the native encoder in a locked state).
 */
class ExportService : Service() {
    companion object {
        const val NOTIF_ID = 4201
    }

    override fun onCreate() {
        super.onCreate()
        val notif = NotificationCompat.Builder(this, ExportEngine.NOTIF_CHANNEL_ID)
            .setContentTitle(getString(com.powercut.R.string.export_channel_name))
            .setContentText(getString(com.powercut.R.string.export_in_progress))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(NOTIF_ID, notif)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
