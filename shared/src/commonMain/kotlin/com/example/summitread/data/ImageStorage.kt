package com.example.summitread.data

/**
 * Stores user pictures (book covers, note photos) in app-private storage.
 * Android: files under `filesDir/images`; iOS: files under Documents/images.
 * Stored paths are absolute file paths, saved in the database as before.
 */
interface ImageStorage {

    /**
     * Copies the picture at [sourceUri] (Android content/file URI string, iOS file URL string) into app storage
     * as `{prefix}_{uuid}.jpg`, off the main thread.
     *
     * @param prefix A descriptive prefix for the saved file (e.g., "cover", "note").
     * @return The absolute file path of the saved image, or null if the operation failed.
     */
    suspend fun copyToStorage(sourceUri: String, prefix: String): String?

    /** Deletes a stored image file (no-op for null/blank/missing paths). Safe to call from any thread. */
    fun delete(path: String?)

    /** Deletes several stored images off the main thread. */
    suspend fun deleteAll(paths: Collection<String?>)
}
