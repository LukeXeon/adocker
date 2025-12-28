package com.github.andock.daemon.engine

import com.github.andock.daemon.app.AppContext
import com.github.andock.daemon.utils.SuspendLazy
import com.github.andock.daemon.utils.suspendLazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EngineModule {

    @Provides
    @Named("redirect")
    fun redirect(appContext: AppContext): Map<String, String> {
        return mapOf(
            DOCKER_SOCK_PATH to appContext.socketFile.absolutePath
        )
    }

    /** Standard Docker socket path on Linux */
    private const val DOCKER_SOCK_PATH = "/var/run/docker.sock"

    @Provides
    @Singleton
    @IntoSet
    fun initializer(version: PRootVersion): SuspendLazy<*> = suspendLazy {
        withTimeoutOrNull(1000) {
            Timber.i(version.value.filterNotNull().first())
        }
    }
}