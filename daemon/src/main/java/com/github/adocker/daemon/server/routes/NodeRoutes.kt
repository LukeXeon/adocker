package com.github.adocker.daemon.server.routes

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes

/**
 * Docker Node API routes.
 * @see <a href="https://docs.docker.com/engine/api/v1.45/#tag/Node">Docker API - Node</a>
 */
@Module
@InstallIn(SingletonComponent::class)
object NodeRoutes {
    @IntoSet
    @Provides
    fun routes(): RoutingHttpHandler = routes(
        // List nodes
        "/nodes" bind GET to {
            // GET /nodes
            // Query params: filters
            Response(NOT_IMPLEMENTED)
        },

        // Inspect a node
        "/nodes/{id}" bind GET to {
            // GET /nodes/{id}
            Response(NOT_IMPLEMENTED)
        },

        // Delete a node
        "/nodes/{id}" bind DELETE to {
            // DELETE /nodes/{id}
            // Query params: force
            Response(NOT_IMPLEMENTED)
        },

        // Update a node
        "/nodes/{id}/update" bind POST to {
            // POST /nodes/{id}/update
            // Query params: version
            // Body: NodeSpec
            Response(NOT_IMPLEMENTED)
        }
    )
}
