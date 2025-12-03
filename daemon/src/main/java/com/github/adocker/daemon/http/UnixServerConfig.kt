package com.github.adocker.daemon.http

import org.http4k.core.HttpHandler
import org.http4k.server.Http4kServer
import org.http4k.server.ServerConfig

/**
 * HTTP server configuration using android.net.LocalServerSocket as the backend.
 * Implements HTTP/1.1 protocol over Unix domain socket connections.
 *
 * @param socketName The name of the Unix socket (Android abstract namespace)
 */
class UnixServerConfig(
    private val socketName: String
) : ServerConfig {

    override fun toServer(http: HttpHandler): Http4kServer {
        return UnixHttp4kServer(socketName, http)
    }
}
