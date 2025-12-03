package com.github.adocker.daemon.api

import com.github.adocker.daemon.app.AppConfig
import com.github.adocker.daemon.http.TcpServerConfig
import com.github.adocker.daemon.http.UnixServerConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.http4k.server.Http4kServer
import org.http4k.server.asServer
import java.io.File
import javax.inject.Named
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    @Provides
    @Singleton
    @Named("unix")
    fun unixHttpServer(
        handler: DockerApiServer,
        appConfig: AppConfig
    ): Http4kServer {
        return handler.asServer(
            UnixServerConfig(
                File(
                    appConfig.tmpDir,
                    "docker.sock"
                ).absolutePath
            )
        )
    }

    @Provides
    @Singleton
    @Named("tcp")
    fun tcpHttpServer(handler: DockerApiServer): Http4kServer {
        return handler.asServer(TcpServerConfig(0))
    }
}