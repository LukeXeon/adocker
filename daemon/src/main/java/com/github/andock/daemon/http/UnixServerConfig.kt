package com.github.andock.daemon.http

import android.net.LocalSocketAddress.Namespace
import org.http4k.core.HttpHandler
import org.http4k.server.Http4kServer
import org.http4k.server.ServerConfig

/**
 * HTTP server configuration using android.net.LocalServerSocket as the backend.
 * Implements HTTP/1.1 protocol over Unix domain socket connections.
 *
 * @param name The name of the Unix socket
 * @param namespace The namespace of the Unix socket
 */
class UnixServerConfig(
    private val name: String,
    private val namespace: Namespace
) : ServerConfig {

    override fun toServer(http: HttpHandler): Http4kServer {
        return UnixHttp4kServer(name, namespace, http)
    }
}
