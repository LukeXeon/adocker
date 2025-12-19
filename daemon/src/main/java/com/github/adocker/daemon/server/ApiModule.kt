package com.github.adocker.daemon.server

import android.net.LocalSocketAddress.Namespace
import com.github.adocker.daemon.app.AppContext
import com.github.adocker.daemon.http.TcpServerConfig
import com.github.adocker.daemon.http.UnixServerConfig
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
}