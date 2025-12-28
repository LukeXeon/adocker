package com.github.andock.daemon.engine

import com.github.andock.daemon.utils.suspendLazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class EngineModule {

    @Provides
    @Singleton
    @IntoSet
    fun initializer(version: PRootVersion) = suspendLazy {
        withTimeoutOrNull(1000) {
            version.get()
        }
    }
}