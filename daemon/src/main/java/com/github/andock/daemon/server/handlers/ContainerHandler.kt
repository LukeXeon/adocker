package com.github.andock.daemon.server.handlers

import com.github.andock.daemon.containers.ContainerManager
import com.github.andock.daemon.containers.ContainerState
import com.github.andock.daemon.images.models.ContainerConfig
import com.github.andock.daemon.server.models.ContainerConfigInfo
import com.github.andock.daemon.server.models.ContainerCreateRequest
import com.github.andock.daemon.server.models.ContainerCreateResponse
import com.github.andock.daemon.server.models.ContainerInspectResponse
import com.github.andock.daemon.server.models.ContainerStateInfo
import com.github.andock.daemon.server.models.ContainerSummary
import com.github.andock.daemon.server.models.HostConfig
import com.github.andock.daemon.server.models.NetworkSettings
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.CONFLICT
import org.http4k.core.Status.Companion.CREATED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.path
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContainerHandler @Inject constructor(
    private val containerManager: ContainerManager,
) {
    fun listContainers(request: Request): Response = runBlocking {
        val all = request.query("all")?.toBoolean() ?: false
        val containers = containerManager.containers.value.values.toList()

        val filteredContainers = if (all) containers else {
            containers.filter { it.state.value is ContainerState.Running }
        }

        val summaries = filteredContainers.mapNotNull { container ->
            val metadata = container.metadata.value ?: return@mapNotNull null
            val state = container.state.value

            val (stateStr, status, running) = when (state) {
                is ContainerState.Created -> Triple("created", "Created", false)
                is ContainerState.Running -> Triple("running", "Up", true)
                is ContainerState.Exited -> Triple("exited", "Exited", false)
                is ContainerState.Dead -> Triple("dead", "Dead", false)
                else -> Triple("unknown", "Unknown", false)
            }

            val cmd = metadata.config.cmd.joinToString(" ")

            ContainerSummary(
                id = container.id,
                names = listOf("/" + metadata.name),
                image = metadata.imageName,
                imageID = metadata.imageId,
                command = cmd,
                created = metadata.createdAt / 1000,
                state = stateStr,
                status = status
            )
        }

        return@runBlocking Response(OK)
            .header("Content-Type", "application/json")
            .body(Json.encodeToString(summaries))
    }

    fun createContainer(request: Request): Response = runBlocking {
        val name = request.query("name")
        val body = request.bodyString()

        if (body.isEmpty()) {
            return@runBlocking Response(BAD_REQUEST)
                .body("""{"message":"Config cannot be empty"}""")
        }

        val createRequest = try {
            Json.decodeFromString<ContainerCreateRequest>(body)
        } catch (e: Exception) {
            return@runBlocking Response(BAD_REQUEST)
                .body("""{"message":"Invalid JSON: ${e.message}"}""")
        }

        // Parse image name
        val imageParts = createRequest.image.split(":")
        val imageTag = if (imageParts.size > 1) imageParts[1] else "latest"
        val imageRepo = imageParts[0]

        // Find image by repository and tag
        val images = com.github.andock.daemon.images.ImageManager::class.java
            .getDeclaredField("imageDao")
            .apply { isAccessible = true }
            .get(containerManager)

        // For now, we'll use a simplified approach
        // You'll need to inject ImageDao or ImageManager to get the actual image

        // Convert request to ContainerConfig
        val config = ContainerConfig(
            cmd = createRequest.cmd ?: listOf("/bin/sh"),
            entrypoint = createRequest.entrypoint,
            env = createRequest.env?.associate {
                val parts = it.split("=", limit = 2)
                if (parts.size == 2) parts[0] to parts[1] else parts[0] to ""
            } ?: emptyMap(),
            workingDir = createRequest.workingDir ?: "/",
            user = createRequest.user ?: "root",
            hostname = createRequest.hostname ?: "localhost"
        )

        // For this implementation, we need to find the image by name
        // This is a simplified version - you should inject ImageDao properly
        try {
            val result = containerManager.createContainer(
                imageId = createRequest.image, // This should be imageId, not name
                name = name,
                config = config
            )

            result.fold(
                onSuccess = { container ->
                    Response(CREATED)
                        .header("Content-Type", "application/json")
                        .body(
                            Json.encodeToString(
                                ContainerCreateResponse(
                                    id = container.id,
                                    warnings = null
                                )
                            )
                        )
                },
                onFailure = { error ->
                    when {
                        error.message?.contains("already exists") == true ->
                            Response(CONFLICT).body("""{"message":"${error.message}"}""")

                        error.message?.contains("not found") == true ->
                            Response(NOT_FOUND).body("""{"message":"${error.message}"}""")

                        else ->
                            Response(BAD_REQUEST).body("""{"message":"${error.message}"}""")
                    }
                }
            )
        } catch (e: Exception) {
            Response(BAD_REQUEST)
                .body("""{"message":"${e.message}"}""")
        }
    }

    fun inspectContainer(request: Request): Response = runBlocking {
        val id = request.path("id") ?: return@runBlocking Response(BAD_REQUEST)
            .body("""{"message":"Container ID required"}""")

        val container = containerManager.containers.value[id]
            ?: return@runBlocking Response(NOT_FOUND)
                .body("""{"message":"No such container: $id"}""")

        val metadata = container.metadata.value
            ?: return@runBlocking Response(NOT_FOUND)
                .body("""{"message":"Container metadata not found"}""")

        val state = container.state.value

        val (stateStr, running, pid) = when (state) {
            is ContainerState.Running -> Triple("running", true, 0)
            is ContainerState.Exited -> Triple("exited", false, 0)
            is ContainerState.Created -> Triple("created", false, 0)
            is ContainerState.Dead -> Triple("dead", false, 0)
            else -> Triple("unknown", false, 0)
        }

        val inspectResponse = ContainerInspectResponse(
            id = container.id,
            created = Instant.ofEpochMilli(metadata.createdAt).toString(),
            path = metadata.config.cmd.firstOrNull() ?: "",
            args = metadata.config.cmd.drop(1),
            state = ContainerStateInfo(
                status = stateStr,
                running = running,
                paused = false,
                restarting = false,
                dead = state is ContainerState.Dead,
                pid = pid,
                exitCode = 0,
                error = if (state is ContainerState.Dead) state.throwable.message ?: "" else "",
                startedAt = Instant.ofEpochMilli(metadata.lastRunAt ?: 0).toString(),
                finishedAt = "0001-01-01T00:00:00Z"
            ),
            image = metadata.imageId,
            name = "/" + metadata.name,
            config = ContainerConfigInfo(
                hostname = metadata.config.hostname,
                user = metadata.config.user,
                env = metadata.config.env.map { "${it.key}=${it.value}" },
                cmd = metadata.config.cmd,
                image = metadata.imageName,
                workingDir = metadata.config.workingDir,
                entrypoint = metadata.config.entrypoint
            ),
            hostConfig = HostConfig(),
            networkSettings = NetworkSettings()
        )

        return@runBlocking Response(OK)
            .header("Content-Type", "application/json")
            .body(Json.encodeToString(inspectResponse))
    }

    fun startContainer(request: Request): Response = runBlocking {
        val id = request.path("id") ?: return@runBlocking Response(BAD_REQUEST)
            .body("""{"message":"Container ID required"}""")

        val container = containerManager.containers.value[id]
            ?: return@runBlocking Response(NOT_FOUND)
                .body("""{"message":"No such container: $id"}""")

        try {
            container.start()
            Response(NO_CONTENT)
        } catch (e: Exception) {
            Response(BAD_REQUEST)
                .body("""{"message":"${e.message}"}""")
        }
    }

    fun stopContainer(request: Request): Response = runBlocking {
        val id = request.path("id") ?: return@runBlocking Response(BAD_REQUEST)
            .body("""{"message":"Container ID required"}""")

        val container = containerManager.containers.value[id]
            ?: return@runBlocking Response(NOT_FOUND)
                .body("""{"message":"No such container: $id"}""")

        try {
            container.stop()
            Response(NO_CONTENT)
        } catch (e: Exception) {
            Response(BAD_REQUEST)
                .body("""{"message":"${e.message}"}""")
        }
    }

    fun removeContainer(request: Request): Response = runBlocking {
        val id = request.path("id") ?: return@runBlocking Response(BAD_REQUEST)
            .body("""{"message":"Container ID required"}""")

        val container = containerManager.containers.value[id]
            ?: return@runBlocking Response(NOT_FOUND)
                .body("""{"message":"No such container: $id"}""")

        try {
            container.remove()
            Response(NO_CONTENT)
        } catch (e: Exception) {
            Response(BAD_REQUEST)
                .body("""{"message":"${e.message}"}""")
        }
    }
}