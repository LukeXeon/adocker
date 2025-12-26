package com.github.andock.daemon.images

import com.github.andock.daemon.app.AppContext
import com.github.andock.daemon.client.ImageClient
import com.github.andock.daemon.client.ImageReference
import com.github.andock.daemon.client.model.ImageConfig
import com.github.andock.daemon.database.dao.ImageDao
import com.github.andock.daemon.database.model.ImageEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Singleton

class ImageDownloader @AssistedInject constructor(
    @Assisted
    private val imageRef: ImageReference,
    private val client: ImageClient,
    private val imageDao: ImageDao,
    private val imageManager: ImageManager,
    scope: CoroutineScope
) {
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

    sealed interface State {
        data class Downloading(val items: Map<String, Progress>) : State
        data class Error(val throwable: Throwable) : State
        object Done : State
    }

    data class Progress(
        val downloaded: Long,
        val total: Long,
    )

    private val _state = MutableStateFlow<State>(State.Downloading(emptyMap()))

    val state = _state.asStateFlow()

    init {
        scope.launch {
            runCatching {
                _state.value = State.Downloading(
                    mapOf(
                        "manifest" to Progress(0, 1)
                    )
                )
                val manifest = client.getManifest(imageRef).getOrThrow()
                _state.value = State.Downloading(
                    mapOf(
                        "manifest" to Progress(1, 1),
                        "config" to Progress(0, 1)
                    )
                )
                val configDigest = manifest.config.digest
                val configResponse = client.getImageConfig(
                    imageRef,
                    configDigest
                ).getOrThrow()
                _state.value = State.Downloading(
                    mapOf(
                        "manifest" to Progress(1, 1),
                        "config" to Progress(1, 1)
                    )
                )
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
            }.fold(
                {
                    _state.value = State.Done
                },
                {
                    _state.value = State.Error(it)
                }
            )
        }
    }

    @Singleton
    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted
            imageRef: ImageReference,
        ): ImageDownloader
    }
}