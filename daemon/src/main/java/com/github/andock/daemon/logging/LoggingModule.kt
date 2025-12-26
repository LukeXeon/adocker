package com.github.andock.daemon.logging

import com.github.andock.daemon.app.AppInitializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object LoggingModule {
    @Provides
    @IntoSet
    fun initializer(initializer: LoggingInitializer): AppInitializer.Task<*> = initializer
}