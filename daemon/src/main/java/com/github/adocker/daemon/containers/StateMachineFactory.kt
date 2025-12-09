package com.github.adocker.daemon.containers

import android.app.Application
import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import com.github.adocker.daemon.R
import com.github.adocker.daemon.app.AppContext
import com.github.adocker.daemon.database.dao.ContainerDao
import com.github.adocker.daemon.database.dao.ImageDao
import com.github.adocker.daemon.database.model.ContainerEntity
import com.github.adocker.daemon.utils.deleteRecursively
import com.github.adocker.daemon.utils.extractTarGz
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.completeWith
import kotlinx.coroutines.runInterruptible
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class StateMachineFactory @Inject constructor(
    containerDao: ContainerDao,
    imageDao: ImageDao,
    appContext: AppContext,
    engine: PRootEngine,
    application: Application,
) : FlowReduxStateMachineFactory<ContainerState, ContainerOperation>() {
    init {
        initializeWith { ContainerState.None }
        spec {
            suspend fun startProcess(
                containerId: String,
                command: List<String>? = null
            ): Result<Process> {
                val containerDir = File(appContext.containersDir, containerId)
                val rootfsDir = File(containerDir, AppContext.ROOTFS_DIR)
                if (!rootfsDir.exists()) {
                    return Result.failure(IllegalStateException("Container rootfs not found"))
                }
                val config = containerDao.getContainerById(containerId)?.config
                    ?: return Result.failure(
                        IllegalStateException("Container not found: $containerId")
                    )
                return engine.startProcess(
                    config,
                    rootfsDir,
                    command
                )
            }
            inState<ContainerState.None> {
                on<ContainerOperation.Load> {
                    override {
                        ContainerState.Loading(it.containerId)
                    }
                }
                on<ContainerOperation.Create> {
                    override {
                        ContainerState.Creating(
                            it.imageId,
                            it.name,
                            it.config
                        )
                    }
                }
            }
            inState<ContainerState.Creating> {
                /**
                 * Generate a random container name
                 */
                fun generateContainerName(): String {
                    val resources = application.resources
                    val adj = resources.getStringArray(R.array.adjectives).random()
                    val noun = resources.getStringArray(R.array.nouns).random()
                    val num = (1000..9999).random()
                    return "${adj}_${noun}_$num"
                }

                suspend fun generateContainerSafeName(): String {
                    var name = generateContainerName()
                    while (true) {
                        if (containerDao.getContainerByName(name) != null) {
                            name = generateContainerName()
                        } else {
                            break
                        }
                    }
                    return name
                }

                onEnter {
                    val (imageId, inputName, config) = snapshot
                    val image = imageDao.getImageById(imageId)
                    if (image == null) {
                        return@onEnter override {
                            ContainerState.Terminated(
                                IllegalArgumentException("Image not found: $imageId")
                            )
                        }
                    }
                    val containerName = if (inputName == null) {
                        generateContainerSafeName()
                    } else {
                        if (containerDao.getContainerById(inputName) != null) {
                            return@onEnter override {
                                ContainerState.Terminated(
                                    IllegalArgumentException("Container with name '${inputName}' already exists")
                                )
                            }
                        } else {
                            inputName
                        }
                    }

                    val containerId = UUID.randomUUID().toString()
                    // Create container directory structure
                    val containerDir = File(appContext.containersDir, containerId)
                    val rootfsDir = File(containerDir, AppContext.Companion.ROOTFS_DIR)
                    rootfsDir.mkdirs()
                    // Extract layers directly to rootfs
                    image.layerIds.forEach { digest ->
                        val layerFile =
                            File(appContext.layersDir, "${digest.removePrefix("sha256:")}.tar.gz")
                        if (layerFile.exists()) {
                            Timber.d("Extracting layer ${digest.take(16)} to container rootfs")
                            FileInputStream(layerFile).use { fis ->
                                extractTarGz(fis, rootfsDir).getOrThrow()
                            }
                            Timber.d("Layer ${digest.take(16)} extracted successfully")
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
                        } else config.workingDir,
                        user = if (config.user == "root") {
                            imageConfig?.user ?: config.user
                        } else config.user
                    )
                    val entity = ContainerEntity(
                        id = containerId,
                        name = containerName,
                        imageId = image.id,
                        imageName = "${image.repository}:${image.tag}",
                        config = mergedConfig
                    )
                    containerDao.insertContainer(entity)
                    return@onEnter override {
                        ContainerState.Created(containerId)
                    }
                }
            }
            inState<ContainerState.Loading> {
                onEnter {
                    val containerId = snapshot.containerId
                    val entity = containerDao.getContainerById(containerId)
                    when {
                        entity == null -> {
                            override {
                                ContainerState.Dead(
                                    containerId,
                                    IllegalArgumentException("Container not found: $containerId")
                                )
                            }
                        }

                        entity.lastRunAt != null -> {
                            override {
                                ContainerState.Exited(containerId)
                            }
                        }

                        else -> {
                            override {
                                ContainerState.Created(containerId)
                            }
                        }
                    }
                }
            }
            inState<ContainerState.Created> {
                on<ContainerOperation.Start> {
                    override {
                        ContainerState.Starting(
                            containerId
                        )
                    }
                }
                on<ContainerOperation.Remove> {
                    override {
                        ContainerState.Removing(containerId)
                    }
                }
            }
            inState<ContainerState.Starting> {
                onEnter {
                    val process = startProcess(snapshot.containerId)
                    process.fold({ process ->
                        override {
                            ContainerState.Running(
                                containerId,
                                process,
                                process.outputStream.bufferedWriter(),
                                File(appContext.logDir, AppContext.STDOUT),
                                File(appContext.logDir, AppContext.STDERR),
                                emptyList()
                            )
                        }
                    }, {
                        override {
                            ContainerState.Dead(
                                containerId,
                                process.exceptionOrNull()!!
                            )
                        }
                    })
                }
            }
            inState<ContainerState.Running> {
                onEnter {
                    runInterruptible {
                        snapshot.mainProcess.waitFor()
                    }
                    override {
                        ContainerState.Stopping(
                            containerId,
                            otherProcesses,
                        )
                    }
                }
                on<ContainerOperation.Exec> {
                    val process = startProcess(snapshot.containerId, it.command)
                    it.process.completeWith(process)
                    process.fold({
                        mutate {
                            copy(
                                otherProcesses = buildList {
                                    addAll(otherProcesses)
                                    add(process.getOrThrow())
                                }
                            )
                        }
                    }, {
                        noChange()
                    })
                }
                on<ContainerOperation.Stop> {
                    override {
                        ContainerState.Stopping(
                            containerId,
                            buildList {
                                add(mainProcess)
                                addAll(otherProcesses)
                            },
                        )
                    }
                }
            }
            inState<ContainerState.Stopping> {
                onEnter {
                    snapshot.processes.onEach {
                        it.destroy()
                    }.forEach {
                        runInterruptible {
                            it.waitFor()
                        }
                    }
                    override {
                        ContainerState.Exited(containerId)
                    }
                }
            }
            inState<ContainerState.Exited> {
                on<ContainerOperation.Start> {
                    override {
                        ContainerState.Starting(
                            containerId
                        )
                    }
                }
                on<ContainerOperation.Remove> {
                    override {
                        ContainerState.Removing(containerId)
                    }
                }
            }
            inState<ContainerState.Removing> {
                onEnter {
                    val containerId = snapshot.containerId
                    // Delete container directory
                    val containerDir = File(appContext.containersDir, containerId)
                    deleteRecursively(containerDir)
                    // Delete from database
                    containerDao.deleteContainerById(containerId)
                    override {
                        ContainerState.Terminated()
                    }
                }
            }
            inState<ContainerState.Dead> {
                on<ContainerOperation.Remove> {
                    val containerId = snapshot.containerId
                    override {
                        ContainerState.Removing(containerId)
                    }
                }
            }
        }
    }
}