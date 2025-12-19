package com.github.adocker.daemon.os

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber
import kotlin.system.exitProcess

class RemoteProcessBuilderService : IRemoteProcessBuilderService.Stub() {

    init {
        Timber.plant(Timber.DebugTree())
    }

    private val scope = CoroutineScope(
        SupervisorJob()
                + Dispatchers.IO
                + CoroutineExceptionHandler { _, e ->
            if (e !is CancellationException) {
                Timber.e(e)
            }
        }
    )

    override fun newProcess(
        cmd: Array<String>,
        env: Array<String>,
        dir: String?
    ): IRemoteProcessSession {
        return RemoteProcessSession(scope, cmd, env, dir)
    }

    override fun destroy() {
        exitProcess(0)
    }
}