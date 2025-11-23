package com.adocker.runner

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for ADocker
 *
 * All initialization is handled by AndroidX App Startup.
 * See the following initializers in core.startup package:
 * - TimberInitializer: Timber logging setup
 * - ConfigInitializer: Config initialization
 * - RegistrySettingsInitializer: Registry settings initialization
 */
@HiltAndroidApp
class ADockerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // All initialization is now handled by AndroidX App Startup
        // See AndroidManifest.xml for InitializationProvider configuration
    }

    companion object {
        lateinit var instance: ADockerApplication
            private set
    }
}
