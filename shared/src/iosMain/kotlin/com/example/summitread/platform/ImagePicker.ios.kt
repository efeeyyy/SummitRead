package com.example.summitread.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

// UNVERIFIED on a Mac / device: compiled as a klib on Windows only (see docs/KMP-PLAN.md).

/**
 * iOS image picker based on `PHPickerViewController` (no photo-library permission needed). The picked photo is
 * copied to a temporary file inside the callback (the system deletes its own file when the callback returns); the
 * file URL string of that copy is reported and then copied into Documents/images by `IosImageStorage`.
 */
@Composable
actual fun rememberImagePicker(onPicked: (uri: String) -> Unit): ImagePicker {
    val latestOnPicked by rememberUpdatedState(onPicked)
    return remember { IosImagePicker { latestOnPicked(it) } }
}

@OptIn(ExperimentalForeignApi::class)
private class IosImagePicker(private val onPicked: (String) -> Unit) : ImagePicker {

    // UIKit keeps its delegate weakly: this object keeps it alive for as long as the picker is.
    private val delegate = object : NSObject(), PHPickerViewControllerDelegateProtocol {
        override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
            picker.dismissViewControllerAnimated(true, completion = null)
            val result = didFinishPicking.firstOrNull() as? PHPickerResult ?: return
            result.itemProvider.loadFileRepresentationForTypeIdentifier("public.image") { url, _ ->
                val copied = url?.let(::copyToTemporaryFile)
                if (copied != null) {
                    dispatch_async(dispatch_get_main_queue()) { onPicked(copied) }
                }
            }
        }
    }

    override fun pickImage() {
        val configuration = PHPickerConfiguration().apply {
            selectionLimit = 1
            filter = PHPickerFilter.imagesFilter
        }
        val picker = PHPickerViewController(configuration).also { it.delegate = delegate }
        topViewController()?.presentViewController(picker, animated = true, completion = null)
    }

    private fun copyToTemporaryFile(source: NSURL): String? {
        val destination = NSTemporaryDirectory() + "picked_${NSUUID().UUIDString}.jpg"
        val destinationUrl = NSURL.fileURLWithPath(destination)
        val ok = NSFileManager.defaultManager.copyItemAtURL(source, toURL = destinationUrl, error = null)
        return if (ok) destinationUrl.absoluteString else null
    }

    private fun topViewController(): UIViewController? {
        val scene = UIApplication.sharedApplication.connectedScenes.firstOrNull { it is UIWindowScene } as? UIWindowScene
        val window = scene?.windows?.firstOrNull { (it as? UIWindow)?.isKeyWindow() == true } as? UIWindow
        var top = window?.rootViewController
        while (top?.presentedViewController != null) top = top.presentedViewController
        return top
    }
}
