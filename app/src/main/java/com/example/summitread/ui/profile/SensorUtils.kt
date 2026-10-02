package com.example.summitread.ui.profile

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.atan2

/**
 * Device tilt in degrees (already damped for UI use). The values are snapshot state; read them
 * inside draw/graphicsLayer lambdas so tilting never recomposes the cards.
 */
@Stable
class TiltState {
    var pitch by mutableFloatStateOf(0f)
        internal set
    var roll by mutableFloatStateOf(0f)
        internal set
}

/**
 * ONE tilt sensor listener for the whole screen (#39/#45). It is registered only while the
 * lifecycle is RESUMED (ON_RESUME/ON_PAUSE) and always released when the composable leaves.
 * Updates are ignored until the tilt changed by [ProfileLogic.TILT_THRESHOLD_DEGREES].
 */
@Composable
fun rememberTiltState(): TiltState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val tilt = remember { TiltState() }

    DisposableEffect(context, lifecycleOwner) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sensorManager?.let {
            it.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) ?: it.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }
        if (sensorManager == null || sensor == null) {
            return@DisposableEffect onDispose { }
        }

        // Preallocated: no per-event allocations.
        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val newPitch: Float
                val newRoll: Float
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    // orientation: 0 = azimuth, 1 = pitch (X), 2 = roll (Y)
                    newPitch = Math.toDegrees(orientation[1].toDouble()).toFloat() * DAMPING
                    newRoll = Math.toDegrees(orientation[2].toDouble()).toFloat() * DAMPING
                } else {
                    // Accelerometer fallback for devices without a rotation vector sensor.
                    val x = event.values[0].toDouble()
                    val y = event.values[1].toDouble()
                    val z = event.values[2].toDouble()
                    newPitch = Math.toDegrees(atan2(y, z)).toFloat() * DAMPING
                    newRoll = -Math.toDegrees(atan2(x, z)).toFloat() * DAMPING
                }
                // getOrientation() uses asin(); near upright (pitch ≈ ±90°) float rounding can
                // yield NaN, and a NaN offset makes LinearGradient creation throw.
                if (!newPitch.isFinite() || !newRoll.isFinite()) return
                val pitch = newPitch.coerceIn(-MAX_TILT_DEGREES, MAX_TILT_DEGREES)
                val roll = newRoll.coerceIn(-MAX_TILT_DEGREES, MAX_TILT_DEGREES)
                if (ProfileLogic.exceedsThreshold(tilt.pitch, tilt.roll, pitch, roll)) {
                    tilt.pitch = pitch
                    tilt.roll = roll
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        var registered = false
        fun register() {
            if (!registered) {
                registered = sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            }
        }
        fun unregister() {
            if (registered) {
                sensorManager.unregisterListener(listener)
                registered = false
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> register()
                Lifecycle.Event.ON_PAUSE -> unregister()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            unregister()
        }
    }

    return tilt
}

private const val DAMPING = 0.5f
private const val MAX_TILT_DEGREES = 45f
