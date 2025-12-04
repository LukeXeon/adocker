package com.github.adocker.daemon.os

import kotlin.system.exitProcess

class RemoteProcessBuilderService : IRemoteProcessBuilderService.Stub() {

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