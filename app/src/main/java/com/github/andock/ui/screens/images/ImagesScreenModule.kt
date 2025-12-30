package com.github.andock.ui.screens.images

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.outlined.Layers
import com.github.andock.R
import com.github.andock.ui.screens.main.MainBottomTab
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object ImagesScreenModule {
    @Provides
    @IntoSet
    fun tab() = MainBottomTab(
        route = Any::class,
        titleResId = R.string.nav_images,
        selectedIcon = Icons.Filled.Layers,
        unselectedIcon = Icons.Outlined.Layers,
        priority = 3,
    ) {

    }
}