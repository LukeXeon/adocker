package com.github.andock.daemon.logging

import com.github.andock.daemon.app.AppContext
import com.github.andock.daemon.app.AppTask
import timber.log.Timber


@AppTask("logging")
fun logging(appContext: AppContext) {
    if (appContext.isDebuggable) {
        Timber.plant(Timber.DebugTree())
    }
    Timber.d("Timber initialized")
}
