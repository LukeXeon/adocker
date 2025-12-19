package com.github.adocker.daemon.server.routes

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.HEAD
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes

/**
 * Docker Container API routes.
 * @see <a href="https://docs.docker.com/engine/api/v1.45/#tag/Container">Docker API - Container</a>
 */
@Module
@InstallIn(SingletonComponent::class)
object ContainerRoutes {
    @IntoSet
    @Provides
    fun routes(): RoutingHttpHandler = routes(
        // List containers
        "/containers/json" bind GET to {
            // GET /containers/json
            // Query params: all, limit, size, filters
            Response(NOT_IMPLEMENTED)
        },

        // Create a container
        "/containers/create" bind POST to {
            // POST /containers/create
            // Query params: name, platform
            // Body: Container config
            Response(NOT_IMPLEMENTED)
        },

        // Inspect a container
        "/containers/{id}/json" bind GET to {
            // GET /containers/{id}/json
            // Query params: size
            Response(NOT_IMPLEMENTED)
        },

        // List processes running inside a container
        "/containers/{id}/top" bind GET to {
            // GET /containers/{id}/top
            // Query params: ps_args
            Response(NOT_IMPLEMENTED)
        },

        // Get container logs
        "/containers/{id}/logs" bind GET to {
            // GET /containers/{id}/logs
            // Query params: follow, stdout, stderr, since, until, timestamps, tail
            Response(NOT_IMPLEMENTED)
        },

        // Get changes on a container's filesystem
        "/containers/{id}/changes" bind GET to {
            // GET /containers/{id}/changes
            Response(NOT_IMPLEMENTED)
        },

        // Export a container
        "/containers/{id}/export" bind GET to {
            // GET /containers/{id}/export
            Response(NOT_IMPLEMENTED)
        },

        // Get container stats based on resource usage
        "/containers/{id}/stats" bind GET to {
            // GET /containers/{id}/stats
            // Query params: stream, one-shot
            Response(NOT_IMPLEMENTED)
        },

        // Resize a container TTY
        "/containers/{id}/resize" bind POST to {
            // POST /containers/{id}/resize
            // Query params: h, w
            Response(NOT_IMPLEMENTED)
        },

        // Start a container
        "/containers/{id}/start" bind POST to {
            // POST /containers/{id}/start
            // Query params: detachKeys
            Response(NOT_IMPLEMENTED)
        },

        // Stop a container
        "/containers/{id}/stop" bind POST to {
            // POST /containers/{id}/stop
            // Query params: signal, t
            Response(NOT_IMPLEMENTED)
        },

        // Restart a container
        "/containers/{id}/restart" bind POST to {
            // POST /containers/{id}/restart
            // Query params: signal, t
            Response(NOT_IMPLEMENTED)
        },

        // Kill a container
        "/containers/{id}/kill" bind POST to {
            // POST /containers/{id}/kill
            // Query params: signal
            Response(NOT_IMPLEMENTED)
        },

        // Update a container
        "/containers/{id}/update" bind POST to {
            // POST /containers/{id}/update
            // Body: Update config (resources)
            Response(NOT_IMPLEMENTED)
        },

        // Rename a container
        "/containers/{id}/rename" bind POST to {
            // POST /containers/{id}/rename
            // Query params: name
            Response(NOT_IMPLEMENTED)
        },

        // Pause a container
        "/containers/{id}/pause" bind POST to {
            // POST /containers/{id}/pause
            Response(NOT_IMPLEMENTED)
        },

        // Unpause a container
        "/containers/{id}/unpause" bind POST to {
            // POST /containers/{id}/unpause
            Response(NOT_IMPLEMENTED)
        },

        // Attach to a container
        "/containers/{id}/attach" bind POST to {
            // POST /containers/{id}/attach
            // Query params: detachKeys, logs, stream, stdin, stdout, stderr
            Response(NOT_IMPLEMENTED)
        },

        // Attach to a container via websocket
        "/containers/{id}/attach/ws" bind GET to {
            // GET /containers/{id}/attach/ws
            // Query params: detachKeys, logs, stream, stdin, stdout, stderr
            Response(NOT_IMPLEMENTED)
        },

        // Wait for a container
        "/containers/{id}/wait" bind POST to {
            // POST /containers/{id}/wait
            // Query params: condition
            Response(NOT_IMPLEMENTED)
        },

        // Remove a container
        "/containers/{id}" bind DELETE to {
            // DELETE /containers/{id}
            // Query params: v, force, link
            Response(NOT_IMPLEMENTED)
        },

        // Get information about files in a container
        "/containers/{id}/archive" bind HEAD to {
            // HEAD /containers/{id}/archive
            // Query params: path
            Response(NOT_IMPLEMENTED)
        },

        // Get an archive of a filesystem resource in a container
        "/containers/{id}/archive" bind GET to {
            // GET /containers/{id}/archive
            // Query params: path
            Response(NOT_IMPLEMENTED)
        },

        // Extract an archive of files or folders to a directory in a container
        "/containers/{id}/archive" bind org.http4k.core.Method.PUT to {
            // PUT /containers/{id}/archive
            // Query params: path, noOverwriteDirNonDir, copyUIDGID
            Response(NOT_IMPLEMENTED)
        },

        // Delete stopped containers
        "/containers/prune" bind POST to {
            // POST /containers/prune
            // Query params: filters
            Response(NOT_IMPLEMENTED)
        }
    )
}
