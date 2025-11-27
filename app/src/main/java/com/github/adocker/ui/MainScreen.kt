package com.github.adocker.ui

import android.os.Build
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.github.adocker.ui.screens.qrcode.MirrorQRCode
import com.github.adocker.ui.components.PhantomProcessWarningDialog
import com.github.adocker.ui.navigation.Screen
import com.github.adocker.ui.navigation.bottomNavItems
import com.github.adocker.ui.screens.containers.ContainerDetailScreen
import com.github.adocker.ui.screens.containers.ContainersScreen
import com.github.adocker.ui.screens.containers.CreateContainerScreen
import com.github.adocker.ui.screens.discover.DiscoverScreen
import com.github.adocker.ui.screens.home.HomeScreen
import com.github.adocker.ui.screens.images.ImageDetailScreen
import com.github.adocker.ui.screens.images.ImagesScreen
import com.github.adocker.ui.screens.qrcode.QRCodeScannerScreen
import com.github.adocker.ui.screens.settings.MirrorSettingsScreen
import com.github.adocker.ui.screens.settings.PhantomProcessScreen
import com.github.adocker.ui.screens.settings.SettingsScreen
import com.github.adocker.ui.screens.terminal.TerminalScreen
import com.github.adocker.ui.viewmodel.MainViewModel
import com.github.adocker.ui.viewmodel.PhantomProcessViewModel
import com.github.adocker.ui.viewmodel.TerminalViewModel
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

    val mainViewModel = hiltViewModel<MainViewModel>()
    val phantomViewModel = hiltViewModel<PhantomProcessViewModel>()

    // Phantom process warning state
    var showPhantomWarning by remember { mutableStateOf(false) }
    var hasCheckedPhantomProcess by remember { mutableStateOf(false) }

    // Observe phantom process UI state
    val phantomUiState by phantomViewModel.uiState.collectAsState()

    // Check phantom process restrictions on first launch
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasCheckedPhantomProcess) {
            scope.launch {
                phantomViewModel.checkStatus()
                hasCheckedPhantomProcess = true
            }
        }
    }

    // Show warning if phantom killer is not disabled and shizuku permission granted
    LaunchedEffect(phantomUiState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            phantomUiState.shizukuPermissionGranted &&
            !phantomUiState.phantomKillerDisabled &&
            !phantomUiState.isChecking &&
            hasCheckedPhantomProcess
        ) {
            showPhantomWarning = true
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
                        val selected =
                            currentDestination?.hierarchy?.any { it.route == screen.route } == true

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
                    onNavigateToImages = {
                        navController.navigate(Screen.Images.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToMirrorSettings = {
                        navController.navigate(Screen.MirrorSettings.route)
                    }
                )
            }

            // Discover
            composable(Screen.Discover.route) {
                DiscoverScreen(viewModel = mainViewModel)
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
                    onNavigateToCreate = { imageId ->
                        navController.navigate(Screen.CreateContainer.createRoute(imageId))
                    },
                    onNavigateToDetail = { imageId ->
                        navController.navigate(Screen.ImageDetail.createRoute(imageId))
                    },
                    onNavigateToQRScanner = {
                        navController.navigate(Screen.QRCodeScanner.route)
                    },
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
            composable(Screen.MirrorSettings.route) { backStackEntry ->
                val scannedMirror = backStackEntry.savedStateHandle
                    .getStateFlow<String?>("scanned_mirror", null)
                    .collectAsState()

                // Process scanned mirror data
                LaunchedEffect(scannedMirror.value) {
                    if (scannedMirror.value != null) {
                        // Will be handled by MirrorSettingsScreen
                        backStackEntry.savedStateHandle.remove<String>("scanned_mirror")
                    }
                }

                MirrorSettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToQRScanner = {
                        navController.navigate(Screen.QRCodeScanner.route)
                    },
                    scannedMirrorData = scannedMirror.value,
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

            // QR Code Scanner
            composable(Screen.QRCodeScanner.route) {
                QRCodeScannerScreen(
                    onBarcodeScanned = { data ->
                        // Try to parse as mirror QR code first
                        try {
                            val json = mainViewModel.json
                            val mirrorQRCode = json.runCatching {
                                decodeFromString<MirrorQRCode>(data)
                            }.getOrNull()
                            if (mirrorQRCode != null) {
                                // It's a mirror QR code
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("scanned_mirror", data)
                                navController.popBackStack()
                                return@QRCodeScannerScreen
                            }
                        } catch (_: Exception) {
                            // Not a mirror QR code, try as image name
                        }
                        // Treat as image name
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("scanned_image", data)
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
                val terminalViewModel = hiltViewModel<TerminalViewModel>()
                TerminalScreen(
                    viewModel = terminalViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Image Detail
            composable(
                route = Screen.ImageDetail.route,
                arguments = listOf(navArgument("imageId") { type = NavType.StringType })
            ) { backStackEntry ->
                val imageId = backStackEntry.arguments?.getString("imageId") ?: return@composable
                ImageDetailScreen(
                    imageId = imageId,
                    viewModel = mainViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToCreate = { imgId ->
                        navController.navigate(Screen.CreateContainer.createRoute(imgId))
                    }
                )
            }

            // Container Detail
            composable(
                route = Screen.ContainerDetail.route,
                arguments = listOf(navArgument("containerId") { type = NavType.StringType })
            ) { backStackEntry ->
                val containerId = backStackEntry.arguments?.getString("containerId") ?: return@composable
                ContainerDetailScreen(
                    containerId = containerId,
                    viewModel = mainViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToTerminal = { cId ->
                        navController.navigate(Screen.Terminal.createRoute(cId))
                    }
                )
            }
        }
    }
}
