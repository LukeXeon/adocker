package com.github.andock.daemon.containers

import com.github.andock.daemon.app.AppContext
import com.github.andock.daemon.app.AppTask
import com.github.andock.daemon.database.AppDatabase
import com.github.andock.daemon.database.dao.ContainerDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


@AppTask("clearContainers")
suspend fun clearContainers(
    @AppTask("app")
    appContext: AppContext,
    @Suppress("unused")
    @AppTask("database")
    database: AppDatabase,
    containerDao: ContainerDao
) {
    withContext(Dispatchers.IO) {
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
}