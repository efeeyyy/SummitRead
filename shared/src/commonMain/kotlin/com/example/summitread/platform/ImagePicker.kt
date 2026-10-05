package com.example.summitread.platform

import androidx.compose.runtime.Composable

/**
 * Opens the system photo picker so the user can choose ONE image.
 *
 * The picked image is reported as a platform URI string, exactly what [com.example.summitread.data.ImageStorage]
 * `copyToStorage` accepts: an Android content URI, or an iOS file URL (a temporary copy of the picked photo).
 * The caller copies it into app storage right away. Cancelling the picker reports nothing.
 */
interface ImagePicker {
    /** Shows the picker. Safe to call repeatedly; the result arrives through the callback given to [rememberImagePicker]. */
    fun pickImage()
}

/**
 * Remembers an [ImagePicker] bound to the current screen. [onPicked] is called on the main thread with the URI string
 * of the chosen image (never for a cancelled picker).
 *
 * Android: `ActivityResultContracts.PickVisualMedia` (no permission needed). iOS: `PHPickerViewController`
 * (UNVERIFIED, compiled only on Windows).
 */
@Composable
expect fun rememberImagePicker(onPicked: (uri: String) -> Unit): ImagePicker
