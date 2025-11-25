package com.github.adocker

import android.annotation.SuppressLint
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
        _instance = this
    }

    companion object {
        @Volatile
        private var _instance: Application? = null

        val instance: Application
            @SuppressLint("PrivateApi")
            get() {
                var application = _instance
                if (application == null) {
                    application = Class.forName("android.app.ActivityThread")
                        .getDeclaredMethod(
                            "currentApplication",
                            Application::class.java
                        ).apply {
                            isAccessible = true
                        }.invoke(null) as Application
                    _instance = application
                }
                return application
            }
    }
}
