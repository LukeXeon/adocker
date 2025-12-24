package com.github.andock.daemon.server

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.routes
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Docker Engine API server implementation.
 * Implements Docker API v1.45 compatible endpoints.
 *
 * @see <a href="https://docs.docker.com/engine/api/v1.45/">Docker Engine API v1.45</a>
 */
@Singleton
class DockerApiServer @Inject constructor(
    routes: Set<RoutingHttpHandler>
) : HttpHandler {
    private val handler = ServerFilters.CatchAll().then(
        routes(
            routes.toList()
        )
    )

    override fun invoke(request: Request): Response {
        val response = handler(request)
        return if (response.status == NOT_FOUND) {
            Response(NOT_FOUND).body("""{"message":"page not found"}""")
        } else {
            response
        }
    }
}
