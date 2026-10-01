package com.example.summitread.ui.mountain

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * True when the app's own theme (not the system setting) is dark. The theme mode can be forced by the user,
 * so [androidx.compose.foundation.isSystemInDarkTheme] would be wrong here (#14, #15).
 */
@Composable
fun isAppDarkTheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/** High-contrast text/border colours for the Mountain screen cards in both themes (#15). */
data class MountainContrast(
    val primaryText: Color,
    val secondaryText: Color,
    val border: Color,
    val isDark: Boolean
)

@Composable
fun mountainContrast(): MountainContrast {
    val dark = isAppDarkTheme()
    val scheme = MaterialTheme.colorScheme
    return if (dark) {
        MountainContrast(
            primaryText = Color.White,
            secondaryText = Color(0xFFE0E0E0),
            border = Color.White.copy(alpha = 0.55f),
            isDark = true
        )
    } else {
        MountainContrast(
            primaryText = scheme.onSurface,
            secondaryText = scheme.onSurfaceVariant,
            border = scheme.outline,
            isDark = false
        )
    }
}
