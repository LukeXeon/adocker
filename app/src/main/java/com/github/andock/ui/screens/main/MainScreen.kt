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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.github.andock.ui.screens.home.HomeKey
import com.github.andock.ui.utils.debounceClick

val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("CompositionLocal LocalNavigator not present")
}

val LocalSnackbarHostState = staticCompositionLocalOf {
    SnackbarHostState()
}

val LocalResultEventBus = staticCompositionLocalOf<EventBus> {
    error("No ResultEventBus has been provided")
}

@Composable
fun MainScreen() {
    val mainViewModel = hiltViewModel<MainViewModel>()
    val bottomTabs = mainViewModel.bottomTabs

    // Create backstack and navigator for Navigation 3
    val backStack = remember { mutableStateListOf<NavKey>(HomeKey) }
    val topLevelRoutes = remember(bottomTabs) {
        bottomTabs.map { it.route() }.toSet()
    }
    val navigator = remember(backStack, topLevelRoutes) {
        Navigator(backStack, topLevelRoutes)
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val bus = remember { EventBus() }
    // Check if we should show bottom navigation
    val showBottomBar = remember(navigator.topLevelRoute) {
        navigator.topLevelRoute in topLevelRoutes
    }

    MainTheme {
        CompositionLocalProvider(
            LocalNavigator provides navigator,
            LocalSnackbarHostState provides snackbarHostState,
            LocalResultEventBus provides bus
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    NavDisplay(
                        backStack = backStack,
                        onBack = { navigator.goBack() },
                        entryProvider = entryProvider {
                            mainViewModel.entryBuilders.forEach { builder -> this.builder() }
                        },
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator()
                        )
                    )
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
                                    val route = tab.route()
                                    val selected = navigator.topLevelRoute == route
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
                                            navigator.navigate(route)
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
