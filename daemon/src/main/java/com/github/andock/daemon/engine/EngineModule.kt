package com.github.andock.daemon.engine

import android.app.Application
import com.github.andock.daemon.app.socketFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object EngineModule {
    @Provides
    @Named("redirect")
    fun redirect(appContext: Application): Map<String, String> {
        return mapOf(
            DOCKER_SOCK_PATH to appContext.socketFile.absolutePath
        )
    }

    /** Standard Docker socket path on Linux */
    private const val DOCKER_SOCK_PATH = "/var/run/docker.sock"
}