package com.github.andock.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.andock.ui.screens.home.HomeRoute
import com.github.andock.ui.utils.debounceClick

val LocalNavController = staticCompositionLocalOf<NavHostController> {
    error("CompositionLocal LocalNavController not present")
}

val LocalSnackbarHostState = staticCompositionLocalOf {
    SnackbarHostState()
}

@Composable
fun MainScreen() {
    val mainViewModel = hiltViewModel<MainViewModel>()
    val bottomTabs = mainViewModel.bottomTabs
    val screens = mainViewModel.screens
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    // Check if we should show bottom navigation
    val showBottomBar = remember(currentDestination) {
        bottomTabs.any { (route, _) ->
            currentDestination?.hierarchy?.any { it.hasRoute(route) } == true
        }
    }
    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(
            WindowInsetsSides.Bottom
        ),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar {
                    bottomTabs.forEach { (route, screen) ->
                        val selected = remember(currentDestination, screen) {
                            currentDestination?.hierarchy
                                ?.any { it.hasRoute(route) } == true
                        }
                        val route = screen.route()
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) {
                                        screen.selectedIcon
                                    } else {
                                        screen.unselectedIcon
                                    },
                                    contentDescription = stringResource(screen.titleResId)
                                )
                            },
                            label = { Text(stringResource(screen.titleResId)) },
                            selected = selected,
                            onClick = debounceClick {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        },
        snackbarHost = {
            SnackbarHost(LocalSnackbarHostState.current)
        }
    ) { paddingValues ->
        CompositionLocalProvider(
            LocalNavController provides navController
        ) {
            NavHost(
                navController = navController,
                startDestination = HomeRoute::class,
                modifier = Modifier.padding(
                    paddingValues
                )
            ) {
                screens.forEach { (route, screen) ->
                    composable(
                        route = route,
                        deepLinks = screen.deepLinks,
                        content = screen.content
                    )
                }
            }
        }
    }
}
