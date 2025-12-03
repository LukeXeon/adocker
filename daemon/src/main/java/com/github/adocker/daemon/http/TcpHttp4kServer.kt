package com.github.adocker.daemon.http

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.http4k.core.HttpHandler
import org.http4k.server.Http4kServer
import timber.log.Timber
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Http4kServer implementation using java.net.ServerSocket as the backend.
 * Implements HTTP/1.1 protocol over TCP connections.
 */
class TcpHttp4kServer(
    private val port: Int,
    httpHandler: HttpHandler
) : Http4kServer {
    private val processor = HttpProcessor(httpHandler)
    private var serverSocket: ServerSocket? = null
    private val running = AtomicBoolean(false)
    private var scope: CoroutineScope? = null

    override fun port(): Int = serverSocket?.localPort ?: port

    override fun start(): Http4kServer {
        if (running.getAndSet(true)) {
            Timber.w("TCP server already running on port $port")
            return this
        }

        serverSocket = ServerSocket(port)
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        Timber.i("TCP server started on port ${port()}")

        scope?.launch {
            acceptConnections()
        }

        return this
    }

    private fun acceptConnections() {
        val socket = serverSocket ?: return
        val currentScope = scope ?: return

        while (running.get() && !socket.isClosed && currentScope.isActive) {
            try {
                val clientSocket = socket.accept()
                currentScope.launch {
                    processor.process(TcpClientConnection(clientSocket))
                }
            } catch (e: Exception) {
                if (running.get()) {
                    Timber.e(e, "Error accepting TCP connection")
                }
            }
        }
    }

    override fun stop(): Http4kServer {
        if (!running.getAndSet(false)) {
            return this
        }

        Timber.i("Stopping TCP server on port $port")

        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Timber.e(e, "Error closing server socket")
        }

        scope?.cancel()
        serverSocket = null
        scope = null

        return this
    }
}
