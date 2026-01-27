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
import com.github.andock.ui.screens.containers.ContainersKey
import com.github.andock.ui.screens.home.HomeKey
import com.github.andock.ui.screens.images.ImagesKey
import com.github.andock.ui.screens.search.SearchKey
import com.github.andock.ui.screens.settings.SettingsKey
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
            HomeKey
        },
        MainBottomTab(
            titleResId = R.string.nav_discover,
            selectedIcon = Icons.Filled.Explore,
            unselectedIcon = Icons.Outlined.Explore,
        ) {
            SearchKey
        },
        MainBottomTab(
            titleResId = R.string.nav_containers,
            selectedIcon = Icons.Filled.ViewInAr,
            unselectedIcon = Icons.Outlined.ViewInAr,
        ) {
            ContainersKey
        },
        MainBottomTab(
            titleResId = R.string.nav_images,
            selectedIcon = Icons.Filled.Layers,
            unselectedIcon = Icons.Outlined.Layers,
        ) {
            ImagesKey
        },
        MainBottomTab(
            titleResId = R.string.nav_settings,
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings,
        ) {
            SettingsKey
        }
    )

}