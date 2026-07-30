package com.powercut.editor.core.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object UriHelper {

    private const val TAG = "UriHelper"

    /**
     * Copies the content of the given Uri to a temporary file in the app cache directory.
     * Uses a large 1MB buffer for fast copying of large video files (4K/8K, long duration).
     * Validates file size and cleans up on failure.
     */
    fun getPathFromUri(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") {
            return uri.path
        }

        var tempFile: File? = null
        try {
            val contentResolver = context.contentResolver

            // Check available cache space before copying
            val cacheDir = context.cacheDir
            val availableSpace = cacheDir.freeSpace
            Log.d(TAG, "Cache dir free space: ${availableSpace / (1024 * 1024)} MB")

            // Get content length if available to pre-check space
            try {
                contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                    val fileSize = afd.length
                    if (fileSize > 0) {
                        Log.d(TAG, "Video file size: ${fileSize / (1024 * 1024)} MB")
                        if (fileSize > availableSpace * 0.9) {
                            // Not enough cache space — try external cache
                            val externalCache = context.externalCacheDir
                            if (externalCache != null && externalCache.freeSpace > fileSize) {
                                Log.d(TAG, "Using external cache dir for large file")
                                tempFile = File(externalCache, "temp_video_${System.currentTimeMillis()}.mp4")
                            } else {
                                Log.e(TAG, "Not enough space for video: need ${fileSize / (1024*1024)}MB, have ${availableSpace / (1024*1024)}MB")
                                // Still try — maybe content length was wrong
                                tempFile = File(cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")
                            }
                        } else {
                            tempFile = File(cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")
                        }
                    } else {
                        tempFile = File(cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")
                    }
                } ?: run {
                    tempFile = File(cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")
                }
            } catch (e: Exception) {
                Log.d(TAG, "Could not get file size, proceeding with copy: ${e.message}")
                tempFile = File(cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")
            }

            if (tempFile == null) {
                tempFile = File(cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")
            }

            // Copy with large 1MB buffer for fast throughput on big files
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    val buffer = ByteArray(1024 * 1024) // 1MB buffer — much faster for large videos
                    var read: Int
                    var totalBytes = 0L
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                        totalBytes += read
                    }
                    outputStream.flush()
                    Log.d(TAG, "Copied ${totalBytes / (1024 * 1024)} MB to temp file")
                }
            } ?: run {
                Log.e(TAG, "Failed to open input stream for uri: $uri")
                tempFile?.delete()
                return null
            }

            // Validate the copied file
            if (tempFile.exists() && tempFile.length() > 0) {
                Log.d(TAG, "Video imported successfully: ${tempFile.absolutePath} (${tempFile.length() / (1024*1024)} MB)")
                return tempFile.absolutePath
            } else {
                Log.e(TAG, "Copied file is empty or missing")
                tempFile?.delete()
                return null
            }
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "OOM during video copy", e)
            tempFile?.delete()
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy video from uri", e)
            tempFile?.delete()
            return null
        }
    }

    /**
     * Get the real duration of a video file in milliseconds using MediaMetadataRetriever.
     * Returns null if duration cannot be determined.
     */
    fun getVideoDurationMs(context: Context, uri: Uri): Long? {
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(
                android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
            )
            retriever.release()
            durationStr?.toLongOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get video duration", e)
            null
        }
    }
}
