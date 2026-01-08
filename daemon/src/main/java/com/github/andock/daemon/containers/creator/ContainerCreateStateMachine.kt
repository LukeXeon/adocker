package com.github.andock.daemon.containers.creator

import com.freeletics.flowredux2.ChangeableState
import com.freeletics.flowredux2.ChangedState
import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import com.github.andock.daemon.app.AppContext
import com.github.andock.daemon.containers.Container
import com.github.andock.daemon.containers.ContainerManager
import com.github.andock.daemon.containers.ContainerState
import com.github.andock.daemon.database.dao.ContainerDao
import com.github.andock.daemon.database.dao.ImageDao
import com.github.andock.daemon.database.model.ContainerEntity
import com.github.andock.daemon.io.extractTarGz
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import timber.log.Timber
import java.io.File
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
class ContainerCreateStateMachine @AssistedInject constructor(
    @Assisted
    initialState: ContainerCreateState,
    private val imageDao: ImageDao,
    private val containerDao: ContainerDao,
    private val appContext: AppContext,
    private val containerFactory: Container.Factory,
    private val containerManager: ContainerManager,
) : FlowReduxStateMachineFactory<ContainerCreateState, CancellationException>() {

    init {
        initializeWith {
            initialState
        }
        spec {
            inState<ContainerCreateState.Creating> {
                onEnter {
                    createContainer()
                }
                on<CancellationException> { action ->
                    override {
                        ContainerCreateState.Error(id, action)
                    }
                }
            }
        }
    }

    private suspend fun ChangeableState<ContainerCreateState.Creating>.createContainer(): ChangedState<ContainerCreateState> {
        val imageId = snapshot.imageId
        val name = snapshot.name
        val config = snapshot.config
        val containerId = snapshot.id
        val image = imageDao.getImageById(imageId) ?: return override {
            ContainerCreateState.Error(
                id,
                IllegalArgumentException("Image not found: $imageId")
            )
        }
        val containerName = if (name == null) {
            containerDao.generateName()
        } else {
            if (containerDao.hasName(name)) {
                return override {
                    ContainerCreateState.Error(
                        id,
                        IllegalArgumentException("Container with name '${name}' already exists")
                    )
                }
            } else {
                name
            }
        }
        // Create container directory structure
        val rootfsDir = File(appContext.containersDir, containerId)
        rootfsDir.mkdirs()
        // Extract layers directly to rootfs
        for (digest in image.layerIds) {
            val layerFile = File(
                appContext.layersDir, "${digest.removePrefix("sha256:")}.tar.gz"
            )
            if (layerFile.exists()) {
                Timber.d("Extracting layer ${digest.take(16)} to container rootfs")
                extractTarGz(
                    layerFile,
                    rootfsDir
                ).fold(
                    {
                        Timber.d("Layer ${digest.take(16)} extracted successfully")
                    },
                    { throwable ->
                        Timber.e(throwable)
                        rootfsDir.runCatching {
                            deleteRecursively()
                        }
                        return override {
                            ContainerCreateState.Error(
                                id,
                                throwable
                            )
                        }
                    }
                )
            } else {
                Timber.w("Layer file not found: ${layerFile.absolutePath}")
            }
        }
        // Merge image config with provided config
        val imageConfig = image.config
        val mergedConfig = config.copy(
            cmd = if (config.cmd == listOf("/bin/sh")) {
                imageConfig?.cmd ?: imageConfig?.entrypoint ?: config.cmd
            } else config.cmd,
            entrypoint = config.entrypoint ?: imageConfig?.entrypoint,
            env = buildMap {
                imageConfig?.env?.forEach { envStr ->
                    val parts = envStr.split("=", limit = 2)
                    if (parts.size == 2) {
                        put(parts[0], parts[1])
                    }
                }
                putAll(config.env)
            },
            workingDir = if (config.workingDir == "/") {
                imageConfig?.workingDir ?: config.workingDir
            } else {
                config.workingDir
            },
            user = if (config.user == "root") {
                imageConfig?.user ?: config.user
            } else {
                config.user
            }
        )
        val entity = ContainerEntity(
            id = containerId,
            name = containerName,
            imageId = image.id,
            imageName = "${image.repository}:${image.tag}",
            config = mergedConfig
        )
        containerDao.insert(entity)
        val container = containerFactory.create(ContainerState.Created(containerId))
        containerManager.addContainer(container)
        return override {
            ContainerCreateState.Done(container)
        }
    }

    @Singleton
    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted
            initialState: ContainerCreateState,
        ): ContainerCreateStateMachine
    }
}