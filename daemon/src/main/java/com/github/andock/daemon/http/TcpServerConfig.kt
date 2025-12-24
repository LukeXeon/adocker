package com.github.andock.daemon.http

import org.http4k.core.HttpHandler
import org.http4k.server.Http4kServer
import org.http4k.server.ServerConfig

/**
 * HTTP server configuration using java.net.ServerSocket as the backend.
 * Implements HTTP/1.1 protocol over TCP connections.
 *
 * @param port The TCP port to listen on
 */
class TcpServerConfig(
    private val port: Int
) : ServerConfig {

    override fun toServer(http: HttpHandler): Http4kServer {
        return TcpHttp4kServer(port, http)
    }
}
