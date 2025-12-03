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
 * Docker Network API routes.
 * @see <a href="https://docs.docker.com/engine/api/v1.45/#tag/Network">Docker API - Network</a>
 */
fun networkRoutes(): RoutingHttpHandler = routes(
    // List networks
    "/networks" bind GET to {
        // GET /networks
        // Query params: filters
        Response(NOT_IMPLEMENTED)
    },

    // Inspect a network
    "/networks/{id}" bind GET to {
        // GET /networks/{id}
        // Query params: verbose, scope
        Response(NOT_IMPLEMENTED)
    },

    // Remove a network
    "/networks/{id}" bind DELETE to {
        // DELETE /networks/{id}
        Response(NOT_IMPLEMENTED)
    },

    // Create a network
    "/networks/create" bind POST to {
        // POST /networks/create
        // Body: NetworkCreateRequest
        Response(NOT_IMPLEMENTED)
    },

    // Connect a container to a network
    "/networks/{id}/connect" bind POST to {
        // POST /networks/{id}/connect
        // Body: NetworkConnectRequest
        Response(NOT_IMPLEMENTED)
    },

    // Disconnect a container from a network
    "/networks/{id}/disconnect" bind POST to {
        // POST /networks/{id}/disconnect
        // Body: NetworkDisconnectRequest
        Response(NOT_IMPLEMENTED)
    },

    // Delete unused networks
    "/networks/prune" bind POST to {
        // POST /networks/prune
        // Query params: filters
        Response(NOT_IMPLEMENTED)
    }
)
