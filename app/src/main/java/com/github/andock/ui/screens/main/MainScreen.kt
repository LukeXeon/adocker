package com.github.andock.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
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
        bottomTabs.any { tab ->
            currentDestination?.hierarchy?.any { it.hasRoute(tab.type) } == true
        }
    }
    MainTheme {
        CompositionLocalProvider(
            LocalNavController provides navController
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = HomeRoute::class,
                    ) {
                        screens.forEach { (route, screen) ->
                            composable(
                                route = route,
                                deepLinks = screen.deepLinks,
                                content = screen.content
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    ) {
                        SnackbarHost(LocalSnackbarHostState.current)
                        AnimatedVisibility(
                            visible = showBottomBar,
                            enter = slideInVertically(initialOffsetY = { it }),
                            exit = slideOutVertically(targetOffsetY = { it })
                        ) {
                            NavigationBar {
                                bottomTabs.forEach { tab ->
                                    tab.Item { selected, route ->
                                        NavigationBarItem(
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) {
                                                        tab.selectedIcon
                                                    } else {
                                                        tab.unselectedIcon
                                                    },
                                                    contentDescription = stringResource(tab.titleResId)
                                                )
                                            },
                                            label = { Text(stringResource(tab.titleResId)) },
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
                        }
                    }
                }
            }
        }
    }
}
