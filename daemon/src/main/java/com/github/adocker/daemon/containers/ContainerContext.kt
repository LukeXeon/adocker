package com.github.adocker.daemon.containers

import com.github.adocker.daemon.app.AppConfig
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.io.File
import javax.inject.Singleton

class ContainerContext @AssistedInject constructor(
    @Assisted
    val containerId: String,
    appConfig: AppConfig,
    private val containerRepository: ContainerRepository,
    private val engine: PRootEngine,
) {
    val containerDir = File(appConfig.containersDir, containerId)
    val rootfsDir = File(containerDir, AppConfig.ROOTFS_DIR)

    suspend fun startProcess(command: List<String>? = null): Result<Process> {
        if (!rootfsDir.exists()) {
            return Result.failure(IllegalStateException("Container rootfs not found"))
        }
        val config = containerRepository.getContainerById(containerId)?.config
        if (config == null) {
            return Result.failure(IllegalStateException("Container not found: $containerId"))
        }
        return engine.startProcess(
            config,
            rootfsDir,
            command
        )
    }

    @Singleton
    @AssistedFactory
    interface Factory {
        fun create(@Assisted containerId: String): ContainerContext
    }
}