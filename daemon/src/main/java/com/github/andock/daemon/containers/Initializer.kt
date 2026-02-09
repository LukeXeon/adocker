package com.github.andock.daemon.containers

import android.app.Application
import com.github.andock.daemon.app.containersDir
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
    appContext: Application,
    containerDao: ContainerDao
) {
    withContext(Dispatchers.IO) {
        val containersDir = appContext.containersDir
        val containers = containerDao.getAllIds().map {
            File(containersDir, it)
        }.toSet()
        containersDir.listFiles {
            !containers.contains(it)
        }.let { it ?: emptyArray() }.map { file ->
            launch {
                file.deleteRecursively()
            }
        }.joinAll()
    }
}