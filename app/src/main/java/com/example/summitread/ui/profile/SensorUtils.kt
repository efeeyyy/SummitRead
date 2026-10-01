package com.example.summitread.ui.profile

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberGyroscopeSensor(): Pair<Float, Float> {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val rotationState = remember { mutableStateOf(Pair(0f, 0f)) }

    DisposableEffect(sensorManager) {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) 
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                        val rotationMatrix = FloatArray(9)
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, it.values)
                        val orientationAngles = FloatArray(3)
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)
                        // orientationAngles: 0 = azimuth (Z), 1 = pitch (X), 2 = roll (Y)
                        val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                        val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
                        
                        // Dampen the values a bit for UI
                        val newPitch = pitch * 0.5f
                        val newRoll = roll * 0.5f
                        val current = rotationState.value
                        if (kotlin.math.abs(newPitch - current.first) > 0.5f || kotlin.math.abs(newRoll - current.second) > 0.5f) {
                            rotationState.value = Pair(newPitch, newRoll)
                        }
                    } else if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        // Fallback for devices without rotation vector
                        val x = it.values[0]
                        val y = it.values[1]
                        val z = it.values[2]
                        val roll = Math.toDegrees(Math.atan2(x.toDouble(), z.toDouble())).toFloat()
                        val pitch = Math.toDegrees(Math.atan2(y.toDouble(), z.toDouble())).toFloat()
                        
                        val newPitch = pitch * 0.5f
                        val newRoll = -roll * 0.5f
                        val current = rotationState.value
                        if (kotlin.math.abs(newPitch - current.first) > 0.5f || kotlin.math.abs(newRoll - current.second) > 0.5f) {
                            rotationState.value = Pair(newPitch, newRoll)
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    return rotationState.value
}
