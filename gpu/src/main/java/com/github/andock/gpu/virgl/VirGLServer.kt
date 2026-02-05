package com.github.andock.gpu.virgl

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VirGL Server Manager
 *
 * Manages the vtest protocol server lifecycle.
 * Provides Unix socket-based server for VirGL clients.
 *
 * Usage:
 * ```kotlin
 * @Inject lateinit var virglServer: VirGLServer
 *
 * // Start server
 * val job = virglServer.startServer()
 *
 * // Stop server
 * virglServer.stopServer()
 * ```
 */
@Singleton
class VirGLServer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var serverJob: Job? = null
    private val socketPath = File(context.filesDir, "virgl-vtest.sock").absolutePath

    /**
     * Start vtest server
     *
     * The server runs in a background coroutine on Dispatchers.IO.
     * The server will listen on a Unix socket and handle multiple clients
     * using epoll multiplexing.
     *
     * @return Job that completes when server exits
     */
    fun startServer(): Job {
        if (serverJob?.isActive == true) {
            Timber.w("VirGL server already running")
            return serverJob!!
        }

        Timber.i("Starting VirGL server on socket: $socketPath")

        return CoroutineScope(Dispatchers.IO).launch {
            try {
                val ret = VirGLNative.nativeStartServer(socketPath)
                if (ret != 0) {
                    Timber.e("VirGL server failed with code: $ret")
                    throw RuntimeException("VirGL server failed: $ret")
                }
                Timber.i("VirGL server exited normally")
            } catch (e: Exception) {
                Timber.e(e, "VirGL server error")
                throw e
            }
        }.also { serverJob = it }
    }

    /**
     * Stop vtest server
     *
     * Signals the server to stop and waits for the event loop to exit.
     */
    fun stopServer() {
        if (serverJob?.isActive != true) {
            Timber.w("VirGL server not running")
            return
        }

        Timber.i("Stopping VirGL server")
        VirGLNative.nativeStopServer()

        // Job will complete when server exits
        serverJob?.cancel()
        serverJob = null
    }

    /**
     * Get socket path for clients to connect
     */
    fun getSocketPath(): String = socketPath

    /**
     * Check if server is running
     */
    fun isRunning(): Boolean = serverJob?.isActive == true
}
