package com.example.summitread.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** Shared animation specs. iOS-like: slightly under-damped springs, medium-low stiffness. */
object AppMotion {
    /** Default for layout/state changes (sheets, indicators, progress). */
    fun <T> standard(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)

    /** Quick, a little bouncier: segmented pill, selection changes. */
    fun <T> snappy(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.82f, stiffness = 520f)

    /** Slow and calm: big hero movements, progress rings filling. */
    fun <T> gentle(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessLow)

    /** Press feedback: fast and critically-damped-ish so release never wobbles. */
    fun <T> press(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.8f, stiffness = 900f)

    /** Short fades (glass bar background, tooltips). */
    fun <T> fade(): FiniteAnimationSpec<T> = tween(durationMillis = 180)

    const val PRESSED_SCALE = 0.97f
}

/**
 * Haptics facade. Everything goes through Compose's [LocalHapticFeedback], which exists on every
 * Compose platform; swap the mapping here if iOS needs UIImpactFeedbackGenerator.
 */
@Immutable
class AppHaptics internal constructor(private val feedback: androidx.compose.ui.hapticfeedback.HapticFeedback) {
    /** Light impact: selection changes, tab switches, segmented control. */
    fun light() = feedback.performHapticFeedback(HapticFeedbackType.SegmentTick)

    /** Toggle switched on/off. */
    fun toggle(on: Boolean) =
        feedback.performHapticFeedback(if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)

    /** Pages logged, session saved. */
    fun logged() = feedback.performHapticFeedback(HapticFeedbackType.Confirm)

    /** Summit reached. */
    fun success() = feedback.performHapticFeedback(HapticFeedbackType.Confirm)

    /** Destructive or rejected action. */
    fun warning() = feedback.performHapticFeedback(HapticFeedbackType.Reject)
}

@Composable
fun rememberAppHaptics(): AppHaptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) { AppHaptics(feedback) }
}
