package com.github.andock.daemon.logging

import android.app.Application
import com.github.andock.common.isDebuggable
import com.github.andock.startup.Task
import timber.log.Timber


@Task("logging")
fun logging(appContext: Application) {
    if (appContext.isDebuggable) {
        Timber.plant(Timber.DebugTree())
    }
    Timber.d("Timber initialized")
}
