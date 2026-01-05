package com.github.andock.daemon.images.downloader

import androidx.room.withTransaction
import com.freeletics.flowredux2.ChangeableState
import com.freeletics.flowredux2.ChangedState
import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import com.github.andock.daemon.app.AppArchitecture
import com.github.andock.daemon.app.AppContext
import com.github.andock.daemon.database.AppDatabase
import com.github.andock.daemon.database.dao.ImageDao
import com.github.andock.daemon.database.dao.LayerDao
import com.github.andock.daemon.database.dao.LayerReferenceDao
import com.github.andock.daemon.database.model.ImageEntity
import com.github.andock.daemon.database.model.LayerEntity
import com.github.andock.daemon.database.model.LayerReferenceEntity
import com.github.andock.daemon.images.DownloadProgress
import com.github.andock.daemon.images.ImageReference
import com.github.andock.daemon.images.ImageRepositories
import com.github.andock.daemon.images.model.ImageConfig
import com.github.andock.daemon.io.sha256
import com.github.andock.daemon.registries.RegistryManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import java.io.File
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
class ImageDownloadStateMachine @AssistedInject constructor(
    @Assisted
    imageRef: ImageReference,
    repositories: ImageRepositories,
    private val imageDao: ImageDao,
    private val layerDao: LayerDao,
    private val layerReferenceDao: LayerReferenceDao,
    private val database: AppDatabase,
    private val appContext: AppContext,
    private val registryManager: RegistryManager,
) : FlowReduxStateMachineFactory<ImageDownloadState, CancellationException>() {
    private val imageRepository = repositories[
        registryManager.getBestServerUrl(imageRef.registry)
    ]

    init {
        initializeWith {
            ImageDownloadState.Downloading(imageRef)
        }
        spec {
            inState<ImageDownloadState.Downloading> {
                onEnter {
                    downloadImage()
                }
                on<CancellationException> { action ->
                    override {
                        ImageDownloadState.Error(imageRef, action)
                    }
                }
            }
            inState<ImageDownloadState.Error> {
                onEnter {
                    Timber.e(snapshot.throwable)
                    noChange()
                }
            }
        }
    }

    private suspend inline fun <T> ChangeableState<ImageDownloadState.Downloading>.downloadStep(
        name: String,
        total: Long,
        block: suspend () -> T
    ): T {
        var exception: Exception? = null
        snapshot.updateProgress { from ->
            buildMap(from.size + 1) {
                putAll(from)
                put(name, DownloadProgress(0, total))
            }
        }
        try {
            return block()
        } catch (e: Exception) {
            exception = e
            throw e
        } finally {
            if (exception == null) {
                snapshot.updateProgress { from ->
                    buildMap(from.size) {
                        putAll(from)
                        put(name, DownloadProgress(total, total))
                    }
                }
            }
        }
    }

    private suspend fun ChangeableState<ImageDownloadState.Downloading>.downloadImage(): ChangedState<ImageDownloadState> {
        try {
            val imageRef = snapshot.ref
            val manifest = downloadStep("manifest", 1) {
                imageRepository.getManifest(imageRef.repository, imageRef.tag).getOrThrow()
            }
            val configDigest = manifest.config.digest
            val imageId = configDigest.removePrefix("sha256:")
            var imageEntity = imageDao.getImageById(imageId)
            if (imageEntity != null) {
                return override {
                    ImageDownloadState.Done(ref)
                }
            }
            val configResponse = downloadStep("config", 1) {
                imageRepository.getImageConfig(
                    imageRef.repository,
                    configDigest
                ).getOrThrow()
            }
            val layers = coroutineScope {
                manifest.layers.map { layer ->
                    async {
                        val layer = LayerEntity(
                            id = layer.digest.removePrefix("sha256:"),
                            size = layer.size,
                            mediaType = layer.mediaType,
                        )
                        val id = layer.id
                        downloadStep(id, layer.size) {
                            val layerEntity = layerDao.getLayerById(id)
                            val destFile = File(
                                appContext.layersDir,
                                "${id}.tar.gz"
                            )
                            if (layerEntity != null
                                && layerEntity.size == destFile.length()
                                && destFile.sha256() == id
                            ) {
                                return@downloadStep
                            }
                            imageRepository.downloadLayer(
                                imageRef.repository,
                                layer,
                                destFile
                            ) { progress ->
                                snapshot.updateProgress { from ->
                                    buildMap(from.size) {
                                        putAll(from)
                                        put(layer.id, progress)
                                    }
                                }
                            }.getOrThrow()
                            val sha256 = destFile.sha256()
                            if (sha256 != id) {
                                throw IllegalStateException("Layer sha256 mismatch: ${sha256}!=${id}")
                            }
                            layerDao.insertLayer(
                                LayerEntity(
                                    id = id,
                                    size = layer.size,
                                    mediaType = layer.mediaType,
                                )
                            )
                        }
                        return@async layer
                    }
                }.awaitAll()
            }
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
            imageEntity = ImageEntity(
                id = imageId,
                registry = imageRef.registry,
                repository = imageRef.repository,
                tag = imageRef.tag,
                architecture = configResponse.architecture ?: AppArchitecture.DEFAULT,
                os = configResponse.os ?: AppArchitecture.OS,
                size = manifest.layers.sumOf { it.size },
                layerIds = manifest.layers.map { it.digest },
                created = System.currentTimeMillis(),
                config = imageConfig
            )
            val references = layers.map { layer ->
                LayerReferenceEntity(
                    imageId = imageId,
                    layerId = layer.id
                )
            }
            database.withTransaction {
                imageDao.insertImage(imageEntity)
                layerReferenceDao.insertLayerReferences(references)
            }
            return override {
                ImageDownloadState.Done(ref)
            }
        } catch (e: Exception) {
            registryManager.checkAll()
            return override {
                ImageDownloadState.Error(ref, e)
            }
        }
    }

    @Singleton
    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted imageRef: ImageReference,
        ): ImageDownloadStateMachine
    }
}