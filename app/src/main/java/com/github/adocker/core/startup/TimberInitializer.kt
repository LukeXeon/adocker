package com.github.adocker.core.startup

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.startup.Initializer
import timber.log.Timber

/**
 * Initializes Timber logging library
 */
class TimberInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        // Check if app is debuggable
        val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.d("Timber initialized")
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        // No dependencies
        return emptyList()
    }
}
