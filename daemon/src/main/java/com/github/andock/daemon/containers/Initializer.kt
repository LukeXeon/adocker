package com.github.andock.daemon.containers

import com.github.andock.daemon.app.AppContext
import com.github.andock.daemon.app.AppTask
import com.github.andock.daemon.database.dao.ContainerDao
import com.github.andock.daemon.database.dao.LogLineDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


@AppTask("containers")
suspend fun containers(
    @AppTask("app")
    appContext: AppContext,
    containerDao: ContainerDao,
    logLineDao: LogLineDao,
) {
    withContext(Dispatchers.IO) {
        listOf(
            launch {
                logLineDao.clearAll()
            },
            launch {
                val containers = containerDao.getAllContainerIds().map {
                    File(appContext.containersDir, it)
                }.toSet()
                appContext.containersDir.listFiles {
                    !containers.contains(it)
                }.let { it ?: emptyArray() }.map { file ->
                    launch {
                        file.deleteRecursively()
                    }
                }.joinAll()
            }
        ).joinAll()
    }
}