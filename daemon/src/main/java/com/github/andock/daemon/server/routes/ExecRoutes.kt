package com.github.andock.daemon.server.routes

import com.github.andock.daemon.containers.ContainerManager
import com.github.andock.daemon.server.model.ExecCreateRequest
import com.github.andock.daemon.server.model.ExecCreateResponse
import com.github.andock.daemon.server.model.ExecInspectResponse
import com.github.andock.daemon.server.model.ProcessConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.CREATED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Docker Exec API routes.
 * @see <a href="https://docs.docker.com/engine/api/v1.45/#tag/Exec">Docker API - Exec</a>
 */
@Module
@InstallIn(SingletonComponent::class)
object ExecRoutes {

    @Singleton
    class ExecSessionManager @Inject constructor() {
        private val sessions = ConcurrentHashMap<String, ExecSession>()

        data class ExecSession(
            val id: String,
            val containerId: String,
            val cmd: List<String>,
            val user: String?,
            val workingDir: String?,
            val tty: Boolean,
            var process: Process? = null,
            var running: Boolean = false,
            var exitCode: Int = 0
        )

        fun createSession(
            containerId: String,
            cmd: List<String>,
            user: String?,
            workingDir: String?,
            tty: Boolean
        ): String {
            val id = UUID.randomUUID().toString()
            val session = ExecSession(
                id = id,
                containerId = containerId,
                cmd = cmd,
                user = user,
                workingDir = workingDir,
                tty = tty
            )
            sessions[id] = session
            return id
        }

        fun getSession(id: String): ExecSession? = sessions[id]

        fun removeSession(id: String) {
            sessions.remove(id)
        }
    }

    class Handler @Inject constructor(
        private val containerManager: ContainerManager,
        private val execSessionManager: ExecSessionManager
    ) {
        fun createExec(request: Request): Response = runBlocking {
            val containerId = request.path("id") ?: return@runBlocking Response(BAD_REQUEST)
                .body("""{"message":"Container ID required"}""")

            val container = containerManager.containers.value[containerId]
                ?: return@runBlocking Response(NOT_FOUND)
                    .body("""{"message":"No such container: $containerId"}""")

            val body = request.bodyString()
            if (body.isEmpty()) {
                return@runBlocking Response(BAD_REQUEST)
                    .body("""{"message":"Config cannot be empty"}""")
            }

            val execRequest = try {
                Json.decodeFromString<ExecCreateRequest>(body)
            } catch (e: Exception) {
                return@runBlocking Response(BAD_REQUEST)
                    .body("""{"message":"Invalid JSON: ${e.message}"}""")
            }

            val execId = execSessionManager.createSession(
                containerId = containerId,
                cmd = execRequest.cmd,
                user = execRequest.user,
                workingDir = execRequest.workingDir,
                tty = execRequest.tty
            )

            return@runBlocking Response(CREATED)
                .header("Content-Type", "application/json")
                .body(Json.encodeToString(ExecCreateResponse(id = execId)))
        }

        fun startExec(request: Request): Response = runBlocking {
            val execId = request.path("id") ?: return@runBlocking Response(BAD_REQUEST)
                .body("""{"message":"Exec ID required"}""")

            val session = execSessionManager.getSession(execId)
                ?: return@runBlocking Response(NOT_FOUND)
                    .body("""{"message":"No such exec instance: $execId"}""")

            val container = containerManager.containers.value[session.containerId]
                ?: return@runBlocking Response(NOT_FOUND)
                    .body("""{"message":"No such container: ${session.containerId}"}""")

            try {
                val result = container.exec(session.cmd)

                result.fold(
                    onSuccess = { process ->
                        session.process = process
                        session.running = true

                        // For now, we return NO_CONTENT for detached mode
                        // In a full implementation, you would handle attached mode differently
                        Response(NO_CONTENT)
                    },
                    onFailure = { error ->
                        Response(BAD_REQUEST)
                            .body("""{"message":"${error.message}"}""")
                    }
                )
            } catch (e: Exception) {
                Response(BAD_REQUEST)
                    .body("""{"message":"${e.message}"}""")
            }
        }

        fun inspectExec(request: Request): Response = runBlocking {
            val execId = request.path("id") ?: return@runBlocking Response(BAD_REQUEST)
                .body("""{"message":"Exec ID required"}""")

            val session = execSessionManager.getSession(execId)
                ?: return@runBlocking Response(NOT_FOUND)
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

            return@runBlocking Response(OK)
                .header("Content-Type", "application/json")
                .body(Json.encodeToString(inspectResponse))
        }
    }

    @IntoSet
    @Provides
    fun routes(handler: Handler): RoutingHttpHandler = routes(
        // Create an exec instance
        "/containers/{id}/exec" bind POST to { request ->
            handler.createExec(request)
        },

        // Start an exec instance
        "/exec/{id}/start" bind POST to { request ->
            handler.startExec(request)
        },

        // Resize an exec instance
        "/exec/{id}/resize" bind POST to {
            Response(NOT_IMPLEMENTED)
        },

        // Inspect an exec instance
        "/exec/{id}/json" bind GET to { request ->
            handler.inspectExec(request)
        }
    )
}
