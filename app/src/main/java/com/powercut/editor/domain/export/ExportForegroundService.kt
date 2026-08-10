package com.powercut.editor.domain.export

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.powercut.editor.MainActivity
import com.powercut.editor.R
import com.powercut.editor.core.base.Resource
import com.powercut.editor.data.VideoProject
import com.powercut.editor.data.ProjectRepository
import com.powercut.editor.domain.processing.VideoProcessor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ExportForegroundService — v4.2 Long-Export Reliability
 *
 * WHY THIS EXISTS
 * ---------------
 * On Android 8+ a background app is killed after ~60 seconds of CPU inactivity,
 * and on Android 9+ the OS will destroy any app that runs a long encode without
 * a foreground service. A 60-minute 1080p re-encode takes 15-40 minutes on a
 * mid-range phone, so without a foreground service + wake lock the export is
 * ALWAYS killed partway through → "Export failed" / corrupted file.
 *
 * WHAT THIS DOES
 * --------------
 *  1. Starts as a FOREGROUND service with a persistent notification. This raises
 *     the process priority to FOREGROUND so Android will not kill it for memory
 *     pressure and exempts it from the background execution limits.
 *  2. Acquires a PARTIAL_WAKE_LOCK so the CPU keeps running when the screen is
 *     off. Without this, Doze mode freezes FFmpeg mid-encode.
 *  3. Acquires a WIFI_LOCK so any network assets (background music, watermark)
 *     keep downloading.
 *  4. Runs the ExportManager.exportProject() inside its own SupervisorJob
 *     coroutine scope that is tied to the SERVICE lifetime, NOT the Activity /
 *     ViewModel lifetime. When the user minimises the app, the ViewModel is
 *     cleared but the service keeps running.
 *  5. Publishes export progress to a companion StateFlow that the UI observes,
 *     and updates the notification text as the job progresses.
 *  6. Stops itself (release locks, cancel notification) when the export
 *     completes or fails.
 *
 * ANDROID VERSION NOTES
 * ---------------------
 *  - startForeground() with FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING is required
 *    on Android 14 (API 34) and is a no-op on earlier versions.
 *  - The POST_NOTIFICATIONS permission is requested at runtime by the UI before
 *    starting the service (Android 13+).
 */
@AndroidEntryPoint
class ExportForegroundService : Service() {

    private val tag = "ExportFgService"

    @Inject
    lateinit var exportManager: ExportManager

    @Inject
    lateinit var videoProcessor: VideoProcessor

    @Inject
    lateinit var projectRepository: ProjectRepository

    private lateinit var wakeLock: PowerManager.WakeLock
    private var wifiLock: WifiManager.WifiLock? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var exportJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "ExportForegroundService created")

        // Acquire a PARTIAL wake lock — keeps CPU alive when screen is off.
        // timeout = 4 hours — long enough for a 2-hour 1080p encode with heavy
        // filters. The lock is released in onDestroy() regardless.
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "PowerCut::ExportWakeLock"
        )
        wakeLock.setReferenceCounted(false)
        try {
            wakeLock.acquire(4 * 60 * 60 * 1000L) // 4 hours max
            Log.d(tag, "PARTIAL_WAKE_LOCK acquired (4h timeout)")
        } catch (e: SecurityException) {
            Log.e(tag, "Could not acquire wake lock: ${e.message}")
        }

        // Acquire a Wi-Fi lock so network assets keep loading during the encode.
        try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiLock = wifiManager.createWifiLock(
                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                "PowerCut::ExportWifiLock"
            )
            wifiLock?.setReferenceCounted(false)
            wifiLock?.acquire()
            Log.d(tag, "WIFI_LOCK acquired")
        } catch (e: Exception) {
            Log.w(tag, "Could not acquire wifi lock: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(tag, "onStartCommand — starting foreground export")

        if (exportJob?.isActive == true) {
            Log.d(tag, "Export already running — ignoring duplicate start request")
            return START_NOT_STICKY
        }

        // Promote to foreground IMMEDIATELY (must happen within 5s on Android 12+).
        val notification = buildNotification("Preparing export…", 0)
        // CRASH FIX: Wrap startForeground() in a try/catch. On Android 12+,
        // startForeground() can throw ForegroundServiceStartNotAllowedException
        // if the app is in a restricted background state (even with the correct
        // permissions declared). Without this catch, the service crashes and
        // takes the app down with it. We also catch SecurityException for
        // missing-permission cases (belt-and-suspenders with the manifest fix).
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+: declare the foreground service type for long
                // video transcode jobs. On API 34+ use mediaProcessing plus dataSync.
                val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                }
                startForeground(
                    NOTIF_ID,
                    notification,
                    serviceType
                )
            } else {
                startForeground(NOTIF_ID, notification)
            }
        } catch (e: Exception) {
            // ForegroundServiceStartNotAllowedException (Android 12+) or
            // SecurityException (missing permission) — either way, we cannot
            // run as a foreground service. Fall back to running the export
            // in the background (no wake lock guarantee, but at least we
            // don't crash). Log the error so it's diagnosable.
            Log.e(tag, "startForeground() failed — running export without foreground priority: ${e.message}", e)
        }

        // Run the export in the service's own scope (survives Activity death).
        exportJob = serviceScope.launch {
            try {
                // Subscribe to export-state changes and mirror progress into the
                // notification so the user sees a live percentage bar.
                launch {
                    exportManager.progress.collect { pct ->
                        if (pct in 0..100) {
                            val msg = if (pct == 0) "Exporting video…" else "Exporting… $pct%"
                            updateNotification(msg, pct)
                        }
                    }
                }

                val project = projectRepository.currentProject.value
                if (project == null) {
                    Log.e(tag, "No current project to export — stopping service")
                    exportManager.publishError("No video project found. Please re-import your video and try again.")
                    stopSelf()
                    return@launch
                }
                Log.d(tag, "Starting export for project: ${project.videoPath.take(60)}…")
                exportManager.exportProject(project)
            } catch (e: Exception) {
                Log.e(tag, "Export job threw exception", e)
                exportManager.publishError(
                    e.message ?: "Export failed unexpectedly. The background service crashed."
                )
            } finally {
                Log.d(tag, "Export job finished — stopping service")
                stopSelf()
            }
        }

        // START_NOT_STICKY: if the system kills us, do NOT restart — a partial
        // output file would be corrupted. The user re-triggers the export.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(tag, "onDestroy — releasing locks and cancelling scope")
        stopForeground(STOP_FOREGROUND_REMOVE)
        exportJob?.cancel()
        serviceScope.cancel()
        try {
            if (wakeLock.isHeld) {
                wakeLock.release()
                Log.d(tag, "PARTIAL_WAKE_LOCK released")
            }
        } catch (e: Exception) {
            Log.w(tag, "Could not release wake lock: ${e.message}")
        }
        try {
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
                Log.d(tag, "WIFI_LOCK released")
            }
        } catch (e: Exception) {
            Log.w(tag, "Could not release wifi lock: ${e.message}")
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Notification helpers
    // ──────────────────────────────────────────────────────────────

    private fun buildNotification(text: String, progress: Int): Notification {
        ensureChannel()

        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.export_notif_title))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, progress == 0)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    private fun updateNotification(text: String, progress: Int) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(text, progress))
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.export_notif_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(R.string.export_notif_channel_desc)
                    setShowBadge(false)
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "powercut_export_channel"
        private const val NOTIF_ID = 4242

        /**
         * Convenience helper to start the export foreground service.
         */
        fun start(context: Context) {
            val intent = Intent(context, ExportForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Stop the service (used by the UI "Cancel" action if needed).
         */
        fun stop(context: Context) {
            context.stopService(Intent(context, ExportForegroundService::class.java))
        }
    }
}
