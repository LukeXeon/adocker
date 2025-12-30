package com.github.andock.ui.screens.registries

import com.github.andock.ui.screens.Screen
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
object RegistriesModule {

    @Provides
    @IntoMap
    @ClassKey(RegistriesRoute::class)
    fun screen() = Screen {
        RegistriesScreen(it)
    }

    @Provides
    @IntoMap
    @ClassKey(AddMirrorRoute::class)
    fun addMirrorScreen() = Screen {
        AddMirrorScreen()
    }
}