package com.github.adocker.daemon.server.routes

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
    fun routes(): RoutingHttpHandler = routes(
        // Check auth configuration
        "/auth" bind POST to {
            // POST /auth
            // Body: AuthConfig
            Response(NOT_IMPLEMENTED)
        },

        // Get system information
        "/info" bind GET to {
            // GET /info
            Response(NOT_IMPLEMENTED)
        },

        // Get version
        "/version" bind GET to {
            // GET /version
            Response(NOT_IMPLEMENTED)
        },

        // Ping
        "/_ping" bind GET to {
            // GET /_ping
            Response(NOT_IMPLEMENTED)
        },

        // Ping (HEAD)
        "/_ping" bind HEAD to {
            // HEAD /_ping
            Response(NOT_IMPLEMENTED)
        },

        // Monitor events
        "/events" bind GET to {
            // GET /events
            // Query params: since, until, filters
            Response(NOT_IMPLEMENTED)
        },

        // Get data usage information
        "/system/df" bind GET to {
            // GET /system/df
            // Query params: type
            Response(NOT_IMPLEMENTED)
        }
    )
}
