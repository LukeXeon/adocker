package com.github.andock.daemon.http

import android.net.LocalSocketAddress.Namespace
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.job
import org.http4k.core.HttpHandler
import org.http4k.server.Http4kServer
import org.http4k.server.ServerConfig
import javax.inject.Singleton

/**
 * HTTP server configuration using android.net.LocalServerSocket as the backend.
 * Implements HTTP/1.1 protocol over Unix domain socket connections.
 *
 * @param name The name of the Unix socket
 * @param namespace The namespace of the Unix socket
 */

class UnixServerConfig @AssistedInject constructor(
    private val name: String,
    private val namespace: Namespace,
    private val scope: CoroutineScope
) : ServerConfig {

    override fun toServer(http: HttpHandler): Http4kServer {
        return UnixHttp4kServer(
            name,
            namespace,
            http,
            scope.coroutineContext.job
        )
    }

    @Singleton
    @AssistedFactory
    interface Factory {
        fun create(
            name: String,
            namespace: Namespace
        ): UnixServerConfig
    }
}
