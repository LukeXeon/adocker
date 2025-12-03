package com.github.adocker.daemon.api.routes

import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes

/**
 * Docker Swarm API routes.
 * @see <a href="https://docs.docker.com/engine/api/v1.45/#tag/Swarm">Docker API - Swarm</a>
 */
fun swarmRoutes(): RoutingHttpHandler = routes(
    // Inspect swarm
    "/swarm" bind GET to {
        // GET /swarm
        Response(NOT_IMPLEMENTED)
    },

    // Initialize a new swarm
    "/swarm/init" bind POST to {
        // POST /swarm/init
        // Body: SwarmInitRequest
        Response(NOT_IMPLEMENTED)
    },

    // Join an existing swarm
    "/swarm/join" bind POST to {
        // POST /swarm/join
        // Body: SwarmJoinRequest
        Response(NOT_IMPLEMENTED)
    },

    // Leave a swarm
    "/swarm/leave" bind POST to {
        // POST /swarm/leave
        // Query params: force
        Response(NOT_IMPLEMENTED)
    },

    // Update a swarm
    "/swarm/update" bind POST to {
        // POST /swarm/update
        // Query params: version, rotateWorkerToken, rotateManagerToken, rotateManagerUnlockKey
        // Body: SwarmSpec
        Response(NOT_IMPLEMENTED)
    },

    // Get the unlock key
    "/swarm/unlockkey" bind GET to {
        // GET /swarm/unlockkey
        Response(NOT_IMPLEMENTED)
    },

    // Unlock a locked manager
    "/swarm/unlock" bind POST to {
        // POST /swarm/unlock
        // Body: SwarmUnlockRequest
        Response(NOT_IMPLEMENTED)
    }
)
