package com.github.andock.daemon.server.routes

import com.github.andock.daemon.server.handlers.ContainerHandler
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
    fun routes(handler: ContainerHandler): RoutingHttpHandler = routes(
        // List containers
        "/containers/json" bind GET to { request ->
            handler.listContainers(request)
        },

        // Create a container
        "/containers/create" bind POST to { request ->
            handler.createContainer(request)
        },

        // Inspect a container
        "/containers/{id}/json" bind GET to { request ->
            handler.inspectContainer(request)
        },

        // List processes running inside a container
        "/containers/{id}/top" bind GET to {
            Response(NOT_IMPLEMENTED)
        },

        // Get container logs
        "/containers/{id}/logs" bind GET to {
            Response(NOT_IMPLEMENTED)
        },

        // Get changes on a container's filesystem
        "/containers/{id}/changes" bind GET to {
            Response(NOT_IMPLEMENTED)
        },

        // Export a container
        "/containers/{id}/export" bind GET to {
            Response(NOT_IMPLEMENTED)
        },

        // Get container stats based on resource usage
        "/containers/{id}/stats" bind GET to {
            Response(NOT_IMPLEMENTED)
        },

        // Resize a container TTY
        "/containers/{id}/resize" bind POST to {
            Response(NOT_IMPLEMENTED)
        },

        // Start a container
        "/containers/{id}/start" bind POST to { request ->
            handler.startContainer(request)
        },

        // Stop a container
        "/containers/{id}/stop" bind POST to { request ->
            handler.stopContainer(request)
        },

        // Restart a container
        "/containers/{id}/restart" bind POST to {
            Response(NOT_IMPLEMENTED)
        },

        // Kill a container
        "/containers/{id}/kill" bind POST to {
            Response(NOT_IMPLEMENTED)
        },

        // Update a container
        "/containers/{id}/update" bind POST to {
            Response(NOT_IMPLEMENTED)
        },

        // Rename a container
        "/containers/{id}/rename" bind POST to {
            Response(NOT_IMPLEMENTED)
        },

        // Pause a container
        "/containers/{id}/pause" bind POST to {
            Response(NOT_IMPLEMENTED)
        },

        // Unpause a container
        "/containers/{id}/unpause" bind POST to {
            Response(NOT_IMPLEMENTED)
        },

        // Attach to a container
        "/containers/{id}/attach" bind POST to {
            Response(NOT_IMPLEMENTED)
        },

        // Attach to a container via websocket
        "/containers/{id}/attach/ws" bind GET to {
            Response(NOT_IMPLEMENTED)
        },

        // Wait for a container
        "/containers/{id}/wait" bind POST to {
            Response(NOT_IMPLEMENTED)
        },

        // Remove a container
        "/containers/{id}" bind DELETE to { request ->
            handler.removeContainer(request)
        },

        // Get information about files in a container
        "/containers/{id}/archive" bind HEAD to {
            Response(NOT_IMPLEMENTED)
        },

        // Get an archive of a filesystem resource in a container
        "/containers/{id}/archive" bind GET to {
            Response(NOT_IMPLEMENTED)
        },

        // Extract an archive of files or folders to a directory in a container
        "/containers/{id}/archive" bind org.http4k.core.Method.PUT to {
            Response(NOT_IMPLEMENTED)
        },

        // Delete stopped containers
        "/containers/prune" bind POST to {
            Response(NOT_IMPLEMENTED)
        }
    )
}
