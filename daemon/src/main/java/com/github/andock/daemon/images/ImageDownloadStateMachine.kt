package com.github.andock.daemon.images

import androidx.room.withTransaction
import com.freeletics.flowredux2.ChangeableState
import com.freeletics.flowredux2.ChangedState
import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import com.github.andock.daemon.app.AppContext
import com.github.andock.daemon.client.DownloadProgress
import com.github.andock.daemon.client.ImageClient
import com.github.andock.daemon.client.ImageReference
import com.github.andock.daemon.client.model.ImageConfig
import com.github.andock.daemon.database.AppDatabase
import com.github.andock.daemon.database.dao.ImageDao
import com.github.andock.daemon.database.dao.LayerDao
import com.github.andock.daemon.database.dao.LayerReferenceDao
import com.github.andock.daemon.database.model.ImageEntity
import com.github.andock.daemon.database.model.LayerReferenceEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
class ImageDownloadStateMachine @AssistedInject constructor(
    @Assisted
    imageRef: ImageReference,
    private val client: ImageClient,
    private val imageDao: ImageDao,
    private val layerDao: LayerDao,
    private val layerReferenceDao: LayerReferenceDao,
    private val database: AppDatabase,
    private val appContext: AppContext,
) : FlowReduxStateMachineFactory<ImageDownloadState, CancellationException>() {

    companion object {
        private fun getRegistryUrl(originalRegistry: String): String {
            return when {
                // Docker Hub - use best available mirror
                originalRegistry == "registry-1.docker.io"
                        || originalRegistry.contains("docker.io") -> {
                    AppContext.DEFAULT_REGISTRY
                }
                // Other registries - use as-is
                originalRegistry.startsWith("http") -> {
                    originalRegistry
                }

                else -> {
                    "https://$originalRegistry"
                }
            }
        }
    }

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
        }
    }

    private suspend fun ChangeableState<ImageDownloadState.Downloading>.downloadImage(): ChangedState<ImageDownloadState> {
        try {
            snapshot.updateProgress {
                mapOf(
                    "manifest" to DownloadProgress(0, 1)
                )
            }
            val imageRef = snapshot.imageRef
            val manifest = client.getManifest(imageRef).getOrThrow()
            snapshot.updateProgress {
                mapOf(
                    "manifest" to DownloadProgress(1, 1),
                    "config" to DownloadProgress(0, 1)
                )
            }
            val configDigest = manifest.config.digest
            val configResponse = client.getImageConfig(
                imageRef,
                configDigest
            ).getOrThrow()
            snapshot.updateProgress {
                mapOf(
                    "manifest" to DownloadProgress(1, 1),
                    "config" to DownloadProgress(1, 1)
                )
            }
            coroutineScope {
                manifest.layers.map { layer ->
                    launch {
                        val layerFile = File(
                            appContext.layersDir,
                            "${layer.digest.removePrefix("sha256:")}.tar.gz"
                        )
                        client.downloadLayer(
                            imageRef,
                            layer,
                            layerFile
                        ) { progress ->
                            snapshot.updateProgress { from ->
                                buildMap(from.size) {
                                    putAll(from)
                                    put(layer.digest, progress)
                                }
                            }
                        }.getOrThrow()
                    }
                }.joinAll()
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
            val imageEntity = ImageEntity(
                id = manifest.config.digest,
                registry = getRegistryUrl(imageRef.registry),
                repository = imageRef.repository,
                tag = imageRef.tag,
                architecture = configResponse.architecture ?: AppContext.ARCHITECTURE,
                os = configResponse.os ?: AppContext.DEFAULT_OS,
                size = manifest.layers.sumOf { it.size },
                layerIds = manifest.layers.map { it.digest },
                created = System.currentTimeMillis(),
                config = imageConfig
            )
            database.withTransaction {
                layerReferenceDao.insertLayerReference(
                    manifest.layers.map {
                        LayerReferenceEntity(
                            imageId = imageEntity.id,
                            layerId = it.digest
                        )
                    }
                )
                imageDao.insertImage(imageEntity)
            }
            return override {
                ImageDownloadState.Done(imageRef)
            }
        } catch (e: Exception) {
            return override {
                ImageDownloadState.Error(imageRef, e)
            }
        }
    }

    @Singleton
    @AssistedFactory
    interface Factory {
        fun create(@Assisted imageRef: ImageReference): ImageDownloadStateMachine
    }
}