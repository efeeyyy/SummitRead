package com.example.summitread

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.example.summitread.data.ThemeMode
import com.example.summitread.platform.IosPlatform
import com.example.summitread.theme.AccentColor
import com.example.summitread.theme.SummitReadTheme
import platform.UIKit.UIViewController

/**
 * iOS entry point (UNVERIFIED on a Mac, never run): the SwiftUI host embeds this controller via
 * `UIViewControllerRepresentable` (`MainViewControllerKt.MainViewController()`). Mirrors `MainActivity`: theme from the
 * preferences, then [SummitReadApp] with the iOS platform objects from [IosPlatform].
 *
 * TODO (phase C, not wired yet): streak reminders (`StreakReminders.observe` + `IosPlatform.reminderScheduler`,
 * notification permission request, reschedule on scene-phase changes), `UNUserNotificationCenterDelegate` for
 * foreground notifications, status bar style.
 */
fun MainViewController(): UIViewController = ComposeUIViewController {
    val container = IosPlatform.container
    val tiltSensor = remember { IosPlatform.createTiltSensor() }
    val userPreferences = container.preferences
    val themeMode by userPreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val accentColorIndex by userPreferences.accentColorIndex.collectAsState(initial = 0)
    val accentColor = AccentColor.entries.getOrNull(accentColorIndex) ?: AccentColor.FOREST_GREEN

    SummitReadTheme(themeMode = themeMode, accentColor = accentColor) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            SummitReadApp(
                bookRepository = container.repository,
                userPreferences = userPreferences,
                imageStorage = container.imageStorage,
                readingTimer = IosPlatform.readingTimer,
                tiltSensor = tiltSensor
            )
        }
    }
}
