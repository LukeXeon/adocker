package com.github.andock.daemon.server.routes

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes

/**
 * Docker Task API routes.
 * @see <a href="https://docs.docker.com/engine/api/v1.45/#tag/Task">Docker API - Task</a>
 */
@Module
@InstallIn(SingletonComponent::class)
object TaskRoutes {
    @IntoSet
    @Provides
    fun routes(): RoutingHttpHandler = routes(
        // List tasks
        "/tasks" bind GET to {
            // GET /tasks
            // Query params: filters
            Response(NOT_IMPLEMENTED)
        },

        // Inspect a task
        "/tasks/{id}" bind GET to {
            // GET /tasks/{id}
            Response(NOT_IMPLEMENTED)
        },

        // Get task logs
        "/tasks/{id}/logs" bind GET to {
            // GET /tasks/{id}/logs
            // Query params: details, follow, stdout, stderr, since, timestamps, tail
            Response(NOT_IMPLEMENTED)
        }
    )
}
