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
 * Docker Service API routes.
 * @see <a href="https://docs.docker.com/engine/api/v1.45/#tag/Service">Docker API - Service</a>
 */
@Module
@InstallIn(SingletonComponent::class)
object ServiceRoutes {
    @IntoSet
    @Provides
    fun routes(): RoutingHttpHandler = routes(
        // List services
        "/services" bind GET to {
            // GET /services
            // Query params: filters, status
            Response(NOT_IMPLEMENTED)
        },

        // Create a service
        "/services/create" bind POST to {
            // POST /services/create
            // Headers: X-Registry-Auth
            // Body: ServiceSpec
            Response(NOT_IMPLEMENTED)
        },

        // Inspect a service
        "/services/{id}" bind GET to {
            // GET /services/{id}
            // Query params: insertDefaults
            Response(NOT_IMPLEMENTED)
        },

        // Delete a service
        "/services/{id}" bind DELETE to {
            // DELETE /services/{id}
            Response(NOT_IMPLEMENTED)
        },

        // Update a service
        "/services/{id}/update" bind POST to {
            // POST /services/{id}/update
            // Query params: version, registryAuthFrom, rollback
            // Headers: X-Registry-Auth
            // Body: ServiceSpec
            Response(NOT_IMPLEMENTED)
        },

        // Get service logs
        "/services/{id}/logs" bind GET to {
            // GET /services/{id}/logs
            // Query params: details, follow, stdout, stderr, since, timestamps, tail
            Response(NOT_IMPLEMENTED)
        }
    )
}
