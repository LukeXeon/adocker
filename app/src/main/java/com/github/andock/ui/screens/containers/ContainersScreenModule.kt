package com.github.andock.ui.screens.containers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.outlined.ViewInAr
import com.github.andock.R
import com.github.andock.ui.screens.main.MainBottomTab
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object ContainersScreenModule {
    @Provides
    @IntoSet
    fun tab() = MainBottomTab(
        route = Any::class,
        titleResId = R.string.nav_containers,
        selectedIcon = Icons.Filled.ViewInAr,
        unselectedIcon = Icons.Outlined.ViewInAr,
        priority = 2
    ) {

    }
}