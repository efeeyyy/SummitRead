package com.example.summitread.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

object ImageStorage {

    /**
     * Copies an image from [sourceUri] to `context.filesDir/images/{prefix}_{uuid}.jpg` on [Dispatchers.IO].
     *
     * @param prefix A descriptive prefix for the saved file (e.g., "cover", "note").
     * @return The absolute file path of the saved image, or null if the operation failed.
     */
    suspend fun copyImageToInternalStorage(context: Context, sourceUri: Uri, prefix: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val imagesDir = File(context.filesDir, "images")
                if (!imagesDir.exists()) {
                    imagesDir.mkdirs()
                }

                val destFile = File(imagesDir, "${prefix}_${UUID.randomUUID()}.jpg")

                val copied = context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    destFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    true
                } ?: false

                if (copied) destFile.absolutePath else null
            } catch (_: IOException) {
                null
            } catch (_: SecurityException) {
                null
            }
        }

    /** Deletes a stored image file (no-op for null/blank/missing paths). Safe to call from any thread. */
    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        try {
            File(path).delete()
        } catch (_: SecurityException) {
            // ignore: nothing useful to do if the file cannot be removed
        }
    }

    /** Deletes several stored images on [Dispatchers.IO]. */
    suspend fun deleteAll(paths: Collection<String?>) {
        if (paths.isEmpty()) return
        withContext(Dispatchers.IO) { paths.forEach(::delete) }
    }
}
