package com.github.andock.daemon.logging

import android.app.Application
import android.content.pm.ApplicationInfo
import com.github.andock.daemon.app.AppInitializer
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Initializes Timber logging library
 */
@Singleton
class TimberInitializer @Inject constructor(
    private val application: Application,
) : AppInitializer.Task<Unit>() {
    override fun create() {
        // Check if app is debuggable
        val isDebuggable =
            (application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.d("Timber initialized")
    }
}