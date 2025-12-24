package com.github.andock.daemon.server

import android.content.Context
import androidx.startup.Initializer
import com.github.andock.daemon.app.AppGlobals
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.EmptyCoroutineContext

class ApiServerInitializer : Initializer<Unit>, Runnable {

    override fun create(context: Context) {
        Dispatchers.Main.dispatch(EmptyCoroutineContext, this)
    }

    override fun run() {
        AppGlobals.servers().forEach {
            it.start()
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}