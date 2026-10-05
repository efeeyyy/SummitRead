package com.example.summitread.platform

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

@Composable
actual fun StatusBarIcons(light: Boolean?, reapplyKey: Any?) {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val original = controller?.isAppearanceLightStatusBars
        onDispose {
            if (controller != null && original != null) controller.isAppearanceLightStatusBars = original
        }
    }
    DisposableEffect(view, light, reapplyKey) {
        if (light != null) {
            val window = view.context.findActivity()?.window
            if (window != null) WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = light
        }
        onDispose { }
    }
}
