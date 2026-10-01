package com.example.summitread

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.summitread.data.BookRepository
import com.example.summitread.data.UserPreferences
import com.example.summitread.ui.home.HomeScreen
import com.example.summitread.ui.mountain.MountainScreen
import com.example.summitread.ui.profile.ProfileScreen

private data class BottomNavItem<T : Any>(
    val route: T,
    val labelResId: Int,
    val icon: ImageVector
)

@Composable
fun SummitReadApp(
    bookRepository: BookRepository,
    userPreferences: UserPreferences,
    context: Context
) {
    val navController = rememberNavController()
    var isFocusMode by remember { mutableStateOf(false) }

    val navItems = remember {
        listOf(
            BottomNavItem(
                route = HomeRoute,
                labelResId = R.string.home,
                icon = Icons.Filled.Home
            ),
            BottomNavItem(
                route = ProfileRoute,
                labelResId = R.string.profile,
                icon = Icons.Filled.Person
            )
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AnimatedVisibility(
                visible = !isFocusMode,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    navItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentDestination?.hasRoute(item.route::class) == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = stringResource(item.labelResId)
                                )
                            },
                            label = { Text(text = stringResource(item.labelResId)) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        SharedTransitionLayout {
            NavHost(
                navController = navController,
                startDestination = HomeRoute,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable<HomeRoute> {
                    HomeScreen(
                        bookRepository = bookRepository,
                        userPreferences = userPreferences,
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
}
