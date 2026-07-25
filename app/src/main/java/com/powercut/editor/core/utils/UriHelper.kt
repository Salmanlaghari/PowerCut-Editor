package com.powercut.editor.core.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object UriHelper {
    /**
     * Copies the content of the given Uri to a temporary file in the app cache directory.
     * This ensures we have a standard local file path for FFmpeg and GPUImage processing.
     */
    fun getPathFromUri(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") {
            return uri.path
        }

        try {
            val contentResolver = context.contentResolver
            val fileName = "temp_video_${System.currentTimeMillis()}.mp4"
            val tempFile = File(context.cacheDir, fileName)

            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    val buffer = ByteArray(4 * 1024)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                    }
                    outputStream.flush()
                }
            }
            return tempFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
