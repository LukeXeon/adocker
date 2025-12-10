package com.github.adocker.daemon.containers

import com.freeletics.flowredux2.ExecutionPolicy
import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import com.github.adocker.daemon.app.AppContext
import com.github.adocker.daemon.database.dao.ContainerDao
import com.github.adocker.daemon.utils.deleteRecursively
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runInterruptible
import java.io.File
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
class StateMachineFactory @AssistedInject constructor(
    @Assisted
    private val initialState: ContainerState,
    private val containerDao: ContainerDao,
    private val appContext: AppContext,
    private val engine: PRootEngine,
) : FlowReduxStateMachineFactory<ContainerState, ContainerOperation>() {

    init {
        initializeWith { initialState }
        spec {
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
                    process.fold(
                        { process ->
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
                        },
                        { exception ->
                            override {
                                ContainerState.Dead(
                                    containerId,
                                    exception
                                )
                            }
                        }
                    )
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
                on<ContainerOperation.Exec>(
                    ExecutionPolicy.Ordered
                ) {
                    val process = startProcess(snapshot.containerId, it.command)
                    process.fold(
                        { process ->
                            mutate {
                                copy(
                                    otherProcesses = buildList(otherProcesses.size + 1) {
                                        addAll(otherProcesses)
                                        add(process)
                                    }
                                )
                            }
                        },
                        {
                            noChange()
                        }
                    )
                }
                on<ContainerOperation.Stop> {
                    override {
                        ContainerState.Stopping(
                            containerId,
                            buildList(otherProcesses.size + 1) {
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
                    removeContainer(snapshot.containerId)
                    override {
                        ContainerState.Terminated(containerId)
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

    private suspend fun removeContainer(containerId: String) {
        // Delete container directory
        val containerDir = File(appContext.containersDir, containerId)
        deleteRecursively(containerDir)
        // Delete from database
        containerDao.deleteContainerById(containerId)
    }

    private suspend fun startProcess(
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

    @Singleton
    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted
            initialState: ContainerState,
        ): StateMachineFactory
    }
}