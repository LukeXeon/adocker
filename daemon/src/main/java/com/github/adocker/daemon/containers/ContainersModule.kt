package com.github.adocker.daemon.containers

import com.github.adocker.daemon.app.AppContext
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object ContainersModule {
    @Provides
    @Named("redirect")
    fun redirect(appContext: AppContext): Map<String, String> {
        return mapOf(
            DOCKER_SOCK_PATH to appContext.socketFile.absolutePath
        )
    }

    /** Standard Docker socket path on Linux */
    private const val DOCKER_SOCK_PATH = "/var/run/docker.sock"
}