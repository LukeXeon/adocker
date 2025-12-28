package com.github.andock.daemon.engine

import com.github.andock.daemon.utils.SuspendLazy
import com.github.andock.daemon.utils.suspendLazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EngineModule {

    @Provides
    @Singleton
    @IntoSet
    fun initializer(version: PRootVersion): SuspendLazy<*> = suspendLazy {
        withTimeoutOrNull(1000) {
            Timber.i(version.value.filterNotNull().first())
        }
    }
}