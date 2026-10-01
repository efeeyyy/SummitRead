package com.example.summitread.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.example.summitread.data.ThemeMode

data class AppThemeState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AccentColor = AccentColor.FOREST_GREEN
)

val LocalAppThemeState = compositionLocalOf { AppThemeState() }

/** Whether the app should render dark for [themeMode], given the current system setting. */
fun resolveIsDark(themeMode: ThemeMode, systemDark: Boolean): Boolean = when (themeMode) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> systemDark
}

@Composable
fun SummitReadTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentColor: AccentColor = AccentColor.FOREST_GREEN,
    content: @Composable () -> Unit
) {
    val isDark = resolveIsDark(themeMode, isSystemInDarkTheme())
    val colorScheme = summitColorScheme(isDark, accentColor)
    val appThemeState = AppThemeState(themeMode = themeMode, accentColor = accentColor)

    CompositionLocalProvider(LocalAppThemeState provides appThemeState) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SummitReadTypography,
            content = content
        )
    }
}

/**
 * Full colour scheme derived from the accent colour, so no role falls back to the default
 * M3 purple (secondaryContainer, tertiary, outline, surfaceContainer*, ...).
 */
fun summitColorScheme(isDark: Boolean, accent: AccentColor): ColorScheme =
    if (isDark) {
        val primary = accent.darkPrimary
        val secondary = lerp(primary, TextOnDark, 0.2f) // readable on the navy surfaces
        val secondaryContainer = lerp(accent.darkPrimaryContainer, DarkSurface, 0.35f)
        darkColorScheme(
            primary = primary,
            onPrimary = accent.darkOnPrimary,
            primaryContainer = accent.darkPrimaryContainer,
            onPrimaryContainer = lerp(primary, Color.White, 0.55f),
            secondary = secondary,
            onSecondary = accent.darkOnPrimary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = lerp(primary, Color.White, 0.6f),
            tertiary = accent.darkSecondary,
            onTertiary = Color.White,
            tertiaryContainer = lerp(accent.darkSecondary, DarkSurface, 0.6f),
            onTertiaryContainer = lerp(primary, Color.White, 0.6f),
            background = DarkBackground,
            onBackground = TextOnDark,
            surface = DarkSurface,
            onSurface = TextOnDark,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = TextSecondaryOnDark,
            surfaceTint = primary,
            inverseSurface = TextOnDark,
            inverseOnSurface = DarkBackground,
            inversePrimary = accent.lightPrimary,
            outline = Color(0xFF8A8AA8),
            outlineVariant = Color(0xFF3F3F63),
            surfaceBright = Color(0xFF34345C),
            surfaceDim = DarkBackground,
            surfaceContainerLowest = Color(0xFF14142A),
            surfaceContainerLow = Color(0xFF1F1F3A),
            surfaceContainer = DarkSurface,
            surfaceContainerHigh = Color(0xFF2B2B4D),
            surfaceContainerHighest = DarkSurfaceVariant
        )
    } else {
        val primary = accent.lightPrimary
        lightColorScheme(
            primary = primary,
            onPrimary = accent.lightOnPrimary,
            primaryContainer = accent.lightPrimaryContainer,
            onPrimaryContainer = lerp(primary, Color.Black, 0.55f),
            secondary = accent.lightSecondary,
            onSecondary = Color.White,
            secondaryContainer = lerp(accent.lightPrimaryContainer, LightSurface, 0.45f),
            onSecondaryContainer = lerp(primary, Color.Black, 0.5f),
            tertiary = lerp(primary, accent.lightSecondary, 0.5f),
            onTertiary = Color.White,
            tertiaryContainer = lerp(accent.lightPrimaryContainer, LightSurface, 0.7f),
            onTertiaryContainer = lerp(primary, Color.Black, 0.5f),
            background = LightBackground,
            onBackground = TextPrimary,
            surface = LightSurface,
            onSurface = TextPrimary,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = TextSecondary,
            surfaceTint = primary,
            inverseSurface = Color(0xFF2C2C2C),
            inverseOnSurface = LightBackground,
            inversePrimary = accent.darkPrimary,
            outline = Color(0xFF8A8680),
            outlineVariant = Color(0xFFD8D4CC),
            surfaceBright = LightSurface,
            surfaceDim = Color(0xFFE6E2DA),
            surfaceContainerLowest = LightSurface,
            surfaceContainerLow = Color(0xFFF9F7F3),
            surfaceContainer = Color(0xFFF3F0EA),
            surfaceContainerHigh = LightSurfaceVariant,
            surfaceContainerHighest = Color(0xFFEAE6DF)
        )
    }
