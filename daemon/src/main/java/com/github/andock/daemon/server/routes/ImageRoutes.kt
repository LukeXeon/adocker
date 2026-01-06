package com.github.andock.daemon.server.routes

import com.github.andock.daemon.images.ImageManager
import com.github.andock.daemon.server.model.ImageConfigData
import com.github.andock.daemon.server.model.ImageDeleteResponse
import com.github.andock.daemon.server.model.ImageInspectResponse
import com.github.andock.daemon.server.model.ImageSummary
import com.github.andock.daemon.server.model.RootFS
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import java.time.Instant
import javax.inject.Inject

/**
 * Docker Image API routes.
 * @see <a href="https://docs.docker.com/engine/api/v1.45/#tag/Image">Docker API - Image</a>
 */
@Module
@InstallIn(SingletonComponent::class)
object ImageRoutes {

    class Handler @Inject constructor(
        private val imageManager: ImageManager
    ) {
        fun listImages(request: Request): Response = runBlocking {
            val images = imageManager.images.first()

            val summaries = images.map { image ->
                ImageSummary(
                    id = image.id,
                    parentId = "",
                    repoTags = listOf("${image.repository}:${image.tag}"),
                    repoDigests = listOf(image.id),
                    created = image.created / 1000,
                    size = image.size,
                    virtualSize = image.size,
                    sharedSize = 0,
                    labels = null,
                    containers = 0
                )
            }

            return@runBlocking Response(OK)
                .header("Content-Type", "application/json")
                .body(Json.encodeToString(summaries))
        }

        fun inspectImage(request: Request): Response = runBlocking {
            val name = request.path("name") ?: return@runBlocking Response(BAD_REQUEST)
                .body("""{"message":"Image name required"}""")

            // Try to find image by ID or by repository:tag
            val images = imageManager.images.first()
            val image = images.find { it.id == name }
                ?: images.find { "${it.repository}:${it.tag}" == name }
                ?: return@runBlocking Response(NOT_FOUND)
                    .body("""{"message":"No such image: $name"}""")

            val config = image.config
            val imageConfig = if (config != null) {
                ImageConfigData(
                    hostname = "",
                    domainname = "",
                    user = config.user ?: "",
                    env = config.env ?: emptyList(),
                    cmd = config.cmd ?: emptyList(),
                    workingDir = config.workingDir ?: "",
                    entrypoint = config.entrypoint,
                    labels = null
                )
            } else null

            val inspectResponse = ImageInspectResponse(
                id = image.id,
                repoTags = listOf("${image.repository}:${image.tag}"),
                repoDigests = listOf(image.id),
                parent = "",
                comment = "",
                created = Instant.ofEpochMilli(image.created).toString(),
                container = "",
                dockerVersion = "",
                author = "",
                config = imageConfig,
                architecture = image.architecture,
                os = image.os,
                size = image.size,
                virtualSize = image.size,
                rootFS = RootFS(
                    type = "layers",
                    layers = image.layerIds
                )
            )

            return@runBlocking Response(OK)
                .header("Content-Type", "application/json")
                .body(Json.encodeToString(inspectResponse))
        }

        fun deleteImage(request: Request): Response = runBlocking {
            val name = request.path("name") ?: return@runBlocking Response(BAD_REQUEST)
                .body("""{"message":"Image name required"}""")

            // Try to find image by ID or by repository:tag
            val images = imageManager.images.first()
            val image = images.find { it.id == name }
                ?: images.find { "${it.repository}:${it.tag}" == name }
                ?: return@runBlocking Response(NOT_FOUND)
                    .body("""{"message":"No such image: $name"}""")

            try {
                imageManager.deleteImage(image.id)

                val responses = listOf(
                    ImageDeleteResponse(untagged = "${image.repository}:${image.tag}"),
                    ImageDeleteResponse(deleted = image.id)
                )

                return@runBlocking Response(OK)
                    .header("Content-Type", "application/json")
                    .body(Json.encodeToString(responses))
            } catch (e: Exception) {
                Response(BAD_REQUEST)
                    .body("""{"message":"${e.message}"}""")
            }
        }
    }

    @IntoSet
    @Provides
    fun routes(handler: Handler): RoutingHttpHandler = routes(
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
