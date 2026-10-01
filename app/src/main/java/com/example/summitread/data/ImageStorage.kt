package com.example.summitread.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException

object ImageStorage {

    /**
     * Copies an image from the given [sourceUri] to internal storage under
     * `context.filesDir/images/{prefix}_{timestamp}.jpg`.
     *
     * @param context Application context.
     * @param sourceUri The content URI of the source image.
     * @param prefix A descriptive prefix for the saved file (e.g., "cover", "note").
     * @return The absolute file path of the saved image, or null if the operation failed.
     */
    fun copyImageToInternalStorage(context: Context, sourceUri: Uri, prefix: String): String? {
        return try {
            val imagesDir = File(context.filesDir, "images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
            val destFile = File(imagesDir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                destFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return null

            destFile.absolutePath
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }
}
