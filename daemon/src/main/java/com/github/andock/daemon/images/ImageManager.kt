package com.github.andock.daemon.images

import com.github.andock.daemon.client.ImageReference
import com.github.andock.daemon.database.dao.ImageDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageManager @Inject constructor(
    private val imageDao: ImageDao,
    private val downloaderFactory: ImageDownloader.Factory
) {
    val images = imageDao.getAllImages()

    fun getImageById(id: String) = imageDao.getImageFlowById(id)

    fun pull(imageName: String): ImageDownloader {
        return downloaderFactory.create(ImageReference.parse(imageName))
    }
}