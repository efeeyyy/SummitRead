package com.example.summitread

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import com.example.summitread.data.BookRepository
import com.example.summitread.data.ImageStorage
import com.example.summitread.data.UserPreferences
import com.example.summitread.platform.LocalReadingTimer
import com.example.summitread.platform.LocalTiltSensor
import com.example.summitread.platform.ReadingTimer
import com.example.summitread.platform.TiltSensor
import com.example.summitread.theme.AppMotion
import com.example.summitread.ui.components.GlassTabBar
import com.example.summitread.ui.components.GlassTabItem
import com.example.summitread.ui.components.LocalBottomBarHeight
import com.example.summitread.ui.components.glassSource
import com.example.summitread.ui.components.rememberGlassState
import com.example.summitread.ui.home.HomeScreen
import com.example.summitread.ui.mountain.MountainScreen
import com.example.summitread.ui.profile.ProfileScreen
import com.example.summitread.resources.Res
import com.example.summitread.resources.library
import com.example.summitread.resources.profile
import org.jetbrains.compose.resources.stringResource

/** Root destinations shown as tabs. The Mountain screen is deeper in the Library (Home) stack. */
private val TabRoutes: List<Any> = listOf(HomeRoute, ProfileRoute)

/**
 * The app shell: Home + Mountain + Profile in a [NavHost] with the floating glass tab bar. The shell owns the
 * shared-element scopes (the cover morphs into the mountain) and the focus-mode flag (which hides the tab bar).
 * [readingTimer] is the platform
 * timer (Android foreground service / iOS in-app timer), exposed to the screens through [LocalReadingTimer];
 * [tiltSensor] is the platform tilt source for the profile relic parallax ([LocalTiltSensor]).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SummitReadApp(
    bookRepository: BookRepository,
    userPreferences: UserPreferences,
    imageStorage: ImageStorage,
    readingTimer: ReadingTimer,
    tiltSensor: TiltSensor
) {
    val navController = rememberNavController()
    // Set by the Mountain screen; only honoured while that screen is the current destination, so a leftover
    // flag can never hide the tab bar on Home/Profile.
    var isFocusMode by remember { mutableStateOf(false) }
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentEntry?.destination
    val onMountain = currentDestination?.hasRoute(MountainRoute::class) == true
    val hideBottomBar = isFocusMode && onMountain
    // Mountain lives inside the Library stack, so the Library tab stays highlighted there.
    val selectedIndex = if (currentDestination?.hasRoute(ProfileRoute::class) == true) 1 else 0

    val libraryLabel = stringResource(Res.string.library)
    val profileLabel = stringResource(Res.string.profile)
    val tabItems = remember(libraryLabel, profileLabel) {
        listOf(
            GlassTabItem(libraryLabel, Icons.Rounded.AutoStories),
            GlassTabItem(profileLabel, Icons.Rounded.Person)
        )
    }

    val glass = rememberGlassState()
    val density = LocalDensity.current
    var measuredBarHeight by remember { mutableStateOf(0.dp) }
    // Screens pad their content by this; it collapses to 0 while the bar is hidden (focus mode).
    val barHeight by animateDpAsState(
        targetValue = if (hideBottomBar) 0.dp else measuredBarHeight,
        animationSpec = AppMotion.standard(),
        label = "bottomBarInset"
    )

    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(
            LocalBottomBarHeight provides barHeight,
            LocalReadingTimer provides readingTimer,
            LocalTiltSensor provides tiltSensor
        ) {
            SharedTransitionLayout(Modifier.fillMaxSize().glassSource(glass)) {
                NavHost(
                    navController = navController,
                    startDestination = HomeRoute,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable<HomeRoute> {
                        HomeScreen(
                            bookRepository = bookRepository,
                            userPreferences = userPreferences,
                            imageStorage = imageStorage,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@composable,
                            onNavigateToMountain = { bookId ->
                                navController.navigate(MountainRoute(bookId)) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    composable<MountainRoute> { backStackEntry ->
                        MountainScreen(
                            bookId = backStackEntry.toRoute<MountainRoute>().bookId,
                            bookRepository = bookRepository,
                            userPreferences = userPreferences,
                            imageStorage = imageStorage,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@composable,
                            onFocusModeChange = { focused -> isFocusMode = focused },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable<ProfileRoute> {
                        ProfileScreen(
                            bookRepository = bookRepository,
                            userPreferences = userPreferences
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !hideBottomBar,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(AppMotion.standard()) { it } + fadeIn(),
            exit = slideOutVertically(AppMotion.standard()) { it } + fadeOut()
        ) {
            GlassTabBar(
                items = tabItems,
                selectedIndex = selectedIndex,
                glassState = glass,
                modifier = Modifier.onSizeChanged { measuredBarHeight = with(density) { it.height.toDp() } },
                onSelect = { index ->
                    val route = TabRoutes[index]
                    if (index == selectedIndex) {
                        // iOS: tapping the active tab returns to the root of that tab's stack.
                        navController.popBackStack(route, inclusive = false)
                    } else {
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}
