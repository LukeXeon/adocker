package com.github.andock.daemon.images

import com.github.andock.daemon.app.AppContext
import com.github.andock.startup.Task


@Task("deleteUnreferencedLayers")
suspend fun deleteUnreferencedLayers(
    @Suppress("unused")
    @Task("app")
    appContext: AppContext,
    imageManager: ImageManager
) {
    imageManager.deleteUnreferencedLayers()
}