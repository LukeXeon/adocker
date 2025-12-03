package com.github.adocker.daemon.http

import android.net.LocalServerSocket
import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.net.LocalSocketAddress.Namespace
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
import java.io.File
import java.io.IOException

/**
 * Http4kServer implementation using android.net.LocalServerSocket as the backend.
 * Implements HTTP/1.1 protocol over Unix domain socket connections.
 */
class UnixHttp4kServer(
    private val name: String,
    private val namespace: Namespace,
    httpHandler: HttpHandler
) : Http4kServer {

    companion object {
        private fun LocalSocket.closeSafely() {
            try {
                close()
            } catch (e: IOException) {
                Timber.e(e, "Error closing Unix server socket")
            }
        }
    }

    private val processor = HttpProcessor(httpHandler)
    private var scope: CoroutineScope? = null

    override fun port(): Int = -1 // Unix sockets don't have ports

    @Synchronized
    override fun start(): Http4kServer {
        if (scope?.isActive == true) {
            Timber.w("Unix server already running on socket $name")
            return this
        }
        if (namespace == Namespace.FILESYSTEM) {
            val socketFile = File(name)
            if (socketFile.exists() && !socketFile.delete()) {
                throw IOException("Failed to delete old socket file $name")
            }
        }
        val localSocket = LocalSocket(LocalSocket.SOCKET_STREAM)
        val serverSocket = try {
            localSocket.bind(
                LocalSocketAddress(
                    name,
                    namespace
                )
            )
            LocalServerSocket(localSocket.fileDescriptor)
        } catch (e: IOException) {
            localSocket.closeSafely()
            throw e
        }
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        Timber.i("Unix server started on socket ${namespace}:${name}")
        scope.launch {
            try {
                awaitCancellation()
            } finally {
                localSocket.closeSafely()
            }
        }
        scope.launch {
            while (isActive) {
                try {
                    val clientSocket = serverSocket.accept()
                    scope.launch {
                        processor.process(UnixClientConnection(clientSocket))
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        Timber.e(e, "Error accepting Unix socket connection")
                    }
                }
            }
        }
        this.scope = scope
        return this
    }


    @Synchronized
    override fun stop(): Http4kServer {
        val scope = scope
        if (scope == null || !scope.isActive) {
            return this
        }
        Timber.i("Stopping Unix server on socket $name")
        scope.cancel()
        this.scope = null
        return this
    }
}
