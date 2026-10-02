package com.example.summitread.platform

import com.example.summitread.data.ImageStorage
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUUID

/** Stores pictures in `Documents/images/{prefix}_{uuid}.jpg` (UNVERIFIED on a Mac). */
@OptIn(ExperimentalForeignApi::class)
class IosImageStorage : ImageStorage {

    private val fileManager = NSFileManager.defaultManager

    private fun imagesDirectory(): String {
        val dir = documentDirectoryPath() + "/images"
        if (!fileManager.fileExistsAtPath(dir)) {
            fileManager.createDirectoryAtPath(dir, withIntermediateDirectories = true, attributes = null, error = null)
        }
        return dir
    }

    /** [sourceUri] is a `file://` URL string or a plain file path. */
    override suspend fun copyToStorage(sourceUri: String, prefix: String): String? =
        withContext(Dispatchers.IO) {
            val source = if (sourceUri.startsWith("file:")) NSURL.URLWithString(sourceUri) else NSURL.fileURLWithPath(sourceUri)
            if (source == null) return@withContext null
            val destination = imagesDirectory() + "/${prefix}_${NSUUID().UUIDString}.jpg"
            val copied = fileManager.copyItemAtURL(source, toURL = NSURL.fileURLWithPath(destination), error = null)
            if (copied) destination else null
        }

    override fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        fileManager.removeItemAtPath(path, error = null)
    }

    override suspend fun deleteAll(paths: Collection<String?>) {
        if (paths.isEmpty()) return
        withContext(Dispatchers.IO) { paths.forEach(::delete) }
    }
}
