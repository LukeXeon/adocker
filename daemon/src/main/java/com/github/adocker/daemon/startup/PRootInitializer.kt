package com.github.adocker.daemon.startup

import android.content.Context
import androidx.startup.Initializer
import com.github.adocker.daemon.di.AppGlobals
import kotlinx.coroutines.launch

class PRootInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        val globals = AppGlobals()
        globals.scope().launch {
            globals.engine()
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>?>?> {
        return listOf(TimberInitializer::class.java)
    }
}