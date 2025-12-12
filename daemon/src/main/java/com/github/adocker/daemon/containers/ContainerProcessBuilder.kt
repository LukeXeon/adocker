package com.github.adocker.daemon.containers

import com.github.adocker.daemon.app.AppContext
import com.github.adocker.daemon.registry.model.ContainerConfig
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContainerProcessBuilder @Inject constructor(
    private val appContext: AppContext,
    private val engine: PRootEngine,
    private val factory: ContainerProcess.Factory
) {
    fun startProcess(
        containerId: String,
        command: List<String>? = null,
        config: ContainerConfig = ContainerConfig(),
    ): Result<ContainerProcess> {
        val containerDir = File(appContext.containersDir, containerId)
        val rootfsDir = File(containerDir, AppContext.ROOTFS_DIR)
        if (!rootfsDir.exists()) {
            return Result.failure(IllegalStateException("Container rootfs not found"))
        }
        val process = engine.startProcess(
            config,
            rootfsDir,
            command
        )
        return process.map {
            factory.create(it)
        }
    }
}