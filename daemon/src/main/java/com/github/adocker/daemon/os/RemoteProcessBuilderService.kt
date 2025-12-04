package com.github.adocker.daemon.os

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlin.system.exitProcess

class RemoteProcessBuilderService : IRemoteProcessBuilderService.Stub() {

    @OptIn(DelicateCoroutinesApi::class)
    override fun newProcess(
        cmd: Array<String>,
        env: Array<String>,
        dir: String
    ): IRemoteProcessSession {
        return RemoteProcessSession(cmd, env, dir)
    }

    override fun destroy() {
        exitProcess(0)
    }
}