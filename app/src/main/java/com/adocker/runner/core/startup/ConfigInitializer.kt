package com.adocker.runner.core.startup

import android.content.Context
import androidx.startup.Initializer
import com.adocker.runner.core.config.Config
import timber.log.Timber

/**
 * Initializes Config
 */
class ConfigInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        Config.init(context)
        Timber.d("Config initialized")
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        // Depends on Timber being initialized first
        return listOf(TimberInitializer::class.java)
    }
}
