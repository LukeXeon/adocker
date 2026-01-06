package com.github.andock.daemon.server

import com.github.andock.daemon.app.AppTask
import org.http4k.server.Http4kServer


@AppTask("server")
fun server(
    servers: Set<@JvmSuppressWildcards Http4kServer>,
    @Suppress("unused")
    @AppTask("logging")
    logging: Unit,
    @Suppress("unused")
    @AppTask("reporter")
    reporter: Unit
) {
    servers.forEach {
        it.start()
    }
}