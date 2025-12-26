package com.github.andock.daemon.containers

import com.github.andock.daemon.app.AppContext
import com.github.andock.daemon.app.AppInitializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
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

    @Provides
    @IntoSet
    fun initializer(initializer: PRootInitializer): AppInitializer.Task<*> = initializer

    /** Standard Docker socket path on Linux */
    private const val DOCKER_SOCK_PATH = "/var/run/docker.sock"
}