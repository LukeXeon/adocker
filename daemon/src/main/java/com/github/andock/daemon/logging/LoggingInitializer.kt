package com.github.andock.daemon.logging

import com.github.andock.daemon.app.AppContext
import com.github.andock.daemon.app.AppInitializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Initializes Timber logging library
 */
@Singleton
class LoggingInitializer @Inject constructor(
    private val appContext: AppContext,
) : AppInitializer.Task<Unit>() {
    override suspend fun create() {
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