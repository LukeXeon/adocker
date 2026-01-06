package com.github.andock.daemon.server.routes

import com.github.andock.daemon.server.handlers.SystemHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import org.http4k.core.Method.GET
import org.http4k.core.Method.HEAD
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes

/**
 * Docker System API routes.
 * @see <a href="https://docs.docker.com/engine/api/v1.45/#tag/System">Docker API - System</a>
 */
@Module
@InstallIn(SingletonComponent::class)
object SystemRoutes {

    @IntoSet
    @Provides
    fun routes(handler: SystemHandler): RoutingHttpHandler = routes(
        // Check auth configuration
        "/auth" bind POST to {
            Response(NOT_IMPLEMENTED)
        },

        // Get system information
        "/info" bind GET to {
            handler.getInfo()
        },

        // Get version
        "/version" bind GET to {
            handler.getVersion()
        },

        // Ping
        "/_ping" bind GET to {
            handler.ping()
        },

        // Ping (HEAD)
        "/_ping" bind HEAD to {
            Response(OK)
                .header("Api-Version", "1.45")
                .header("Docker-Experimental", "false")
        },

        // Monitor events
        "/events" bind GET to {
            Response(NOT_IMPLEMENTED)
        },

        // Get data usage information
        "/system/df" bind GET to {
            handler.getDiskUsage()
        }
    )
}
