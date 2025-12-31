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
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServerModule {
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
    @Named("server")
    fun initializer(
        servers: Set<@JvmSuppressWildcards Http4kServer>,
        @Named("logging")
        logging: SuspendLazy<Unit>,
        @Named("reporter")
        reporter: SuspendLazy<Unit>
    ) = suspendLazy {
        logging.getValue()
        reporter.getValue()
        servers.forEach {
            it.start()
        }
    }

    @Provides
    @IntoMap
    @StringKey("server")
    fun initializerToMap(
        @Named("server") task: SuspendLazy<Unit>
    ): SuspendLazy<*> = task
}