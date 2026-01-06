package com.github.andock.ui.screens.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewInAr
import com.github.andock.R
import com.github.andock.ui.screens.containers.ContainersRoute
import com.github.andock.ui.screens.home.HomeRoute
import com.github.andock.ui.screens.images.ImagesRoute
import com.github.andock.ui.screens.search.SearchRoute
import com.github.andock.ui.screens.settings.SettingsRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MainModule {
    @Provides
    @Singleton
    fun tabs() = listOf(
        MainBottomTab(
            titleResId = R.string.nav_home,
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
        ) {
            HomeRoute
        },
        MainBottomTab(
            titleResId = R.string.nav_discover,
            selectedIcon = Icons.Filled.Explore,
            unselectedIcon = Icons.Outlined.Explore,
        ) {
            SearchRoute
        },
        MainBottomTab(
            titleResId = R.string.nav_containers,
            selectedIcon = Icons.Filled.ViewInAr,
            unselectedIcon = Icons.Outlined.ViewInAr,
        ) {
            ContainersRoute
        },
        MainBottomTab(
            titleResId = R.string.nav_images,
            selectedIcon = Icons.Filled.Layers,
            unselectedIcon = Icons.Outlined.Layers,
        ) {
            ImagesRoute
        },
        MainBottomTab(
            titleResId = R.string.nav_settings,
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings,
        ) {
            SettingsRoute
        }
    )

}