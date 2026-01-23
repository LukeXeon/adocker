package com.github.andock.daemon.engine

import com.github.andock.startup.Task
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

@Task("loadEngineVersion")
suspend fun loadEngineVersion(
    version: PRootVersion,
    @Suppress("unused")
    @Task("logging")
    logging: Unit,
    @Suppress("unused")
    @Task("reporter")
    reporter: Unit
) {
    withTimeoutOrNull(100) {
        Timber.i(version.value.filterNotNull().first())
    }
}