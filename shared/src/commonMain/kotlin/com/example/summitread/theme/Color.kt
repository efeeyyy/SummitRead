package com.example.summitread.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

// --- iOS system palette (SummitRead design system, see docs/DESIGN.md) -----------------------

// Light
val IosLightGroupedBackground = Color(0xFFF2F2F7) // systemGroupedBackground
val IosLightSurface = Color(0xFFFFFFFF)           // secondarySystemGroupedBackground
val IosLightLabel = Color(0xFF000000)
val IosLightSecondaryLabel = Color(0xFF636366)    // >=4.5:1 on every light surface (iOS 60% alpha is ~3.2:1)
val IosLightTertiaryLabel = Color(0xFFA0A0A5)     // placeholders / disabled only
val IosLightSeparator = Color(0x4A3C3C43)         // 29% alpha, hairline
val IosLightFill = Color(0x1F767680)              // tertiarySystemFill (12%)

// Dark (true black, OLED friendly)
val IosDarkBackground = Color(0xFF000000)
val IosDarkSurface = Color(0xFF1C1C1E)            // secondarySystemGroupedBackground
val IosDarkElevated = Color(0xFF2C2C2E)           // tertiarySystemGroupedBackground
val IosDarkLabel = Color(0xFFFFFFFF)
val IosDarkSecondaryLabel = Color(0xFF9D9DA3)
val IosDarkTertiaryLabel = Color(0xFF6E6E73)
val IosDarkSeparator = Color(0x99545458)
val IosDarkFill = Color(0x3D767680)               // 24%

// Shelf accent (wooden shelf on the home screen; decorative, not part of the iOS palette)
val ShelfBrown = Color(0xFF8B6914)
val ShelfBrownDark = Color(0xFF6B4F12)
val ShelfBrownLight = Color(0xFFC49B3A)

/**
 * The single user-chosen tint colour. Values are slightly desaturated "premium" tints:
 * every [lightPrimary]/[lightOnPrimary] and [darkPrimary]/[darkOnPrimary] pair keeps text
 * contrast >= 4.5:1 (enforced by AccentContrastTest).
 */
enum class AccentColor(
    val displayName: String,
    val lightPrimary: Color,
    val lightSecondary: Color,
    val lightPrimaryContainer: Color,
    val lightOnPrimary: Color,
    val darkPrimary: Color,
    val darkSecondary: Color,
    val darkPrimaryContainer: Color,
    val darkOnPrimary: Color
) {
    FOREST_GREEN(
        displayName = "Forest",
        lightPrimary = Color(0xFF2B7A55),
        lightSecondary = Color(0xFF3F8F69),
        lightPrimaryContainer = Color(0xFFDCEFE4),
        lightOnPrimary = Color.White,
        darkPrimary = Color(0xFF5FD39A),
        darkSecondary = Color(0xFF3FA97A),
        darkPrimaryContainer = Color(0xFF173A2B),
        darkOnPrimary = Color(0xFF04261A)
    ),
    SUNSET_ORANGE(
        displayName = "Sunset",
        lightPrimary = Color(0xFFB8440F),
        lightSecondary = Color(0xFFD1601F),
        lightPrimaryContainer = Color(0xFFFBE3D4),
        lightOnPrimary = Color.White,
        darkPrimary = Color(0xFFFF9F5C),
        darkSecondary = Color(0xFFE67E33),
        darkPrimaryContainer = Color(0xFF47260F),
        darkOnPrimary = Color(0xFF3A1904)
    ),
    OCEAN_BLUE(
        displayName = "Ocean",
        lightPrimary = Color(0xFF0A64CF),
        lightSecondary = Color(0xFF2273D6),
        lightPrimaryContainer = Color(0xFFDAE9FB),
        lightOnPrimary = Color.White,
        darkPrimary = Color(0xFF64B0FF),
        darkSecondary = Color(0xFF3D8FE6),
        darkPrimaryContainer = Color(0xFF12304F),
        darkOnPrimary = Color(0xFF00203F)
    ),
    BERRY_PURPLE(
        displayName = "Berry",
        lightPrimary = Color(0xFF8338AC),
        lightSecondary = Color(0xFF9A52C1),
        lightPrimaryContainer = Color(0xFFEDDCF6),
        lightOnPrimary = Color.White,
        darkPrimary = Color(0xFFC999EE),
        darkSecondary = Color(0xFFAC74D6),
        darkPrimaryContainer = Color(0xFF3A1F4E),
        darkOnPrimary = Color(0xFF2B0F42)
    ),
    AUTUMN_RED(
        displayName = "Autumn",
        lightPrimary = Color(0xFFC0332B),
        lightSecondary = Color(0xFFC63D35),
        lightPrimaryContainer = Color(0xFFF9DDDA),
        lightOnPrimary = Color.White,
        darkPrimary = Color(0xFFFF8B80),
        darkSecondary = Color(0xFFE86A5F),
        darkPrimaryContainer = Color(0xFF4E1F1B),
        darkOnPrimary = Color(0xFF450905)
    )
}

/** Semantic colour roles. Read via `AppTheme.colors`; also mapped into the Material colour scheme. */
@Immutable
data class AppColors(
    val isDark: Boolean,
    /** Primary canvas of grouped screens (iOS systemGroupedBackground; pure black in dark). */
    val groupedBackground: Color,
    /** Plain full-screen canvas (iOS systemBackground). */
    val background: Color,
    /** Cards / grouped rows sitting on [groupedBackground]. */
    val surface: Color,
    /** Sheets, alerts, popovers: one step above [surface]. */
    val elevatedSurface: Color,
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    /** Hairline (translucent): use with 0.5dp-1px width. */
    val separator: Color,
    /** Translucent grey fill for fields, segmented tracks, neutral buttons. */
    val fill: Color,
    /** The user-chosen accent. */
    val tint: Color,
    val onTint: Color,
    /** Soft tinted background (badges, tinted buttons). */
    val tintContainer: Color,
    val success: Color,
    val warning: Color,
    val destructive: Color,
    /** Summit / achievement gold. */
    val gold: Color
) {
    /** [fill] flattened onto [surface], for places that need an opaque colour. */
    val fillOpaque: Color get() = fill.compositeOver(surface)
}

fun appColors(isDark: Boolean, accent: AccentColor): AppColors =
    if (isDark) {
        AppColors(
            isDark = true,
            groupedBackground = IosDarkBackground,
            background = IosDarkBackground,
            surface = IosDarkSurface,
            elevatedSurface = IosDarkElevated,
            label = IosDarkLabel,
            secondaryLabel = IosDarkSecondaryLabel,
            tertiaryLabel = IosDarkTertiaryLabel,
            separator = IosDarkSeparator,
            fill = IosDarkFill,
            tint = accent.darkPrimary,
            onTint = accent.darkOnPrimary,
            tintContainer = accent.darkPrimaryContainer,
            success = Color(0xFF32D74B),
            warning = Color(0xFFFF9F0A),
            destructive = Color(0xFFFF6961),
            gold = Color(0xFFF5C451)
        )
    } else {
        AppColors(
            isDark = false,
            groupedBackground = IosLightGroupedBackground,
            background = IosLightSurface,
            surface = IosLightSurface,
            elevatedSurface = IosLightSurface,
            label = IosLightLabel,
            secondaryLabel = IosLightSecondaryLabel,
            tertiaryLabel = IosLightTertiaryLabel,
            separator = IosLightSeparator,
            fill = IosLightFill,
            tint = accent.lightPrimary,
            onTint = accent.lightOnPrimary,
            tintContainer = accent.lightPrimaryContainer,
            success = Color(0xFF248A3D),
            warning = Color(0xFFB25000),
            destructive = Color(0xFFD70015),
            gold = Color(0xFFB8860B)
        )
    }

val LocalAppColors = staticCompositionLocalOf { appColors(isDark = false, accent = AccentColor.FOREST_GREEN) }
