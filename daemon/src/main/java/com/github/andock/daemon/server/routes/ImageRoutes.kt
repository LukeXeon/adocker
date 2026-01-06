package com.github.andock.daemon.server.routes

import com.github.andock.daemon.server.handlers.ImageHandler
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
    fun routes(handler: ImageHandler): RoutingHttpHandler = routes(
        // List images
        "/images/json" bind GET to { request ->
            handler.listImages(request)
        },

        // Build an image
        "/build" bind POST to {
            Response(NOT_IMPLEMENTED)
        },

        // Delete builder cache
        "/build/prune" bind POST to {
            Response(NOT_IMPLEMENTED)
        },

        // Create an image (pull)
        "/images/create" bind POST to {
            Response(NOT_IMPLEMENTED)
        },

        // Inspect an image
        "/images/{name}/json" bind GET to { request ->
            handler.inspectImage(request)
        },

        // Get the history of an image
        "/images/{name}/history" bind GET to {
            Response(NOT_IMPLEMENTED)
        },

        // Push an image
        "/images/{name}/push" bind POST to {
            Response(NOT_IMPLEMENTED)
        },

        // Tag an image
        "/images/{name}/tag" bind POST to {
            Response(NOT_IMPLEMENTED)
        },

        // Remove an image
        "/images/{name}" bind DELETE to { request ->
            handler.deleteImage(request)
        },

        // Search images
        "/images/search" bind GET to {
            Response(NOT_IMPLEMENTED)
        },

        // Delete unused images
        "/images/prune" bind POST to {
            Response(NOT_IMPLEMENTED)
        },

        // Create a new image from a container
        "/commit" bind POST to {
            Response(NOT_IMPLEMENTED)
        },

        // Export an image
        "/images/{name}/get" bind GET to {
            Response(NOT_IMPLEMENTED)
        },

        // Export several images
        "/images/get" bind GET to {
            Response(NOT_IMPLEMENTED)
        },

        // Import images
        "/images/load" bind POST to {
            Response(NOT_IMPLEMENTED)
        }
    )
}
