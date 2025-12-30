package com.github.andock.daemon.server

import android.net.LocalSocketAddress.Namespace
import com.github.andock.daemon.app.AppContext
import com.github.andock.daemon.http.TcpServerConfig
import com.github.andock.daemon.http.UnixServerConfig
import com.github.andock.daemon.utils.SuspendLazy
import com.github.andock.daemon.utils.suspendLazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.IntoSet
import dagger.multibindings.StringKey
import org.http4k.server.Http4kServer
import org.http4k.server.asServer
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    @Provides
    @Singleton
    @IntoSet
    fun unixHttpServer(
        handler: DockerApiServer,
        appContext: AppContext
    ): Http4kServer {
        return handler.asServer(
            UnixServerConfig(
                appContext.socketFile.absolutePath,
                Namespace.FILESYSTEM
            )
        )
    }

    @Provides
    @Singleton
    @IntoSet
    fun tcpHttpServer(handler: DockerApiServer): Http4kServer {
        return handler.asServer(TcpServerConfig(0))
    }

    @Provides
    @Singleton
    @IntoMap
    @StringKey("server")
    fun initializer(
        servers: Set<@JvmSuppressWildcards Http4kServer>
    ): SuspendLazy<*> = suspendLazy {
        servers.forEach {
            it.start()
        }
    }
}