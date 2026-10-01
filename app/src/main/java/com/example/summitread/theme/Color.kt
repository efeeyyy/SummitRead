package com.example.summitread.theme

import androidx.compose.ui.graphics.Color

// Base palette — Light theme
val LightBackground = Color(0xFFF8F6F2)       // warm off-white
val LightSurface = Color(0xFFFFFFFF)           // pure white cards
val LightSurfaceVariant = Color(0xFFF0EDE8)    // slightly warm gray

// Base palette — Dark theme
val DarkBackground = Color(0xFF1A1A2E)         // deep navy
val DarkSurface = Color(0xFF242442)            // slightly lighter navy
val DarkSurfaceVariant = Color(0xFF2D2D50)

// Text colors
val TextPrimary = Color(0xFF2C2C2C)            // near-black
val TextSecondary = Color(0xFF6B6B6B)          // medium gray
val TextOnDark = Color(0xFFE8E8E8)
val TextSecondaryOnDark = Color(0xFFB0B0B0)

// Shelf accent
val ShelfBrown = Color(0xFF8B6914)             // wooden shelf
val ShelfBrownDark = Color(0xFF6B4F12)
val ShelfBrownLight = Color(0xFFC49B3A)

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
        lightPrimary = Color(0xFF2E7D32),
        lightSecondary = Color(0xFF66BB6A),
        lightPrimaryContainer = Color(0xFFC8E6C9),
        lightOnPrimary = Color.White,
        darkPrimary = Color(0xFF81C784),
        darkSecondary = Color(0xFF388E3C),
        darkPrimaryContainer = Color(0xFF1B5E20),
        darkOnPrimary = Color(0xFF1B5E20)
    ),
    SUNSET_ORANGE(
        displayName = "Sunset",
        lightPrimary = Color(0xFFE65100),
        lightSecondary = Color(0xFFFF8F00),
        lightPrimaryContainer = Color(0xFFFFE0B2),
        lightOnPrimary = Color.White,
        darkPrimary = Color(0xFFFFB74D),
        darkSecondary = Color(0xFFF57C00),
        darkPrimaryContainer = Color(0xFFBF360C),
        darkOnPrimary = Color(0xFF3E2723)
    ),
    OCEAN_BLUE(
        displayName = "Ocean",
        lightPrimary = Color(0xFF1565C0),
        lightSecondary = Color(0xFF42A5F5),
        lightPrimaryContainer = Color(0xFFBBDEFB),
        lightOnPrimary = Color.White,
        darkPrimary = Color(0xFF90CAF9),
        darkSecondary = Color(0xFF1E88E5),
        darkPrimaryContainer = Color(0xFF0D47A1),
        darkOnPrimary = Color(0xFF0D47A1)
    ),
    BERRY_PURPLE(
        displayName = "Berry",
        lightPrimary = Color(0xFF7B1FA2),
        lightSecondary = Color(0xFFAB47BC),
        lightPrimaryContainer = Color(0xFFE1BEE7),
        lightOnPrimary = Color.White,
        darkPrimary = Color(0xFFCE93D8),
        darkSecondary = Color(0xFF8E24AA),
        darkPrimaryContainer = Color(0xFF4A148C),
        darkOnPrimary = Color(0xFF4A148C)
    ),
    AUTUMN_RED(
        displayName = "Autumn",
        lightPrimary = Color(0xFFC62828),
        lightSecondary = Color(0xFFEF5350),
        lightPrimaryContainer = Color(0xFFFFCDD2),
        lightOnPrimary = Color.White,
        darkPrimary = Color(0xFFEF9A9A),
        darkSecondary = Color(0xFFD32F2F),
        darkPrimaryContainer = Color(0xFFB71C1C),
        darkOnPrimary = Color(0xFFB71C1C)
    )
}
