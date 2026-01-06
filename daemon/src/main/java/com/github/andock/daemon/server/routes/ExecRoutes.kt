package com.github.andock.daemon.server.routes

import com.github.andock.daemon.server.handlers.ExecHandler
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
    fun routes(handler: ExecHandler): RoutingHttpHandler = routes(
        // Create an exec instance
        "/containers/{id}/exec" bind POST to { request ->
            handler.createExec(request)
        },

        // Start an exec instance
        "/exec/{id}/start" bind POST to { request ->
            handler.startExec(request)
        },

        // Resize an exec instance
        "/exec/{id}/resize" bind POST to {
            Response(NOT_IMPLEMENTED)
        },

        // Inspect an exec instance
        "/exec/{id}/json" bind GET to { request ->
            handler.inspectExec(request)
        }
    )
}
