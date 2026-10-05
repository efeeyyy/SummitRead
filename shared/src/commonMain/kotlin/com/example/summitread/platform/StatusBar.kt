package com.example.summitread.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Controls the status bar icon colour while the calling screen is shown. [light] = the surface behind the icons is
 * light (so the icons must be dark); null = leave the current appearance alone. The previous appearance is restored
 * when the composable leaves the composition; [reapplyKey] forces the value to be applied again when it changes.
 *
 * Android: `WindowInsetsController.isAppearanceLightStatusBars`. iOS: no-op for now (the status bar style is owned
 * by the hosting view controller; UNVERIFIED / not implemented).
 */
@Composable
expect fun StatusBarIcons(light: Boolean?, reapplyKey: Any? = null)

/** The app-wide reading timer, provided by the platform host (`SummitReadApp`). */
val LocalReadingTimer = staticCompositionLocalOf<ReadingTimer> {
    error("No ReadingTimer provided: wrap the UI in SummitReadApp")
}
