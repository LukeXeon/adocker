package com.github.andock.daemon.images

import com.freeletics.flowredux2.ChangeableState
import com.freeletics.flowredux2.ChangedState
import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import com.github.andock.daemon.app.AppContext
import com.github.andock.daemon.client.ImageClient
import com.github.andock.daemon.client.ImageReference
import com.github.andock.daemon.client.model.ImageConfig
import com.github.andock.daemon.database.model.ImageEntity
import com.github.andock.daemon.images.ImageDownloadState.Downloading.Progress
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
class ImageDownloadStateMachine @AssistedInject constructor(
    @Assisted
    private val imageRef: ImageReference,
    private val client: ImageClient,
) : FlowReduxStateMachineFactory<ImageDownloadState, Unit>() {

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
            ImageDownloadState.Downloading()
        }
        spec {
            inState<ImageDownloadState.Downloading> {
                onEnter {
                    downloadImage()
                }
                on<Unit> {
                    override {
                        ImageDownloadState.Error(CancellationException())
                    }
                }
            }
        }
    }

    private suspend fun ChangeableState<ImageDownloadState.Downloading>.downloadImage(): ChangedState<ImageDownloadState> {
        try {
            val manifest = client.getManifest(imageRef).getOrThrow()
            snapshot.update {
                mapOf(
                    "manifest" to Progress(1, 1),
                    "config" to Progress(0, 1)
                )
            }
            val configDigest = manifest.config.digest
            val configResponse = client.getImageConfig(
                imageRef,
                configDigest
            ).getOrThrow()
            snapshot.update {
                mapOf(
                    "manifest" to Progress(1, 1),
                    "config" to Progress(1, 1)
                )
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
            return override {
                ImageDownloadState.Done
            }
        } catch (e: Exception) {
            return override {
                ImageDownloadState.Error(e)
            }
        }
    }

    @Singleton
    @AssistedFactory
    interface Factory {
        fun create(@Assisted imageRef: ImageReference): ImageDownloadStateMachine
    }
}