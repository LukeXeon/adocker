package com.adocker.runner.ui

import android.os.Build
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.adocker.runner.core.utils.PhantomProcessManager
import com.adocker.runner.ui.components.PhantomProcessWarningDialog
import com.adocker.runner.ui.navigation.Screen
import com.adocker.runner.ui.navigation.bottomNavItems
import com.adocker.runner.ui.screens.containers.ContainersScreen
import com.adocker.runner.ui.screens.containers.CreateContainerScreen
import com.adocker.runner.ui.screens.home.HomeScreen
import com.adocker.runner.ui.screens.images.ImagesScreen
import com.adocker.runner.ui.screens.images.PullImageScreen
import com.adocker.runner.ui.screens.images.QRCodeScannerScreen
import com.adocker.runner.ui.screens.settings.MirrorSettingsScreen
import com.adocker.runner.ui.screens.settings.PhantomProcessScreen
import com.adocker.runner.ui.screens.settings.SettingsScreen
import com.adocker.runner.ui.screens.terminal.TerminalScreen
import com.adocker.runner.ui.viewmodel.MainViewModel
import com.adocker.runner.ui.viewmodel.TerminalViewModel
import kotlinx.coroutines.launch

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Check if we should show bottom navigation
    val showBottomBar = remember(currentDestination) {
        bottomNavItems.any { screen ->
            currentDestination?.hierarchy?.any { it.route == screen.route } == true
        }
    }

    val mainViewModel: MainViewModel = hiltViewModel()

    // Create PhantomProcessManager instance
    // Note: PhantomProcessManager is @Singleton and injected via Hilt in ViewModels
    // Here we create a local instance for startup check
    val phantomProcessManager = remember(context) {
        PhantomProcessManager()
    }

    // Phantom process warning state
    var showPhantomWarning by remember { mutableStateOf(false) }
    var hasCheckedPhantomProcess by remember { mutableStateOf(false) }

    // Check phantom process restrictions on first launch
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasCheckedPhantomProcess) {
            scope.launch {
                if (phantomProcessManager.hasShizukuPermission()) {
                    val isDisabled = phantomProcessManager.isPhantomProcessKillerDisabled()
                    if (!isDisabled) {
                        showPhantomWarning = true
                    }
                }
                hasCheckedPhantomProcess = true
            }
        }
    }

    // Show warning dialog
    if (showPhantomWarning) {
        PhantomProcessWarningDialog(
            onDismiss = { showPhantomWarning = false },
            onNavigateToSettings = {
                showPhantomWarning = false
                navController.navigate(Screen.PhantomProcess.route)
            }
        )
    }

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
                                    contentDescription = stringResource(screen.titleResId)
                                )
                            },
                            label = { Text(stringResource(screen.titleResId)) },
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
                    },
                    onNavigateToPhantomProcess = {
                        navController.navigate(Screen.PhantomProcess.route)
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

            // Phantom Process Management
            composable(Screen.PhantomProcess.route) {
                PhantomProcessScreen(
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
