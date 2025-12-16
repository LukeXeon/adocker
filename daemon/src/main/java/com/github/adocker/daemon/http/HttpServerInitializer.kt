package com.github.adocker.daemon.http

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.startup.Initializer
import com.github.adocker.daemon.app.AppGlobals

class HttpServerInitializer : Initializer<Unit>, Runnable {

    override fun create(context: Context) {
        Handler(Looper.getMainLooper()).post(this)
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