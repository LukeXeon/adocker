package com.github.andock.ui.screens.terminal

import androidx.navigation.toRoute
import com.github.andock.ui.screens.Screen
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
object TerminalModule {
    @Provides
    @IntoMap
    @ClassKey(TerminalRoute::class)
    fun screen() = Screen {
        TerminalScreen(it.toRoute<TerminalRoute>().containerId)
    }
}