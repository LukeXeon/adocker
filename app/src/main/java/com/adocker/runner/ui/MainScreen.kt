package com.adocker.runner.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.adocker.runner.ui.navigation.Screen
import com.adocker.runner.ui.navigation.bottomNavItems
import com.adocker.runner.ui.screens.containers.ContainersScreen
import com.adocker.runner.ui.screens.containers.CreateContainerScreen
import com.adocker.runner.ui.screens.home.HomeScreen
import com.adocker.runner.ui.screens.images.ImagesScreen
import com.adocker.runner.ui.screens.images.PullImageScreen
import com.adocker.runner.ui.screens.images.QRCodeScannerScreen
import com.adocker.runner.ui.screens.settings.MirrorSettingsScreen
import com.adocker.runner.ui.screens.settings.SettingsScreen
import com.adocker.runner.ui.screens.terminal.TerminalScreen
import com.adocker.runner.ui.viewmodel.MainViewModel
import com.adocker.runner.ui.viewmodel.TerminalViewModel

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Check if we should show bottom navigation
    val showBottomBar = remember(currentDestination) {
        bottomNavItems.any { screen ->
            currentDestination?.hierarchy?.any { it.route == screen.route } == true
        }
    }

    val mainViewModel: MainViewModel = hiltViewModel()

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Home
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = mainViewModel,
                    onNavigateToContainers = {
                        navController.navigate(Screen.Containers.route)
                    },
                    onNavigateToImages = {
                        navController.navigate(Screen.Images.route)
                    },
                    onNavigateToPull = {
                        navController.navigate(Screen.PullImage.route)
                    }
                )
            }

            // Containers
            composable(Screen.Containers.route) {
                ContainersScreen(
                    viewModel = mainViewModel,
                    onNavigateToTerminal = { containerId ->
                        navController.navigate(Screen.Terminal.createRoute(containerId))
                    },
                    onNavigateToDetail = { containerId ->
                        navController.navigate(Screen.ContainerDetail.createRoute(containerId))
                    }
                )
            }

            // Images
            composable(Screen.Images.route) {
                ImagesScreen(
                    viewModel = mainViewModel,
                    onNavigateToPull = {
                        navController.navigate(Screen.PullImage.route)
                    },
                    onNavigateToCreate = { imageId ->
                        navController.navigate(Screen.CreateContainer.createRoute(imageId))
                    },
                    onNavigateToDetail = { imageId ->
                        navController.navigate(Screen.ImageDetail.createRoute(imageId))
                    }
                )
            }

            // Settings
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToMirrorSettings = {
                        navController.navigate(Screen.MirrorSettings.route)
                    }
                )
            }

            // Mirror Settings
            composable(Screen.MirrorSettings.route) {
                MirrorSettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Pull Image
            composable(Screen.PullImage.route) { backStackEntry ->
                val scannedImage = backStackEntry.savedStateHandle
                    .getStateFlow<String?>("scanned_image", null)
                    .collectAsState()

                // Clear scanned image after reading
                LaunchedEffect(scannedImage.value) {
                    if (scannedImage.value != null) {
                        backStackEntry.savedStateHandle.remove<String>("scanned_image")
                    }
                }

                PullImageScreen(
                    viewModel = mainViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToQRScanner = {
                        navController.navigate(Screen.QRCodeScanner.route)
                    },
                    scannedImageName = scannedImage.value
                )
            }

            // QR Code Scanner
            composable(Screen.QRCodeScanner.route) {
                QRCodeScannerScreen(
                    onBarcodeScanned = { imageName ->
                        // Set the scanned image to the previous screen's savedStateHandle
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("scanned_image", imageName)
                        navController.popBackStack()
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Create Container
            composable(
                route = Screen.CreateContainer.route,
                arguments = listOf(navArgument("imageId") { type = NavType.StringType })
            ) { backStackEntry ->
                val imageId = backStackEntry.arguments?.getString("imageId") ?: return@composable
                CreateContainerScreen(
                    imageId = imageId,
                    viewModel = mainViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Terminal
            composable(
                route = Screen.Terminal.route,
                arguments = listOf(navArgument("containerId") { type = NavType.StringType })
            ) {
                val terminalViewModel: TerminalViewModel = hiltViewModel()
                TerminalScreen(
                    viewModel = terminalViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
