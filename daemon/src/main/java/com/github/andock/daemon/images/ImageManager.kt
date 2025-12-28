package com.github.andock.daemon.images

import androidx.room.withTransaction
import com.github.andock.daemon.app.AppContext
import com.github.andock.daemon.client.ImageReference
import com.github.andock.daemon.database.AppDatabase
import com.github.andock.daemon.database.dao.ImageDao
import com.github.andock.daemon.database.dao.LayerDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageManager @Inject constructor(
    private val imageDao: ImageDao,
    private val layerDao: LayerDao,
    private val database: AppDatabase,
    private val appContext: AppContext,
    private val downloaderFactory: ImageDownloader.Factory,
    scope: CoroutineScope,
) {
    val images = imageDao.getAllImages().stateIn(
        scope,
        SharingStarted.Eagerly,
        emptyList()
    )

    fun getImageById(id: String) = imageDao.getImageFlowById(id)

    fun pullImage(imageName: String): ImageDownloader {
        return downloaderFactory.create(ImageReference.parse(imageName))
    }

    suspend fun deleteImage(id: String) {
        database.withTransaction {
            imageDao.deleteImageById(id)
            val layers = layerDao.deleteUnreferencedLayers()
            withContext(Dispatchers.IO) {
                layers.asSequence().map {
                    File(appContext.layersDir, "${it}.tar.gz")
                }.forEach {
                    it.delete()
                }
            }
        }
    }
}