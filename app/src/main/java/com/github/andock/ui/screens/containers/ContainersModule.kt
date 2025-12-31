package com.github.andock.ui.screens.containers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.outlined.ViewInAr
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
object ContainersModule {
    @Provides
    @IntoMap
    @ClassKey(ContainersRoute::class)
    fun tab() = MainBottomTab(
        titleResId = R.string.nav_containers,
        selectedIcon = Icons.Filled.ViewInAr,
        unselectedIcon = Icons.Outlined.ViewInAr,
    ) {
        ContainersRoute
    }

    @Provides
    @IntoMap
    @ClassKey(ContainersRoute::class)
    fun screen() = Screen {
        ContainersScreen()
    }

    @Provides
    @IntoMap
    @ClassKey(ContainerDetailRoute::class)
    fun detailScreen() = Screen {
        ContainerDetailScreen()
    }

    @Provides
    @IntoMap
    @ClassKey(ContainerCreateRoute::class)
    fun createScreen() = Screen {
        ContainerCreateScreen()
    }
}