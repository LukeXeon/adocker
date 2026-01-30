package com.github.andock.daemon.http

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.job
import org.http4k.core.HttpHandler
import org.http4k.server.Http4kServer
import org.http4k.server.ServerConfig
import javax.inject.Singleton

/**
 * HTTP server configuration using java.net.ServerSocket as the backend.
 * Implements HTTP/1.1 protocol over TCP connections.
 *
 * @param port The TCP port to listen on
 */
class TcpServerConfig @AssistedInject constructor(
    @Assisted("port")
    private val port: Int,
    private val scope: CoroutineScope
) : ServerConfig {

    override fun toServer(http: HttpHandler): Http4kServer {
        return TcpHttp4kServer(
            port,
            http,
            scope.coroutineContext.job
        )
    }

    @Singleton
    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("port")
            port: Int
        ): TcpServerConfig
    }
}
