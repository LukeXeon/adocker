package com.github.andock.daemon.server.handlers

import com.github.andock.daemon.containers.ContainerManager
import com.github.andock.daemon.server.models.ExecCreateRequest
import com.github.andock.daemon.server.models.ExecCreateResponse
import com.github.andock.daemon.server.models.ExecInspectResponse
import com.github.andock.daemon.server.models.ProcessConfig
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.path
import javax.inject.Inject

class ExecHandler @Inject constructor(
    private val containerManager: ContainerManager,
    private val execSessionManager: ExecSessionManager
) {
    fun createExec(request: Request): Response = runBlocking {
        val containerId = request.path("id")
            ?: return@runBlocking Response.Companion(Status.Companion.BAD_REQUEST)
                .body("""{"message":"Container ID required"}""")

        val container = containerManager.containers.value[containerId]
            ?: return@runBlocking Response.Companion(Status.Companion.NOT_FOUND)
                .body("""{"message":"No such container: $containerId"}""")

        val body = request.bodyString()
        if (body.isEmpty()) {
            return@runBlocking Response.Companion(Status.Companion.BAD_REQUEST)
                .body("""{"message":"Config cannot be empty"}""")
        }

        val execRequest = try {
            Json.Default.decodeFromString<ExecCreateRequest>(body)
        } catch (e: Exception) {
            return@runBlocking Response.Companion(Status.Companion.BAD_REQUEST)
                .body("""{"message":"Invalid JSON: ${e.message}"}""")
        }

        val execId = execSessionManager.createSession(
            containerId = containerId,
            cmd = execRequest.cmd,
            user = execRequest.user,
            workingDir = execRequest.workingDir,
            tty = execRequest.tty
        )

        return@runBlocking Response.Companion(Status.Companion.CREATED)
            .header("Content-Type", "application/json")
            .body(Json.Default.encodeToString(ExecCreateResponse(id = execId)))
    }

    fun startExec(request: Request): Response = runBlocking {
        val execId = request.path("id")
            ?: return@runBlocking Response.Companion(Status.Companion.BAD_REQUEST)
                .body("""{"message":"Exec ID required"}""")

        val session = execSessionManager.getSession(execId)
            ?: return@runBlocking Response.Companion(Status.Companion.NOT_FOUND)
                .body("""{"message":"No such exec instance: $execId"}""")

        val container = containerManager.containers.value[session.containerId]
            ?: return@runBlocking Response.Companion(Status.Companion.NOT_FOUND)
                .body("""{"message":"No such container: ${session.containerId}"}""")

        try {
            val result = container.exec(session.cmd)

            result.fold(
                onSuccess = { process ->
                    session.process = process
                    session.running = true

                    // For now, we return NO_CONTENT for detached mode
                    // In a full implementation, you would handle attached mode differently
                    Response.Companion(Status.Companion.NO_CONTENT)
                },
                onFailure = { error ->
                    Response.Companion(Status.Companion.BAD_REQUEST)
                        .body("""{"message":"${error.message}"}""")
                }
            )
        } catch (e: Exception) {
            Response.Companion(Status.Companion.BAD_REQUEST)
                .body("""{"message":"${e.message}"}""")
        }
    }

    fun inspectExec(request: Request): Response = runBlocking {
        val execId = request.path("id")
            ?: return@runBlocking Response.Companion(Status.Companion.BAD_REQUEST)
                .body("""{"message":"Exec ID required"}""")

        val session = execSessionManager.getSession(execId)
            ?: return@runBlocking Response.Companion(Status.Companion.NOT_FOUND)
                .body("""{"message":"No such exec instance: $execId"}""")

        val inspectResponse = ExecInspectResponse(
            id = session.id,
            running = session.running,
            exitCode = session.exitCode,
            processConfig = ProcessConfig(
                tty = session.tty,
                entrypoint = session.cmd.firstOrNull() ?: "",
                arguments = session.cmd.drop(1),
                privileged = false,
                user = session.user
            ),
            openStdin = false,
            openStderr = true,
            openStdout = true,
            canRemove = !session.running,
            containerID = session.containerId,
            detachKeys = "",
            pid = 0
        )

        return@runBlocking Response.Companion(Status.Companion.OK)
            .header("Content-Type", "application/json")
            .body(Json.Default.encodeToString(inspectResponse))
    }
}