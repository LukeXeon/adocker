package com.adocker.runner.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Screen(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Containers : Screen(
        route = "containers",
        title = "Containers",
        selectedIcon = Icons.Filled.ViewInAr,
        unselectedIcon = Icons.Outlined.ViewInAr
    )

    data object Images : Screen(
        route = "images",
        title = "Images",
        selectedIcon = Icons.Filled.Layers,
        unselectedIcon = Icons.Outlined.Layers
    )

    data object Settings : Screen(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    data object Terminal : Screen(
        route = "terminal/{containerId}",
        title = "Terminal",
        selectedIcon = Icons.Filled.Terminal,
        unselectedIcon = Icons.Outlined.Terminal
    ) {
        fun createRoute(containerId: String) = "terminal/$containerId"
    }

    data object ImageDetail : Screen(
        route = "image/{imageId}",
        title = "Image Detail",
        selectedIcon = Icons.Filled.Info,
        unselectedIcon = Icons.Outlined.Info
    ) {
        fun createRoute(imageId: String) = "image/$imageId"
    }

    data object ContainerDetail : Screen(
        route = "container/{containerId}",
        title = "Container Detail",
        selectedIcon = Icons.Filled.Info,
        unselectedIcon = Icons.Outlined.Info
    ) {
        fun createRoute(containerId: String) = "container/$containerId"
    }

    data object PullImage : Screen(
        route = "pull",
        title = "Pull Image",
        selectedIcon = Icons.Filled.CloudDownload,
        unselectedIcon = Icons.Outlined.CloudDownload
    )

    data object CreateContainer : Screen(
        route = "create/{imageId}",
        title = "Create Container",
        selectedIcon = Icons.Filled.Add,
        unselectedIcon = Icons.Outlined.Add
    ) {
        fun createRoute(imageId: String) = "create/$imageId"
    }

    data object MirrorSettings : Screen(
        route = "mirror_settings",
        title = "Registry Mirrors",
        selectedIcon = Icons.Filled.Cloud,
        unselectedIcon = Icons.Outlined.Cloud
    )

    data object QRCodeScanner : Screen(
        route = "qr_scanner",
        title = "Scan QR Code",
        selectedIcon = Icons.Filled.QrCodeScanner,
        unselectedIcon = Icons.Outlined.QrCodeScanner
    )

    data object PhantomProcess : Screen(
        route = "phantom_process",
        title = "Phantom Process Management",
        selectedIcon = Icons.Filled.Block,
        unselectedIcon = Icons.Outlined.Block
    )
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Containers,
    Screen.Images,
    Screen.Settings
)
