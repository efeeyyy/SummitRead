package com.example.summitread.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import com.example.summitread.data.ThemeMode

data class AppThemeState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AccentColor = AccentColor.FOREST_GREEN
)

val LocalAppThemeState = compositionLocalOf { AppThemeState() }

@Composable
fun SummitReadTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    accentColor: AccentColor = AccentColor.FOREST_GREEN,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = accentColor.darkPrimary,
            secondary = accentColor.darkSecondary,
            primaryContainer = accentColor.darkPrimaryContainer,
            onPrimary = accentColor.darkOnPrimary,
            background = DarkBackground,
            surface = DarkSurface,
            surfaceVariant = DarkSurfaceVariant,
            onBackground = TextOnDark,
            onSurface = TextOnDark,
            onSurfaceVariant = TextSecondaryOnDark
        )
    } else {
        lightColorScheme(
            primary = accentColor.lightPrimary,
            secondary = accentColor.lightSecondary,
            primaryContainer = accentColor.lightPrimaryContainer,
            onPrimary = accentColor.lightOnPrimary,
            background = LightBackground,
            surface = LightSurface,
            surfaceVariant = LightSurfaceVariant,
            onBackground = TextPrimary,
            onSurface = TextPrimary,
            onSurfaceVariant = TextSecondary
        )
    }

    val appThemeState = AppThemeState(themeMode = themeMode, accentColor = accentColor)

    CompositionLocalProvider(LocalAppThemeState provides appThemeState) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SummitReadTypography,
            content = content
        )
    }
}
