package com.github.adocker.ui2.screens.main

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.ui.graphics.vector.ImageVector
import com.github.adocker.R

sealed class Screen(
    val route: String,
    @param:StringRes val titleResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Screen(
        route = "home",
        titleResId = R.string.nav_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Discover : Screen(
        route = "discover",
        titleResId = R.string.nav_discover,
        selectedIcon = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore
    )

    data object Containers : Screen(
        route = "containers",
        titleResId = R.string.nav_containers,
        selectedIcon = Icons.Filled.ViewInAr,
        unselectedIcon = Icons.Outlined.ViewInAr
    )

    data object Images : Screen(
        route = "images",
        titleResId = R.string.nav_images,
        selectedIcon = Icons.Filled.Layers,
        unselectedIcon = Icons.Outlined.Layers
    )

    data object Settings : Screen(
        route = "settings",
        titleResId = R.string.nav_settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    data object Terminal : Screen(
        route = "terminal/{containerId}",
        titleResId = R.string.nav_terminal,
        selectedIcon = Icons.Filled.Terminal,
        unselectedIcon = Icons.Outlined.Terminal
    ) {
        fun createRoute(containerId: String) = "terminal/$containerId"
    }

    data object ImageDetail : Screen(
        route = "image/{imageId}",
        titleResId = R.string.nav_image_detail,
        selectedIcon = Icons.Filled.Info,
        unselectedIcon = Icons.Outlined.Info
    ) {
        fun createRoute(imageId: String) = "image/$imageId"
    }

    data object ContainerDetail : Screen(
        route = "container/{containerId}",
        titleResId = R.string.nav_container_detail,
        selectedIcon = Icons.Filled.Info,
        unselectedIcon = Icons.Outlined.Info
    ) {
        fun createRoute(containerId: String) = "container/$containerId"
    }

    data object CreateContainer : Screen(
        route = "create/{imageId}",
        titleResId = R.string.nav_create_container,
        selectedIcon = Icons.Filled.Add,
        unselectedIcon = Icons.Outlined.Add
    ) {
        fun createRoute(imageId: String) = "create/$imageId"
    }

    data object MirrorSettings : Screen(
        route = "mirror_settings",
        titleResId = R.string.mirror_settings_title,
        selectedIcon = Icons.Filled.Cloud,
        unselectedIcon = Icons.Outlined.Cloud
    )

    data object QRCodeScanner : Screen(
        route = "qr_scanner",
        titleResId = R.string.nav_qr_scanner,
        selectedIcon = Icons.Filled.QrCodeScanner,
        unselectedIcon = Icons.Outlined.QrCodeScanner
    )

    data object PhantomProcess : Screen(
        route = "phantom_process",
        titleResId = R.string.nav_phantom_process,
        selectedIcon = Icons.Filled.Block,
        unselectedIcon = Icons.Outlined.Block
    )
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Discover,
    Screen.Containers,
    Screen.Images,
    Screen.Settings
)
