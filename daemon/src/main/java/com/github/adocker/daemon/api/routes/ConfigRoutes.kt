package com.github.adocker.daemon.api.routes

import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes

/**
 * Docker Config API routes.
 * @see <a href="https://docs.docker.com/engine/api/v1.45/#tag/Config">Docker API - Config</a>
 */
fun configRoutes(): RoutingHttpHandler = routes(
    // List configs
    "/configs" bind GET to {
        // GET /configs
        // Query params: filters
        Response(NOT_IMPLEMENTED)
    },

    // Create a config
    "/configs/create" bind POST to {
        // POST /configs/create
        // Body: ConfigSpec
        Response(NOT_IMPLEMENTED)
    },

    // Inspect a config
    "/configs/{id}" bind GET to {
        // GET /configs/{id}
        Response(NOT_IMPLEMENTED)
    },

    // Delete a config
    "/configs/{id}" bind DELETE to {
        // DELETE /configs/{id}
        Response(NOT_IMPLEMENTED)
    },

    // Update a config
    "/configs/{id}/update" bind POST to {
        // POST /configs/{id}/update
        // Query params: version
        // Body: ConfigSpec
        Response(NOT_IMPLEMENTED)
    }
)
