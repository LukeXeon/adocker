package com.github.adocker.daemon.api.routes

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes

/**
 * Docker Distribution API routes.
 * @see <a href="https://docs.docker.com/engine/api/v1.45/#tag/Distribution">Docker API - Distribution</a>
 */
@Module
@InstallIn(SingletonComponent::class)
object DistributionRoutes {
    @IntoSet
    @Provides
    fun routes(): RoutingHttpHandler = routes(
        // Get image information from the registry
        "/distribution/{name}/json" bind GET to {
            // GET /distribution/{name}/json
            Response(NOT_IMPLEMENTED)
        }
    )
}
