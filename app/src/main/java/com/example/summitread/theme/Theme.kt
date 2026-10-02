package com.example.summitread.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
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

/** Entry point for design-system tokens: `AppTheme.colors.label`, `AppTheme.isDark`. */
object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current

    val isDark: Boolean
        @Composable @ReadOnlyComposable get() = LocalAppColors.current.isDark
}

@Composable
fun SummitReadTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentColor: AccentColor = AccentColor.FOREST_GREEN,
    content: @Composable () -> Unit
) {
    val isDark = resolveIsDark(themeMode, isSystemInDarkTheme())
    val colors = remember(isDark, accentColor) { appColors(isDark, accentColor) }
    val colorScheme = remember(isDark, accentColor) { summitColorScheme(isDark, accentColor) }
    val appThemeState = remember(themeMode, accentColor) { AppThemeState(themeMode, accentColor) }

    CompositionLocalProvider(
        LocalAppThemeState provides appThemeState,
        LocalAppColors provides colors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SummitReadTypography,
            shapes = SummitReadShapes,
            content = content
        )
    }
}

/** WCAG contrast ratio between two opaque colours (1..21). */
fun contrastRatio(a: Color, b: Color): Double {
    val la = a.luminance().toDouble()
    val lb = b.luminance().toDouble()
    val hi = maxOf(la, lb)
    val lo = minOf(la, lb)
    return (hi + 0.05) / (lo + 0.05)
}

/** White or near-black, whichever reads better on [background]. */
fun readableOn(background: Color): Color {
    val dark = Color(0xFF111111)
    return if (contrastRatio(background, Color.White) >= contrastRatio(background, dark)) Color.White else dark
}

/**
 * Material colour scheme built from the iOS palette ([appColors]) and the accent, so every
 * role (secondaryContainer, tertiary, outline, surfaceContainer*, ...) is defined and existing
 * M3 screens pick up the new look.
 */
fun summitColorScheme(isDark: Boolean, accent: AccentColor): ColorScheme {
    val c = appColors(isDark, accent)
    return if (isDark) {
        val primary = accent.darkPrimary
        darkColorScheme(
            primary = primary,
            onPrimary = accent.darkOnPrimary,
            primaryContainer = accent.darkPrimaryContainer,
            onPrimaryContainer = lerp(primary, Color.White, 0.55f),
            secondary = accent.darkSecondary,
            onSecondary = readableOn(accent.darkSecondary),
            secondaryContainer = accent.darkPrimaryContainer,
            onSecondaryContainer = lerp(primary, Color.White, 0.55f),
            tertiary = accent.darkSecondary,
            onTertiary = readableOn(accent.darkSecondary),
            tertiaryContainer = accent.darkPrimaryContainer,
            onTertiaryContainer = lerp(primary, Color.White, 0.55f),
            background = c.groupedBackground,
            onBackground = c.label,
            surface = c.surface,
            onSurface = c.label,
            surfaceVariant = IosDarkElevated,
            onSurfaceVariant = c.secondaryLabel,
            surfaceTint = primary,
            inverseSurface = Color(0xFFE5E5EA),
            inverseOnSurface = Color(0xFF1C1C1E),
            inversePrimary = accent.lightPrimary,
            error = c.destructive,
            onError = Color(0xFF3A0504),
            errorContainer = Color(0xFF4A1512),
            onErrorContainer = Color(0xFFFFB4AE),
            outline = Color(0xFF8E8E93),
            outlineVariant = Color(0xFF38383A),
            scrim = Color.Black,
            surfaceBright = Color(0xFF3A3A3C),
            surfaceDim = IosDarkBackground,
            surfaceContainerLowest = IosDarkBackground,
            surfaceContainerLow = c.surface,
            surfaceContainer = c.surface,
            surfaceContainerHigh = c.elevatedSurface,
            surfaceContainerHighest = c.elevatedSurface
        )
    } else {
        val primary = accent.lightPrimary
        lightColorScheme(
            primary = primary,
            onPrimary = accent.lightOnPrimary,
            primaryContainer = accent.lightPrimaryContainer,
            onPrimaryContainer = lerp(primary, Color.Black, 0.55f),
            secondary = accent.lightSecondary,
            onSecondary = readableOn(accent.lightSecondary),
            secondaryContainer = accent.lightPrimaryContainer,
            onSecondaryContainer = lerp(primary, Color.Black, 0.55f),
            tertiary = accent.lightSecondary,
            onTertiary = readableOn(accent.lightSecondary),
            tertiaryContainer = accent.lightPrimaryContainer,
            onTertiaryContainer = lerp(primary, Color.Black, 0.55f),
            background = c.groupedBackground,
            onBackground = c.label,
            surface = c.surface,
            onSurface = c.label,
            surfaceVariant = Color(0xFFE9E9EE),
            onSurfaceVariant = c.secondaryLabel,
            surfaceTint = primary,
            inverseSurface = Color(0xFF1C1C1E),
            inverseOnSurface = Color(0xFFF2F2F7),
            inversePrimary = accent.darkPrimary,
            error = c.destructive,
            onError = Color.White,
            errorContainer = Color(0xFFFFE3E0),
            onErrorContainer = Color(0xFF8C0010),
            outline = Color(0xFF8E8E93),
            outlineVariant = Color(0xFFD1D1D6),
            scrim = Color.Black,
            surfaceBright = c.surface,
            surfaceDim = Color(0xFFE5E5EA),
            surfaceContainerLowest = c.surface,
            surfaceContainerLow = c.surface,
            surfaceContainer = c.surface,
            surfaceContainerHigh = Color(0xFFF7F7FA),
            surfaceContainerHighest = c.surface
        )
    }
}
