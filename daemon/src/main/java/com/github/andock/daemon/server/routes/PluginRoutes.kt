package com.github.andock.daemon.server.routes

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
 * Docker Plugin API routes.
 * @see <a href="https://docs.docker.com/engine/api/v1.45/#tag/Plugin">Docker API - Plugin</a>
 */
@Module
@InstallIn(SingletonComponent::class)
object PluginRoutes {
    @IntoSet
    @Provides
    fun routes(): RoutingHttpHandler = routes(
        // List plugins
        "/plugins" bind GET to {
            // GET /plugins
            // Query params: filters
            Response(NOT_IMPLEMENTED)
        },

        // Get plugin privileges
        "/plugins/privileges" bind GET to {
            // GET /plugins/privileges
            // Query params: remote
            Response(NOT_IMPLEMENTED)
        },

        // Install a plugin
        "/plugins/pull" bind POST to {
            // POST /plugins/pull
            // Query params: remote, name
            // Headers: X-Registry-Auth
            // Body: Plugin privileges
            Response(NOT_IMPLEMENTED)
        },

        // Inspect a plugin
        "/plugins/{name}/json" bind GET to {
            // GET /plugins/{name}/json
            Response(NOT_IMPLEMENTED)
        },

        // Remove a plugin
        "/plugins/{name}" bind DELETE to {
            // DELETE /plugins/{name}
            // Query params: force
            Response(NOT_IMPLEMENTED)
        },

        // Enable a plugin
        "/plugins/{name}/enable" bind POST to {
            // POST /plugins/{name}/enable
            // Query params: timeout
            Response(NOT_IMPLEMENTED)
        },

        // Disable a plugin
        "/plugins/{name}/disable" bind POST to {
            // POST /plugins/{name}/disable
            // Query params: force
            Response(NOT_IMPLEMENTED)
        },

        // Upgrade a plugin
        "/plugins/{name}/upgrade" bind POST to {
            // POST /plugins/{name}/upgrade
            // Query params: remote
            // Headers: X-Registry-Auth
            // Body: Plugin privileges
            Response(NOT_IMPLEMENTED)
        },

        // Create a plugin
        "/plugins/create" bind POST to {
            // POST /plugins/create
            // Query params: name
            // Body: Plugin tar archive
            Response(NOT_IMPLEMENTED)
        },

        // Push a plugin
        "/plugins/{name}/push" bind POST to {
            // POST /plugins/{name}/push
            Response(NOT_IMPLEMENTED)
        },

        // Configure a plugin
        "/plugins/{name}/set" bind POST to {
            // POST /plugins/{name}/set
            // Body: Plugin config
            Response(NOT_IMPLEMENTED)
        }
    )
}
