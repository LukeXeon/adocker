package com.github.andock.daemon.images

import android.app.Application
import com.github.andock.startup.Task


@Task("deleteUnreferencedLayers")
suspend fun deleteUnreferencedLayers(
    @Suppress("unused")
    @Task("app")
    appContext: Application,
    imageManager: ImageManager
) {
    imageManager.deleteUnreferencedLayers()
}