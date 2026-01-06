package com.github.andock.daemon.server.handlers

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExecSessionManager @Inject constructor() {
    private val sessions = ConcurrentHashMap<String, ExecSession>()

    data class ExecSession(
        val id: String,
        val containerId: String,
        val cmd: List<String>,
        val user: String?,
        val workingDir: String?,
        val tty: Boolean,
        var process: Process? = null,
        var running: Boolean = false,
        var exitCode: Int = 0
    )

    fun createSession(
        containerId: String,
        cmd: List<String>,
        user: String?,
        workingDir: String?,
        tty: Boolean
    ): String {
        val id = UUID.randomUUID().toString()
        val session = ExecSession(
            id = id,
            containerId = containerId,
            cmd = cmd,
            user = user,
            workingDir = workingDir,
            tty = tty
        )
        sessions[id] = session
        return id
    }

    fun getSession(id: String): ExecSession? = sessions[id]

    fun removeSession(id: String) {
        sessions.remove(id)
    }
}