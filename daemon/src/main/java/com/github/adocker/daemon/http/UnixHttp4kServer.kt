package com.github.adocker.daemon.http

import android.net.LocalServerSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.http4k.core.HttpHandler
import org.http4k.server.Http4kServer
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Http4kServer implementation using android.net.LocalServerSocket as the backend.
 * Implements HTTP/1.1 protocol over Unix domain socket connections.
 */
class UnixHttp4kServer(
    private val socketName: String,
    httpHandler: HttpHandler
) : Http4kServer {

    private var serverSocket: LocalServerSocket? = null
    private val running = AtomicBoolean(false)
    private var scope: CoroutineScope? = null
    private val connectionHandler = ConnectionHandler(httpHandler)

    override fun port(): Int = -1 // Unix sockets don't have ports

    override fun start(): Http4kServer {
        if (running.getAndSet(true)) {
            Timber.w("Unix server already running on socket $socketName")
            return this
        }

        serverSocket = LocalServerSocket(socketName)
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        Timber.i("Unix server started on socket $socketName")

        scope?.launch {
            acceptConnections()
        }

        return this
    }

    private fun acceptConnections() {
        val socket = serverSocket ?: return
        val currentScope = scope ?: return

        while (running.get() && currentScope.isActive) {
            try {
                val clientSocket = socket.accept()
                if (clientSocket != null) {
                    currentScope.launch {
                        val connection = UnixClientConnection(clientSocket)
                        connectionHandler.handle(connection)
                    }
                }
            } catch (e: Exception) {
                if (running.get()) {
                    Timber.e(e, "Error accepting Unix socket connection")
                }
            }
        }
    }

    override fun stop(): Http4kServer {
        if (!running.getAndSet(false)) {
            return this
        }

        Timber.i("Stopping Unix server on socket $socketName")

        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Timber.e(e, "Error closing Unix server socket")
        }

        scope?.cancel()
        serverSocket = null
        scope = null

        return this
    }
}
