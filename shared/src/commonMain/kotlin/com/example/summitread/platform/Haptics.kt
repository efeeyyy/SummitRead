package com.example.summitread.platform

/**
 * Haptic feedback vocabulary of the app.
 *
 * - Android: Compose `HapticFeedback` (`AppHaptics` in :app).
 * - iOS: UIKit feedback generators (`IosHaptics`, unverified on a Mac); Compose Multiplatform's own haptics can
 *   replace it in phase B if they cover the same types.
 */
interface Haptics {
    /** Light impact: selection changes, tab switches, segmented control. */
    fun light()

    /** Toggle switched on/off. */
    fun toggle(on: Boolean)

    /** Pages logged, session saved. */
    fun logged()

    /** Summit reached. */
    fun success()

    /** Destructive or rejected action. */
    fun warning()
}
