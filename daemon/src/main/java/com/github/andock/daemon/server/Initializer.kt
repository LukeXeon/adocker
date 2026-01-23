package com.github.andock.daemon.server

import com.github.andock.startup.Task
import org.http4k.server.Http4kServer


@Task("startServers")
fun startServers(
    servers: Set<@JvmSuppressWildcards Http4kServer>,
    @Suppress("unused")
    @Task("logging")
    logging: Unit,
    @Suppress("unused")
    @Task("reporter")
    reporter: Unit
) {
    servers.forEach {
        it.start()
    }
}