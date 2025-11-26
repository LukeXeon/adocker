package com.github.adocker.daemon.containers

import androidx.annotation.WorkerThread
import com.github.adocker.daemon.config.AppConfig
import com.github.adocker.daemon.database.model.ContainerEntity
import com.github.adocker.daemon.engine.PRootEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.io.File
import javax.inject.Singleton

class RunningProcess @AssistedInject constructor(
    @Assisted
    entity: ContainerEntity,
    engine: PRootEngine,
    appConfig: AppConfig,
) {
    val containerId = entity.id
    val containerDir = File(appConfig.containersDir, containerId)
    val rootfsDir = File(containerDir, AppConfig.Companion.ROOTFS_DIR)

    init {
        if (!rootfsDir.exists()) {
            throw IllegalStateException("Container rootfs not found")
        }
    }

    val process = engine.startContainer(
        entity.config,
        rootfsDir
    ).getOrThrow()

    val stdin = process.outputStream.bufferedWriter()
    val stdout = process.inputStream.bufferedReader()

    @Singleton
    @AssistedFactory
    interface Factory {
        @WorkerThread
        fun create(@Assisted entity: ContainerEntity): RunningProcess
    }
}