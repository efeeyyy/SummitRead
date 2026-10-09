package com.example.summitread.platform

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Device tilt source for the profile relic parallax.
 *
 * Reports raw angles in degrees in the Android convention (pitch about the X axis: 0 lying flat, -90 upright in
 * portrait; roll about the Y axis: 0 lying flat). Filtering (damping, clamping, NaN guard) is done by [TiltFilter].
 *
 * - Android: SensorManager rotation vector with accelerometer fallback (`AndroidTiltSensor`).
 * - iOS: CoreMotion device motion (`IosTiltSensor`, sign conventions unverified on a Mac).
 */
interface TiltSensor {

    /** Starts delivering tilt on the main thread; false when the device has no usable sensor. Idempotent. */
    fun start(onTilt: (pitchDegrees: Float, rollDegrees: Float) -> Unit): Boolean

    /** Stops delivering tilt and releases the sensor. Idempotent. */
    fun stop()
}

/** Pure post-processing of raw tilt angles (#39/#45). */
object TiltFilter {
    const val DAMPING = 0.5f
    const val MAX_TILT_DEGREES = 45f

    /**
     * Damped and clamped tilt, or null when an angle is not finite: near upright (pitch of about +-90 degrees)
     * float rounding can yield NaN, and a NaN offset makes gradient creation throw on Android.
     */
    fun filter(rawPitchDegrees: Float, rawRollDegrees: Float): Pair<Float, Float>? {
        val pitch = rawPitchDegrees * DAMPING
        val roll = rawRollDegrees * DAMPING
        if (!pitch.isFinite() || !roll.isFinite()) return null
        return pitch.coerceIn(-MAX_TILT_DEGREES, MAX_TILT_DEGREES) to roll.coerceIn(-MAX_TILT_DEGREES, MAX_TILT_DEGREES)
    }
}

/** The platform tilt sensor, provided by the host (`SummitReadApp`). Used by the profile relic parallax. */
val LocalTiltSensor = staticCompositionLocalOf<TiltSensor> {
    error("No TiltSensor provided: wrap the UI in SummitReadApp")
}
