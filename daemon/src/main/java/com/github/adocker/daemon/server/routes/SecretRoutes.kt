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
 * Docker Secret API routes.
 * @see <a href="https://docs.docker.com/engine/api/v1.45/#tag/Secret">Docker API - Secret</a>
 */
@Module
@InstallIn(SingletonComponent::class)
object SecretRoutes {
    @IntoSet
    @Provides
    fun routes(): RoutingHttpHandler = routes(
        // List secrets
        "/secrets" bind GET to {
            // GET /secrets
            // Query params: filters
            Response(NOT_IMPLEMENTED)
        },

        // Create a secret
        "/secrets/create" bind POST to {
            // POST /secrets/create
            // Body: SecretSpec
            Response(NOT_IMPLEMENTED)
        },

        // Inspect a secret
        "/secrets/{id}" bind GET to {
            // GET /secrets/{id}
            Response(NOT_IMPLEMENTED)
        },

        // Delete a secret
        "/secrets/{id}" bind DELETE to {
            // DELETE /secrets/{id}
            Response(NOT_IMPLEMENTED)
        },

        // Update a secret
        "/secrets/{id}/update" bind POST to {
            // POST /secrets/{id}/update
            // Query params: version
            // Body: SecretSpec
            Response(NOT_IMPLEMENTED)
        }
    )
}
