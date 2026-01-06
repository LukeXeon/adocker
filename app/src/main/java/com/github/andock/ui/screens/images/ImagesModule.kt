package com.github.andock.ui.screens.images

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.outlined.Layers
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
object ImagesModule {
    @Provides
    @IntoMap
    @ClassKey(ImagesRoute::class)
    fun tab() = MainBottomTab(
        titleResId = R.string.nav_images,
        selectedIcon = Icons.Filled.Layers,
        unselectedIcon = Icons.Outlined.Layers,
    ) {
        ImagesRoute
    }

    @Provides
    @IntoMap
    @ClassKey(ImagesRoute::class)
    fun screen() = Screen {
        ImagesScreen()
    }

    @Provides
    @IntoMap
    @ClassKey(ImageDetailRoute::class)
    fun detailScreen() = Screen {
        ImageDetailScreen()
    }

    @Provides
    @IntoMap
    @ClassKey(ImageTagsRoute::class)
    fun selectScreen() = Screen {
        ImageTagsScreen()
    }
}