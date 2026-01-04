package com.github.andock.daemon.os

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber
import kotlin.coroutines.CoroutineContext
import kotlin.system.exitProcess

class RemoteProcessBuilderService : IRemoteProcessBuilderService.Stub(),
    CoroutineExceptionHandler {

    init {
        Timber.plant(Timber.DebugTree())
    }

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + this
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

    @Deprecated("Coroutine use only", level = DeprecationLevel.HIDDEN)
    override fun handleException(
        context: CoroutineContext,
        exception: Throwable
    ) {
        if (exception !is CancellationException) {
            Timber.d(exception)
        }
    }

    @Deprecated("Coroutine use only", level = DeprecationLevel.HIDDEN)
    override val key: CoroutineContext.Key<*>
        get() = CoroutineExceptionHandler
}