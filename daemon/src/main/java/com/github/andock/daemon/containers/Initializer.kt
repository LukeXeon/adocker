package com.github.andock.daemon.containers

import com.github.andock.daemon.app.AppContext
import com.github.andock.daemon.database.dao.ContainerDao
import com.github.andock.startup.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


@Task("clearContainers")
suspend fun clearContainers(
    @Task("app")
    appContext: AppContext,
    containerDao: ContainerDao
) {
    withContext(Dispatchers.IO) {
        val containers = containerDao.getAllIds().map {
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
}