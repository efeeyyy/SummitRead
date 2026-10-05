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
import com.example.summitread.theme.AppMotion
import com.example.summitread.ui.components.GlassTabBar
import com.example.summitread.ui.components.GlassTabItem
import com.example.summitread.ui.components.LocalBottomBarHeight
import com.example.summitread.ui.components.glassSource
import com.example.summitread.ui.components.rememberGlassState
import com.example.summitread.ui.home.HomeScreen
import com.example.summitread.resources.Res
import com.example.summitread.resources.library
import com.example.summitread.resources.profile
import org.jetbrains.compose.resources.stringResource

/** Root destinations shown as tabs. The Mountain screen is deeper in the Library (Home) stack. */
private val TabRoutes: List<Any> = listOf(HomeRoute, ProfileRoute)

/**
 * What the app shell hands the Mountain screen slot. The shell owns the shared-element scopes (so the cover can
 * morph into the mountain) and the focus-mode flag (which hides the tab bar).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
class MountainSlotScope(
    val bookId: Long,
    val sharedTransitionScope: SharedTransitionScope,
    val animatedVisibilityScope: AnimatedVisibilityScope,
    val onFocusModeChange: (Boolean) -> Unit,
    val onNavigateBack: () -> Unit
)

/**
 * The app shell: Home + Mountain + Profile in a [NavHost] with the floating glass tab bar. Mountain and Profile are
 * injected as slots until they move to commonMain (KMP phase B steps 3 and 4).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SummitReadApp(
    bookRepository: BookRepository,
    userPreferences: UserPreferences,
    imageStorage: ImageStorage,
    mountainContent: @Composable (MountainSlotScope) -> Unit,
    profileContent: @Composable () -> Unit
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
        CompositionLocalProvider(LocalBottomBarHeight provides barHeight) {
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
                        mountainContent(
                            MountainSlotScope(
                                bookId = backStackEntry.toRoute<MountainRoute>().bookId,
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this@composable,
                                onFocusModeChange = { focused -> isFocusMode = focused },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        )
                    }
                    composable<ProfileRoute> {
                        profileContent()
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
