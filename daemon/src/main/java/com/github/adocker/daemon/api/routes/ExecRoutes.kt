package com.github.adocker.daemon.api.routes

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes

/**
 * Docker Exec API routes.
 * @see <a href="https://docs.docker.com/engine/api/v1.45/#tag/Exec">Docker API - Exec</a>
 */
@Module
@InstallIn(SingletonComponent::class)
object ExecRoutes {
    @IntoSet
    @Provides
    fun routes(): RoutingHttpHandler = routes(
        // Create an exec instance
        "/containers/{id}/exec" bind POST to {
            // POST /containers/{id}/exec
            // Body: ExecConfig
            Response(NOT_IMPLEMENTED)
        },

        // Start an exec instance
        "/exec/{id}/start" bind POST to {
            // POST /exec/{id}/start
            // Body: ExecStartConfig
            Response(NOT_IMPLEMENTED)
        },

        // Resize an exec instance
        "/exec/{id}/resize" bind POST to {
            // POST /exec/{id}/resize
            // Query params: h, w
            Response(NOT_IMPLEMENTED)
        },

        // Inspect an exec instance
        "/exec/{id}/json" bind GET to {
            // GET /exec/{id}/json
            Response(NOT_IMPLEMENTED)
        }
    )
}
