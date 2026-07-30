package com.powercut.editor.core.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File

object UriHelper {

    private const val TAG = "UriHelper"

    /**
     * Get a usable file path from a content URI.
     * Strategy:
     * 1. If it's already a file:// URI, return the path directly
     * 2. Try to get the actual file path from content URI (some providers expose real paths)
     * 3. If all else fails, return the URI string itself (ExoPlayer can handle content:// URIs)
     *
     * IMPORTANT: We do NOT copy large videos to cache anymore.
     * ExoPlayer and FFmpeg-Kit can both handle content:// URIs directly.
     */
    fun getPathFromUri(context: Context, uri: Uri): String? {
        // File URI — return path directly
        if (uri.scheme == "file") {
            return uri.path
        }

        // Content URI — try to get real file path first
        val realPath = getRealPathFromUri(context, uri)
        if (realPath != null && File(realPath).exists()) {
            Log.d(TAG, "Got real file path: $realPath")
            return realPath
        }

        // For content:// URIs, return the URI string directly
        // ExoPlayer handles content:// URIs natively
        // FFmpeg-Kit can use content:// URIs via ContentResolver on newer versions
        Log.d(TAG, "Using content URI directly: $uri")
        return uri.toString()
    }

    /**
     * Try to resolve content URI to a real file path using common provider patterns.
     */
    private fun getRealPathFromUri(context: Context, uri: Uri): String? {
        try {
            // Method 1: Try ContentResolver query (works for MediaStore URIs)
            val projection = arrayOf(android.provider.MediaStore.Video.Media.DATA)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DATA)
                    val path = cursor.getString(columnIndex)
                    if (!path.isNullOrEmpty() && File(path).exists()) {
                        return path
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "ContentResolver query failed: ${e.message}")
        }

        try {
            // Method 2: Try openFileDescriptor to get the actual file path
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val fd = pfd.fd
                // On Linux/Android, /proc/self/fd/N is a symlink to the actual file
                val procPath = "/proc/self/fd/$fd"
                val link = File(procPath).canonicalPath
                if (link != null && !link.startsWith("/proc/") && File(link).exists()) {
                    return link
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "openFileDescriptor approach failed: ${e.message}")
        }

        return null
    }

    /**
     * Get the real duration of a video file in milliseconds using MediaMetadataRetriever.
     * Works with both file paths and content URIs.
     */
    fun getVideoDurationMs(context: Context, uriOrPath: String): Long? {
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            val uri = if (uriOrPath.startsWith("content://") || uriOrPath.startsWith("file://")) {
                Uri.parse(uriOrPath)
            } else {
                Uri.fromFile(File(uriOrPath))
            }
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(
                android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
            )
            retriever.release()
            durationStr?.toLongOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get video duration for: $uriOrPath", e)
            null
        }
    }
}
