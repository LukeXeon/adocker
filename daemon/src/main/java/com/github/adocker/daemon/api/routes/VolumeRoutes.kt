package com.github.adocker.daemon.api.routes

import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Method.PUT
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes

/**
 * Docker Volume API routes.
 * @see <a href="https://docs.docker.com/engine/api/v1.45/#tag/Volume">Docker API - Volume</a>
 */
fun volumeRoutes(): RoutingHttpHandler = routes(
    // List volumes
    "/volumes" bind GET to {
        // GET /volumes
        // Query params: filters
        Response(NOT_IMPLEMENTED)
    },

    // Create a volume
    "/volumes/create" bind POST to {
        // POST /volumes/create
        // Body: VolumeCreateOptions
        Response(NOT_IMPLEMENTED)
    },

    // Inspect a volume
    "/volumes/{name}" bind GET to {
        // GET /volumes/{name}
        Response(NOT_IMPLEMENTED)
    },

    // Update a volume
    "/volumes/{name}" bind PUT to {
        // PUT /volumes/{name}
        // Query params: version
        // Body: VolumeUpdateBody
        Response(NOT_IMPLEMENTED)
    },

    // Remove a volume
    "/volumes/{name}" bind DELETE to {
        // DELETE /volumes/{name}
        // Query params: force
        Response(NOT_IMPLEMENTED)
    },

    // Delete unused volumes
    "/volumes/prune" bind POST to {
        // POST /volumes/prune
        // Query params: filters
        Response(NOT_IMPLEMENTED)
    }
)
