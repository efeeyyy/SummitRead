package com.example.summitread

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import com.example.summitread.theme.resolveIsDark
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.summitread.data.BookRepository
import com.example.summitread.data.ThemeMode
import com.example.summitread.data.UserPreferences
import com.example.summitread.data.db.AppDatabase
import com.example.summitread.theme.AccentColor
import com.example.summitread.theme.SummitReadTheme
import androidx.lifecycle.lifecycleScope
import com.example.summitread.notifications.NotificationChannels
import com.example.summitread.worker.StreakReminderScheduler
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getInstance(applicationContext)
        val bookRepository = BookRepository(db.bookDao())
        val userPreferences = UserPreferences(applicationContext)

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            // Permission granted or rejected
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        NotificationChannels.ensureCreated(applicationContext)
        // 6h / 1h streak-rescue reminders: rescheduled after every log and when the preference changes.
        StreakReminderScheduler.observe(lifecycleScope, applicationContext, bookRepository, userPreferences)

        enableEdgeToEdge()
        setContent {
            val themeMode by userPreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val accentColorIndex by userPreferences.accentColorIndex.collectAsState(initial = 0)
            val accentColor = AccentColor.entries.getOrNull(accentColorIndex) ?: AccentColor.FOREST_GREEN

            // Status/navigation bar icon colours follow the APP theme, not the system one.
            val isDark = resolveIsDark(themeMode, isSystemInDarkTheme())
            DisposableEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { isDark },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.argb(0xe6, 0xFF, 0xFF, 0xFF),
                        android.graphics.Color.argb(0x80, 0x1B, 0x1B, 0x1B)
                    ) { isDark }
                )
                onDispose { }
            }

            SummitReadTheme(
                themeMode = themeMode,
                accentColor = accentColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SummitReadApp(
                        bookRepository = bookRepository,
                        userPreferences = userPreferences,
                        context = applicationContext
                    )
                }
            }
        }
    }
}
