package com.github.andock.ui.screens.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Home
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
object HomeModule {
    @Provides
    @IntoMap
    @ClassKey(HomeRoute::class)
    fun tab() = MainBottomTab(
        titleResId = R.string.nav_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        priority = 0
    ) { navController ->
        navController.navigate(HomeRoute()) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    @Provides
    @IntoMap
    @ClassKey(HomeRoute::class)
    fun screen() = Screen {
        HomeScreen()
    }
}