/*
 * Copyright 2014-2025 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package com.github.adocker.daemon.aio.backend

import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.awaitClosed
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.server.cio.HttpRequestHandler
import io.ktor.server.cio.HttpServer
import io.ktor.server.cio.UnixSocketServerSettings
import io.ktor.server.cio.backend.ServerIncomingConnection
import io.ktor.server.cio.backend.startServerConnectionPipeline
import io.ktor.server.engine.DefaultUncaughtExceptionHandler
import io.ktor.server.engine.internal.ClosedChannelException
import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.io.IOException
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.time.Duration.Companion.seconds

private val LOGGER = KtorSimpleLogger("io.ktor.server.cio.backend.UnixSocketServer")

/**
 * Start an http server with [settings] invoking [handler] for every request
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.server.cio.backend.unixSocketServer)
 */
@OptIn(InternalAPI::class)
fun CoroutineScope.unixSocketServer(
    settings: UnixSocketServerSettings,
    handler: HttpRequestHandler
): HttpServer {
    val socket = CompletableDeferred<ServerSocket>()

    val serverLatch: CompletableJob = Job()

    val serverJob = launch(
        context = CoroutineName("server-root-$settings"),
        start = CoroutineStart.UNDISPATCHED
    ) {
        serverLatch.join()
    }

    val timeout = settings.connectionIdleTimeoutSeconds.seconds

    val acceptJob = launch(serverJob + CoroutineName("accept-$settings")) {
        val socketFile = Path(settings.socketPath)
        if (SystemFileSystem.exists(socketFile)) {
            SystemFileSystem.delete(socketFile)
        }
        val serverSocket = UnixServerSocketImpl(settings.socketPath)

        serverSocket.use { server ->
            socket.complete(server)

            val exceptionHandler = coroutineContext[CoroutineExceptionHandler]
                ?: DefaultUncaughtExceptionHandler(LOGGER)

            val connectionScope = CoroutineScope(
                coroutineContext +
                        SupervisorJob(serverJob) +
                        exceptionHandler +
                        CoroutineName("request")
            )

            try {
                while (true) {
                    val client: Socket = try {
                        server.accept()
                    } catch (cause: IOException) {
                        LOGGER.trace("Failed to accept connection", cause)
                        continue
                    }

                    val connection = ServerIncomingConnection(
                        client.openReadChannel(),
                        client.openWriteChannel(),
                        client.remoteAddress.toNetworkAddress(),
                        client.localAddress.toNetworkAddress()
                    )

                    val clientJob = connectionScope.startServerConnectionPipeline(
                        connection,
                        timeout,
                        handler
                    )

                    clientJob.invokeOnCompletion {
                        client.close()
                    }
                }
            } catch (closed: ClosedChannelException) {
                LOGGER.trace("Server socket closed", closed)
                coroutineContext.cancel()
            } finally {
                server.close()
                server.awaitClosed()
                connectionScope.coroutineContext.cancel()
            }
        }
    }

    acceptJob.invokeOnCompletion { cause ->
        cause?.let { socket.completeExceptionally(it) }
        serverLatch.complete()
    }
    return HttpServer(serverJob, acceptJob, socket)
}
