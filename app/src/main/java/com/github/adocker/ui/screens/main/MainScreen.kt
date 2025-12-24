package com.github.adocker.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.adocker.ui2.screens.main.Screen
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph

@Destination<RootGraph>(start = true)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    // Check if we should show bottom navigation
//    val showBottomBar = remember(currentDestination) {
//        bottomNavItems.any { screen ->
//            currentDestination?.hierarchy?.any { it.route == screen.route } == true
//        }
//    }
    val mainViewModel = hiltViewModel<MainViewModel>()
    val tabs = mainViewModel.tabs
    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar {
//                    tabs.forEach { screen ->
//                        val selected =
//                            currentDestination?.hierarchy?.any { it.route == screen.route } == true
//
//                        NavigationBarItem(
//                            icon = {
//                                Icon(
//                                    imageVector = if (selected) {
//                                        screen.selectedIcon
//                                    } else {
//                                        screen.unselectedIcon
//                                    },
//                                    contentDescription = stringResource(screen.titleResId)
//                                )
//                            },
//                            label = { Text(stringResource(screen.titleResId)) },
//                            selected = selected,
//                            onClick = {
//
//                            }
//                        )
//                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {

        }
    }
}
