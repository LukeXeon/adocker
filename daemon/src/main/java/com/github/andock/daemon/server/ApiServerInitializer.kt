package com.github.andock.daemon.server

import com.github.andock.daemon.app.AppInitializer
import org.http4k.server.Http4kServer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiServerInitializer @Inject constructor(
    private val servers: Set<@JvmSuppressWildcards Http4kServer>
) : AppInitializer.Task<Unit>() {
    override fun create() {
        servers.forEach {
            it.start()
        }
    }
}