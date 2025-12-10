package com.github.adocker.daemon.containers

import com.github.adocker.daemon.app.AppContext
import com.github.adocker.daemon.database.dao.ContainerDao
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContainerProcessBuilder @Inject constructor(
    private val appContext: AppContext,
    private val containerDao: ContainerDao,
    private val engine: PRootEngine,
) {
    suspend fun startProcess(
        containerId: String,
        command: List<String>? = null
    ): Result<Process> {
        val containerDir = File(appContext.containersDir, containerId)
        val rootfsDir = File(containerDir, AppContext.ROOTFS_DIR)
        if (!rootfsDir.exists()) {
            return Result.failure(IllegalStateException("Container rootfs not found"))
        }
        val config = containerDao.getContainerById(containerId)?.config ?: return Result.failure(
            IllegalStateException("Container not found: $containerId")
        )
        return engine.startProcess(
            config,
            rootfsDir,
            command
        )
    }
}