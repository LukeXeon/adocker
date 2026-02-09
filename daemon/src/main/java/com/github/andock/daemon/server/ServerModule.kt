package com.github.andock.daemon.server

import android.app.Application
import android.net.LocalSocketAddress.Namespace
import com.github.andock.daemon.app.socketFile
import com.github.andock.daemon.http.TcpServerConfig
import com.github.andock.daemon.http.UnixServerConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import org.http4k.server.Http4kServer
import org.http4k.server.asServer
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServerModule {
    @Provides
    @Singleton
    @IntoSet
    fun unixHttpServer(
        handler: DockerApiServer,
        appContext: Application,
        factory: UnixServerConfig.Factory
    ): Http4kServer {
        return handler.asServer(
            factory.create(
                appContext.socketFile.absolutePath,
                Namespace.FILESYSTEM
            )
        )
    }

    @Provides
    @Singleton
    @IntoSet
    fun tcpHttpServer(
        handler: DockerApiServer,
        factory: TcpServerConfig.Factory
    ): Http4kServer {
        return handler.asServer(factory.create(0))
    }
}