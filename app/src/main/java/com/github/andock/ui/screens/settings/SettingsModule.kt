package com.github.andock.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Settings
import com.github.andock.R
import com.github.andock.ui.screens.Screen
import com.github.andock.ui.screens.main.MainBottomTab
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {
    @Provides
    @IntoMap
    @ClassKey(SettingsRoute::class)
    fun tab() = MainBottomTab(
        titleResId = R.string.nav_settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        priority = 4
    ) {

    }

    @Provides
    @IntoMap
    @ClassKey(SettingsRoute::class)
    fun screen() = Screen {
        SettingsScreen()
    }
}