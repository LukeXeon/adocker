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
 * Docker Image API routes.
 * @see <a href="https://docs.docker.com/engine/api/v1.45/#tag/Image">Docker API - Image</a>
 */
@Module
@InstallIn(SingletonComponent::class)
object ImageRoutes {
    @IntoSet
    @Provides
    fun routes(): RoutingHttpHandler = routes(
        // List images
        "/images/json" bind GET to {
            // GET /images/json
            // Query params: all, filters, shared-size, digests
            Response(NOT_IMPLEMENTED)
        },

        // Build an image
        "/build" bind POST to {
            // POST /build
            // Query params: dockerfile, t, extrahosts, remote, q, nocache, cachefrom, pull, rm, forcerm, memory, memswap, cpushares, cpusetcpus, cpuperiod, cpuquota, buildargs, shmsize, squash, labels, networkmode, platform, target, outputs
            // Headers: Content-type, X-Registry-Config
            Response(NOT_IMPLEMENTED)
        },

        // Delete builder cache
        "/build/prune" bind POST to {
            // POST /build/prune
            // Query params: keep-storage, all, filters
            Response(NOT_IMPLEMENTED)
        },

        // Create an image
        "/images/create" bind POST to {
            // POST /images/create
            // Query params: fromImage, fromSrc, repo, tag, message, changes, platform
            // Headers: X-Registry-Auth
            Response(NOT_IMPLEMENTED)
        },

        // Inspect an image
        "/images/{name}/json" bind GET to {
            // GET /images/{name}/json
            Response(NOT_IMPLEMENTED)
        },

        // Get the history of an image
        "/images/{name}/history" bind GET to {
            // GET /images/{name}/history
            Response(NOT_IMPLEMENTED)
        },

        // Push an image
        "/images/{name}/push" bind POST to {
            // POST /images/{name}/push
            // Query params: tag
            // Headers: X-Registry-Auth
            Response(NOT_IMPLEMENTED)
        },

        // Tag an image
        "/images/{name}/tag" bind POST to {
            // POST /images/{name}/tag
            // Query params: repo, tag
            Response(NOT_IMPLEMENTED)
        },

        // Remove an image
        "/images/{name}" bind DELETE to {
            // DELETE /images/{name}
            // Query params: force, noprune
            Response(NOT_IMPLEMENTED)
        },

        // Search images
        "/images/search" bind GET to {
            // GET /images/search
            // Query params: term, limit, filters
            Response(NOT_IMPLEMENTED)
        },

        // Delete unused images
        "/images/prune" bind POST to {
            // POST /images/prune
            // Query params: filters
            Response(NOT_IMPLEMENTED)
        },

        // Create a new image from a container
        "/commit" bind POST to {
            // POST /commit
            // Query params: container, repo, tag, comment, author, pause, changes
            Response(NOT_IMPLEMENTED)
        },

        // Export an image
        "/images/{name}/get" bind GET to {
            // GET /images/{name}/get
            Response(NOT_IMPLEMENTED)
        },

        // Export several images
        "/images/get" bind GET to {
            // GET /images/get
            // Query params: names
            Response(NOT_IMPLEMENTED)
        },

        // Import images
        "/images/load" bind POST to {
            // POST /images/load
            // Query params: quiet
            Response(NOT_IMPLEMENTED)
        }
    )
}
