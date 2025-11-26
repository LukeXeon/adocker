package com.github.adocker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for ADocker
 *
 * All initialization is handled by AndroidX App Startup.
 * See the following initializers in core.startup package:
 * - TimberInitializer: Timber logging setup
 */
@HiltAndroidApp
class ADockerApplication : Application()
