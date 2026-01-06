package com.github.andock.daemon.engine

import com.github.andock.daemon.app.AppTask
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

@AppTask("loadEngineVersion")
suspend fun loadEngineVersion(
    version: PRootVersion,
    @Suppress("unused")
    @AppTask("logging")
    logging: Unit,
    @Suppress("unused")
    @AppTask("reporter")
    reporter: Unit
) {
    withTimeoutOrNull(200) {
        Timber.i(version.value.filterNotNull().first())
    }
}