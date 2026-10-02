package com.example.summitread.platform

import android.content.Context
import android.net.Uri
import com.example.summitread.data.ImageStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

/** Stores pictures in `context.filesDir/images/{prefix}_{uuid}.jpg` (unchanged behaviour of the old `ImageStorage`). */
class AndroidImageStorage(context: Context) : ImageStorage {

    private val appContext = context.applicationContext

    override suspend fun copyToStorage(sourceUri: String, prefix: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val imagesDir = File(appContext.filesDir, "images")
                if (!imagesDir.exists()) {
                    imagesDir.mkdirs()
                }

                val destFile = File(imagesDir, "${prefix}_${UUID.randomUUID()}.jpg")

                val copied = appContext.contentResolver.openInputStream(Uri.parse(sourceUri))?.use { inputStream ->
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

    override fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        try {
            File(path).delete()
        } catch (_: SecurityException) {
            // ignore: nothing useful to do if the file cannot be removed
        }
    }

    override suspend fun deleteAll(paths: Collection<String?>) {
        if (paths.isEmpty()) return
        withContext(Dispatchers.IO) { paths.forEach(::delete) }
    }
}
