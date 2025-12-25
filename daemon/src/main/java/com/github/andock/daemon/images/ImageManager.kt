package com.github.andock.daemon.images

import com.github.andock.daemon.app.AppContext
import com.github.andock.daemon.client.RegistryClient
import com.github.andock.daemon.client.model.ImageConfig
import com.github.andock.daemon.client.model.ImageReference
import com.github.andock.daemon.database.dao.ImageDao
import com.github.andock.daemon.database.model.ImageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageManager @Inject constructor(
    scope: CoroutineScope,
    private val client: RegistryClient,
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

    suspend fun pull(imageName: String): Result<Image> {
        val imageRef = ImageReference.parse(imageName)
        client.getManifest(imageRef).fold(
            { manifest ->
                client.getImageConfig(
                    imageRef,
                    manifest.config.digest
                ).fold(
                    { configResponse ->
                        // Create image config
                        val imageConfig = ImageConfig(
                            cmd = configResponse.config?.cmd,
                            entrypoint = configResponse.config?.entrypoint,
                            env = configResponse.config?.env,
                            workingDir = configResponse.config?.workingDir,
                            user = configResponse.config?.user
                        )
                        // Save image to database
                        val imageEntity = ImageEntity(
                            repository = imageRef.repository,
                            tag = imageRef.tag,
                            digest = manifest.config.digest,
                            architecture = configResponse.architecture ?: AppContext.ARCHITECTURE,
                            os = configResponse.os ?: AppContext.DEFAULT_OS,
                            size = manifest.layers.sumOf { it.size },
                            layerIds = manifest.layers.map { it.digest },
                            config = imageConfig
                        )
                        imageDao.insertImage(imageEntity)
                        return Result.success(factory.create(ImageState.Downloaded(manifest.config.digest)))
                    },
                    {
                        return Result.failure(it)
                    }
                )
            },
            {
                return Result.failure(it)
            }
        )
    }
}