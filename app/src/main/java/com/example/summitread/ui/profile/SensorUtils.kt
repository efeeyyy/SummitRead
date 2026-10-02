package com.example.summitread.ui.profile

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
import com.example.summitread.platform.AndroidTiltSensor
import com.example.summitread.platform.TiltFilter
import com.example.summitread.platform.TiltSensor

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
 *
 * The sensor itself is the platform [TiltSensor] (SensorManager on Android); damping, clamping and the NaN
 * guard are the shared [TiltFilter].
 */
@Composable
fun rememberTiltState(): TiltState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val tilt = remember { TiltState() }

    DisposableEffect(context, lifecycleOwner) {
        val sensor: TiltSensor = AndroidTiltSensor(context)

        fun register() {
            sensor.start { rawPitch, rawRoll ->
                val (pitch, roll) = TiltFilter.filter(rawPitch, rawRoll) ?: return@start
                if (ProfileLogic.exceedsThreshold(tilt.pitch, tilt.roll, pitch, roll)) {
                    tilt.pitch = pitch
                    tilt.roll = roll
                }
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> register()
                Lifecycle.Event.ON_PAUSE -> sensor.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sensor.stop()
        }
    }

    return tilt
}
