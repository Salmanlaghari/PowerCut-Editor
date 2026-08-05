package com.powercut

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.powercut.export.ExportEngine

/**
 * PowerCut Pro 2027 8K — Application entry point.
 * Initializes the native export engine (loads `libpowercut_native.so`)
 * and creates the export notification channel once per process.
 */
class PowerCutApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        // One-time native init (caches jmethodID/jfieldID via JNI_OnLoad).
        ExportEngine.init(this)
        registerExportChannel()
    }

    private fun registerExportChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                ExportEngine.NOTIF_CHANNEL_ID,
                getString(R.string.export_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.export_channel_desc)
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(ch)
        }
    }

    companion object {
        @Volatile lateinit var instance: PowerCutApp
            private set
    }
}
