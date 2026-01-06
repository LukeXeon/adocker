package com.github.andock.daemon.server.handlers

import com.github.andock.daemon.images.ImageManager
import com.github.andock.daemon.server.models.ImageConfigData
import com.github.andock.daemon.server.models.ImageDeleteResponse
import com.github.andock.daemon.server.models.ImageInspectResponse
import com.github.andock.daemon.server.models.ImageSummary
import com.github.andock.daemon.server.models.RootFS
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.path
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageHandler @Inject constructor(
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

        return@runBlocking Response.Companion(Status.Companion.OK)
            .header("Content-Type", "application/json")
            .body(Json.Default.encodeToString(summaries))
    }

    fun inspectImage(request: Request): Response = runBlocking {
        val name = request.path("name")
            ?: return@runBlocking Response.Companion(Status.Companion.BAD_REQUEST)
                .body("""{"message":"Image name required"}""")

        // Try to find image by ID or by repository:tag
        val images = imageManager.images.first()
        val image = images.find { it.id == name }
            ?: images.find { "${it.repository}:${it.tag}" == name }
            ?: return@runBlocking Response.Companion(Status.Companion.NOT_FOUND)
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

        return@runBlocking Response.Companion(Status.Companion.OK)
            .header("Content-Type", "application/json")
            .body(Json.Default.encodeToString(inspectResponse))
    }

    fun deleteImage(request: Request): Response = runBlocking {
        val name = request.path("name")
            ?: return@runBlocking Response.Companion(Status.Companion.BAD_REQUEST)
                .body("""{"message":"Image name required"}""")

        // Try to find image by ID or by repository:tag
        val images = imageManager.images.first()
        val image = images.find { it.id == name }
            ?: images.find { "${it.repository}:${it.tag}" == name }
            ?: return@runBlocking Response.Companion(Status.Companion.NOT_FOUND)
                .body("""{"message":"No such image: $name"}""")

        try {
            imageManager.deleteImage(image.id)

            val responses = listOf(
                ImageDeleteResponse(untagged = "${image.repository}:${image.tag}"),
                ImageDeleteResponse(deleted = image.id)
            )

            return@runBlocking Response.Companion(Status.Companion.OK)
                .header("Content-Type", "application/json")
                .body(Json.Default.encodeToString(responses))
        } catch (e: Exception) {
            Response.Companion(Status.Companion.BAD_REQUEST)
                .body("""{"message":"${e.message}"}""")
        }
    }
}