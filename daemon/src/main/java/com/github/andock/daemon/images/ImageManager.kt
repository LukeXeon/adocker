package com.github.andock.daemon.images

import com.github.andock.daemon.app.AppContext
import com.github.andock.daemon.client.ImageClient
import com.github.andock.daemon.client.ImageReference
import com.github.andock.daemon.client.model.ImageConfig
import com.github.andock.daemon.database.dao.ImageDao
import com.github.andock.daemon.database.model.ImageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageManager @Inject constructor(
    scope: CoroutineScope,
    private val client: ImageClient,
    private val imageDao: ImageDao,
    private val factory: Image.Factory,
) {
    private val _images = MutableStateFlow<Map<String, Image>>(emptyMap())

    val images = _images.asStateFlow()
    val sortedList = _images.map {
        it.asSequence().sortedBy { image -> image.key }.map { image -> image.value }.toList()
    }.stateIn(
        scope,
        SharingStarted.Eagerly,
        emptyList()
    )

    suspend fun pull(imageName: String) = runCatching {
        val imageRef = ImageReference.parse(imageName)
        val manifest = client.getManifest(imageRef).getOrThrow()
        var image = _images.value[manifest.config.digest]
        if (image != null) {
            return@runCatching image
        }
        val configDigest = manifest.config.digest
        val configResponse = client.getImageConfig(
            imageRef,
            configDigest
        ).getOrThrow()
        // Create image config
        val imageConfig = configResponse.config?.let { config ->
            ImageConfig(
                cmd = config.cmd,
                entrypoint = config.entrypoint,
                env = config.env,
                workingDir = config.workingDir,
                user = config.user
            )
        }
        // Save image to database
        val imageEntity = ImageEntity(
            id = manifest.config.digest,
            repository = imageRef.repository,
            tag = imageRef.tag,
            architecture = configResponse.architecture ?: AppContext.ARCHITECTURE,
            os = configResponse.os ?: AppContext.DEFAULT_OS,
            size = manifest.layers.sumOf { it.size },
            layerIds = manifest.layers.map { it.digest },
            created = System.currentTimeMillis(),
            config = imageConfig
        )
        imageDao.insertImage(imageEntity)
        image = factory.create(ImageState.Waiting(manifest.config.digest))
        _images.update {
            it + (image.id to image)
        }
        return@runCatching image
    }
}