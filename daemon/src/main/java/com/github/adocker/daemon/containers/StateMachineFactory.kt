package com.github.adocker.daemon.containers

import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.github.adocker.daemon.app.AppContext
import com.github.adocker.daemon.database.dao.ContainerDao
import com.github.adocker.daemon.utils.deleteRecursively
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runInterruptible
import java.io.File
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class StateMachineFactory @Inject constructor(
    containerDao: ContainerDao,
    appContext: AppContext,
    engine: PRootEngine,
) : FlowReduxStateMachineFactory<ContainerState, ContainerOperation>() {
    init {
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
        spec {
            inState<ContainerState.None> {
                on<ContainerOperation.Load> {
                    override {
                        ContainerState.Loading(it.containerId)
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
                                    IllegalStateException("")
                                )
                            }
                        }

                        entity.lastRunAt == null -> {
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
                    val result = startProcess(snapshot.containerId)
                    if (result.isSuccess) {
                        override {
                            val process = result.getOrThrow()
                            ContainerState.Running(
                                containerId,
                                process,
                                process.outputStream.bufferedWriter(),
                                File(appContext.logDir, AppContext.STDOUT),
                                File(appContext.logDir, AppContext.STDERR),
                                emptyList()
                            )
                        }
                    } else {
                        override {
                            ContainerState.Dead(
                                containerId,
                                result.exceptionOrNull()!!
                            )
                        }
                    }
                }
            }
            inState<ContainerState.Running> {
                onEnter {
                    runInterruptible {
                        snapshot.mainProcess.waitFor()
                    }
                    override {
                        ContainerState.Exited(containerId)
                    }
                }
                on<ContainerOperation.Exec> {
                    val process = startProcess(snapshot.containerId, it.command)
                    it.callback(process)
                    if (process.isSuccess) {
                        mutate {
                            copy(
                                otherProcesses = buildList {
                                    addAll(otherProcesses)
                                    add(process.getOrThrow())
                                }
                            )
                        }
                    } else {
                        noChange()
                    }
                }
                on<ContainerOperation.Stop> {
                    snapshot.mainProcess.destroy()
                    noChange()
                }
            }
            inState<ContainerState.Stopping> {
                onEnter {
                    buildList {
                        add(snapshot.mainProcess)
                        addAll(snapshot.otherProcesses)
                    }.onEach {
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
                    noChange()
                }
            }
            inState<ContainerState.Dead> {
                on<ContainerOperation.Remove> {
                    override {
                        ContainerState.Removing(containerId)
                    }
                }
            }
        }
    }
}