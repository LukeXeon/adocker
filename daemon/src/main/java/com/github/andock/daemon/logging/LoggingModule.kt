package com.github.andock.daemon.logging

import com.github.andock.daemon.app.AppContext
import com.github.andock.daemon.utils.SuspendLazy
import com.github.andock.daemon.utils.suspendLazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LoggingModule {
    @Provides
    @Singleton
    @IntoSet
    fun initializer(appContext: AppContext): SuspendLazy<*> = suspendLazy {
        // Check if app is debuggable
        if (appContext.isDebuggable) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.d("Timber initialized")
        withContext(Dispatchers.IO) {
            appContext.logDir.deleteRecursively()
            appContext.logDir.mkdirs()
        }
    }
}