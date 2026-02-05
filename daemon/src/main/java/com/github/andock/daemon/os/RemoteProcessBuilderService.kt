package com.github.andock.daemon.os

import timber.log.Timber
import kotlin.system.exitProcess

class RemoteProcessBuilderService : IRemoteProcessBuilderService.Stub() {

    init {
        Timber.plant(Timber.DebugTree())
    }

    override fun newProcess(
        cmd: Array<String>,
        env: Array<String>,
        dir: String?
    ): IRemoteProcessSession {
        return RemoteProcessSession(cmd, env, dir)
    }

    override fun destroy() {
        exitProcess(0)
    }
}