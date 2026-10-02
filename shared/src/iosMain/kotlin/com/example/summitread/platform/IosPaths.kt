package com.example.summitread.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

// NOTE: all iosMain code is UNVERIFIED on a Mac. It was written against the Kotlin/Native Apple platform bindings
// without being compiled (no Mac available in the Windows development environment). See docs/KMP-PLAN.md.

/** Absolute path of the app's Documents directory (database, preferences and pictures live here on iOS). */
@OptIn(ExperimentalForeignApi::class)
internal fun documentDirectoryPath(): String {
    val url = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null
    )
    return requireNotNull(url?.path) { "The Documents directory is not available" }
}
