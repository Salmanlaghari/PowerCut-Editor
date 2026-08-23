package com.powercut.editor.data

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.File
import java.security.MessageDigest

class DownloadManagerService(private val context: Context) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val logger = Log.getLogger("DownloadManager")
    private val downloadDir = File(context.filesDir, "downloads")

    init {
        downloadDir.mkdirs()
    }

    fun downloadItem(item: DownloadItem): Long {
        val request = DownloadManager.Request(Uri.parse(item.downloadUrl))
            .setTitle(item.name)
            .setDescription("Downloading $item.name (${item.sizeMb}MB)")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "PowerCut/${item.name}.zip"
            )
        return downloadManager.enqueue(request)
    }

    fun verifyDownload(file: File, expectedMd5: String): Boolean {
        val digest = MessageDigest.getInstance("MD5")
        val fileIs = FileInputStream(file)
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (fileIs.read(buffer) != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        fileIs.close()
        val md5 = digest.digest().joinToString("") { "%02x".format(it) }
        return md5.equals(expectedMd5, ignoreCase = true)
    }

    fun getDownloadStatus(downloadId: Long): Int {
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)
        cursor.moveToFirst()
        val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
        cursor.close()
        return status
    }
}

// Broadcast receiver to handle completed downloads
class DownloadCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            // Notify app of completed download for registration
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            // In real app: broadcast to activity or update local database
            Log.i("DownloadManager", "Download completed: $downloadId")
        }
    }
}