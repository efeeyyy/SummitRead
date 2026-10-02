package com.example.summitread.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue
import kotlin.math.PI

/**
 * CoreMotion device motion (UNVERIFIED on a Mac). [TiltSensor] reports angles in the Android convention:
 * CoreMotion's attitude pitch is positive when the top edge tips up (upright portrait = +90), Android's is
 * negative there, so the pitch is negated; roll has the same sign on both. The signs still need a check on a device.
 */
@OptIn(ExperimentalForeignApi::class)
class IosTiltSensor : TiltSensor {

    private val motionManager = CMMotionManager()
    private var running = false

    override fun start(onTilt: (pitchDegrees: Float, rollDegrees: Float) -> Unit): Boolean {
        if (!motionManager.deviceMotionAvailable) return false
        if (running) motionManager.stopDeviceMotionUpdates()
        motionManager.deviceMotionUpdateInterval = UPDATE_INTERVAL_SECONDS
        motionManager.startDeviceMotionUpdatesToQueue(NSOperationQueue.mainQueue) { motion, _ ->
            val attitude = motion?.attitude ?: return@startDeviceMotionUpdatesToQueue
            onTilt(
                (-attitude.pitch * RADIANS_TO_DEGREES).toFloat(),
                (attitude.roll * RADIANS_TO_DEGREES).toFloat()
            )
        }
        running = true
        return true
    }

    override fun stop() {
        if (running) {
            motionManager.stopDeviceMotionUpdates()
            running = false
        }
    }

    private companion object {
        const val UPDATE_INTERVAL_SECONDS = 1.0 / 30.0
        const val RADIANS_TO_DEGREES = 180.0 / PI
    }
}
