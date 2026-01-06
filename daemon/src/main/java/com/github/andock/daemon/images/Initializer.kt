package com.github.andock.daemon.images

import com.github.andock.daemon.app.AppContext
import com.github.andock.daemon.app.AppTask


@AppTask("deleteUnreferencedLayers")
suspend fun deleteUnreferencedLayers(
    @Suppress("unused")
    @AppTask("app")
    appContext: AppContext,
    imageManager: ImageManager
) {
    imageManager.deleteUnreferencedLayers()
}