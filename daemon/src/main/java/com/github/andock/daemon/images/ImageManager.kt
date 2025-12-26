package com.github.andock.daemon.images

import com.github.andock.daemon.client.ImageReference
import com.github.andock.daemon.database.dao.ImageDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageManager @Inject constructor(
    imageDao: ImageDao,
    scope: CoroutineScope,
    private val downloaderFactory: ImageDownloader.Factory
) {
    val images = imageDao.getAllImages().stateIn(
        scope,
        SharingStarted.Lazily,
        emptyList()
    )

    fun pull(imageName: String): ImageDownloader {
        return downloaderFactory.create(ImageReference.parse(imageName))
    }
}