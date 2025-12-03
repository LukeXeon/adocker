package com.github.adocker.daemon.api

import com.github.adocker.daemon.api.routes.configRoutes
import com.github.adocker.daemon.api.routes.containerRoutes
import com.github.adocker.daemon.api.routes.distributionRoutes
import com.github.adocker.daemon.api.routes.execRoutes
import com.github.adocker.daemon.api.routes.imageRoutes
import com.github.adocker.daemon.api.routes.networkRoutes
import com.github.adocker.daemon.api.routes.nodeRoutes
import com.github.adocker.daemon.api.routes.pluginRoutes
import com.github.adocker.daemon.api.routes.secretRoutes
import com.github.adocker.daemon.api.routes.serviceRoutes
import com.github.adocker.daemon.api.routes.swarmRoutes
import com.github.adocker.daemon.api.routes.systemRoutes
import com.github.adocker.daemon.api.routes.taskRoutes
import com.github.adocker.daemon.api.routes.volumeRoutes
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.then
import org.http4k.filter.ServerFilters
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
class DockerApiServer @Inject constructor() : HttpHandler {

    private val apiRoutes = routes(
        containerRoutes(),
        imageRoutes(),
        volumeRoutes(),
        networkRoutes(),
        execRoutes(),
        systemRoutes(),
        swarmRoutes(),
        nodeRoutes(),
        serviceRoutes(),
        taskRoutes(),
        secretRoutes(),
        configRoutes(),
        pluginRoutes(),
        distributionRoutes()
    )

    private val handler: HttpHandler = ServerFilters.CatchAll()
        .then(apiRoutes)

    override fun invoke(request: org.http4k.core.Request): Response {
        val response = handler(request)
        return if (response.status == NOT_FOUND) {
            Response(NOT_FOUND).body("""{"message":"page not found"}""")
        } else {
            response
        }
    }
}
