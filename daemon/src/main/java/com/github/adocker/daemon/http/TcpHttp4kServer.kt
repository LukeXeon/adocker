package com.github.adocker.daemon.http

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.http4k.core.HttpHandler
import org.http4k.server.Http4kServer
import timber.log.Timber
import java.io.IOException
import java.net.ServerSocket

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
    private var scope: CoroutineScope? = null

    @Synchronized
    override fun port(): Int = serverSocket?.localPort ?: -1

    @Synchronized
    override fun start(): Http4kServer {
        if (scope?.isActive == true) {
            Timber.w("TCP server already running on port $port")
            return this
        }
        val serverSocket = ServerSocket(port)
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        Timber.i("TCP server started on port ${port()}")
        scope.launch {
            try {
                awaitCancellation()
            } finally {
                try {
                    serverSocket.close()
                } catch (e: IOException) {
                    Timber.e(e, "Error closing server socket")
                }
            }
        }
        scope.launch {
            while (!serverSocket.isClosed && isActive) {
                try {
                    val clientSocket = serverSocket.accept()
                    scope.launch {
                        processor.process(TcpClientConnection(clientSocket))
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        Timber.e(e, "Error accepting TCP connection")
                    }
                }
            }
        }
        this.serverSocket = serverSocket
        this.scope = scope
        return this
    }

    @Synchronized
    override fun stop(): Http4kServer {
        val scope = this.scope
        if (scope == null || scope.isActive) {
            return this
        }
        Timber.i("Stopping TCP server on port $port")
        scope.cancel()
        this.serverSocket = null
        this.scope = null
        return this
    }
}
