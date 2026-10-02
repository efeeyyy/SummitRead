package com.example.summitread.platform

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.atan2

/**
 * SensorManager based tilt: the rotation vector sensor, or the accelerometer on devices without one.
 * Raw degrees only; damping/clamping is [TiltFilter]'s job. Not thread-safe: use from the main thread.
 */
class AndroidTiltSensor(context: Context) : TiltSensor {

    private val sensorManager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val sensor: Sensor? = sensorManager?.let {
        it.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) ?: it.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    // Preallocated: no per-event allocations.
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    private var callback: ((Float, Float) -> Unit)? = null
    private var registered = false

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val onTilt = callback ?: return
            val pitch: Float
            val roll: Float
            if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)
                // orientation: 0 = azimuth, 1 = pitch (X), 2 = roll (Y)
                pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
            } else {
                // Accelerometer fallback for devices without a rotation vector sensor.
                val x = event.values[0].toDouble()
                val y = event.values[1].toDouble()
                val z = event.values[2].toDouble()
                pitch = Math.toDegrees(atan2(y, z)).toFloat()
                roll = -Math.toDegrees(atan2(x, z)).toFloat()
            }
            onTilt(pitch, roll)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    override fun start(onTilt: (pitchDegrees: Float, rollDegrees: Float) -> Unit): Boolean {
        val manager = sensorManager ?: return false
        val tiltSensor = sensor ?: return false
        callback = onTilt
        if (!registered) {
            registered = manager.registerListener(listener, tiltSensor, SensorManager.SENSOR_DELAY_UI)
        }
        return registered
    }

    override fun stop() {
        if (registered) {
            sensorManager?.unregisterListener(listener)
            registered = false
        }
        callback = null
    }
}
