package com.github.adocker.daemon.containers

import com.freeletics.flowredux2.ChangeableState
import com.freeletics.flowredux2.ChangedState
import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import com.github.adocker.daemon.app.AppContext
import com.github.adocker.daemon.database.dao.ContainerDao
import com.github.adocker.daemon.utils.deleteRecursively
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
class ContainerStateMachineFactory @AssistedInject constructor(
    @Assisted
    initialState: ContainerState,
    private val containerDao: ContainerDao,
    private val appContext: AppContext,
    private val processBuilder: ContainerProcessBuilder,
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
                    startContainer()
                }
            }
            inState<ContainerState.Running> {
                onEnter {
                    snapshot.mainProcess.job.join()
                    override {
                        ContainerState.Stopping(
                            containerId,
                            mainProcess,
                            childProcesses,
                        )
                    }
                }
                untilIdentityChanges({ it.childProcesses }) {
                    onEnter {
                        withContext(Dispatchers.IO) {
                            select {
                                snapshot.childProcesses.forEach { process ->
                                    launch {
                                        runInterruptible {
                                            process.waitFor()
                                        }
                                    }.onJoin {}
                                }
                            }
                        }
                        mutate {
                            copy(
                                childProcesses = buildSet(childProcesses.size) {
                                    childProcesses.forEach { process ->
                                        if (process.isAlive) {
                                            add(process)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
                on<ContainerOperation.Exec> {
                    val containerId = snapshot.containerId
                    val config = containerDao.getContainerById(snapshot.containerId)?.config
                    if (config == null) {
                        noChange()
                    } else {
                        mutate {
                            val process = processBuilder.startProcess(
                                containerId,
                                it.command,
                                config
                            )
                            process.fold(
                                { childProcess ->
                                    copy(
                                        childProcesses = buildSet(childProcesses.size + 1) {
                                            addAll(childProcesses)
                                            add(childProcess)
                                        }
                                    )
                                },
                                {
                                    this
                                }
                            )
                        }
                    }
                }
                on<ContainerOperation.Stop> {
                    override {
                        ContainerState.Stopping(
                            containerId,
                            mainProcess,
                            childProcesses,
                        )
                    }
                }
            }
            inState<ContainerState.Stopping> {
                onEnter {
                    stopContainer()
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

    private suspend fun ChangeableState<ContainerState.Starting>.startContainer(): ChangedState<ContainerState> {
        val containerId = snapshot.containerId
        val config = containerDao.getContainerById(snapshot.containerId)?.config
        return if (config == null) {
            override {
                ContainerState.Dead(
                    containerId,
                    IllegalStateException("Container not found: $containerId")
                )
            }
        } else {
            override {
                val process = processBuilder.startProcess(containerId, config = config)
                process.fold(
                    { mainProcess ->
                        val stdin = mainProcess.outputStream.bufferedWriter()
                        val logDir = File(appContext.logDir, containerId)
                        val stdout = File(logDir, AppContext.STDOUT)
                        val stderr = File(logDir, AppContext.STDERR)
                        ContainerState.Running(
                            containerId,
                            mainProcess,
                            stdin,
                            stdout,
                            stderr
                        )
                    },
                    { exception ->
                        ContainerState.Dead(
                            containerId,
                            exception
                        )
                    }
                )
            }
        }
    }

    private suspend fun ChangeableState<ContainerState.Stopping>.stopContainer(): ChangedState<ContainerState> {
        snapshot.mainProcess.job.cancel()
        snapshot.childProcesses.forEach { process ->
            process.job.cancel()
        }
        snapshot.mainProcess.job.join()
        sequenceOf(snapshot.mainProcess.job).plus()

        return override {
            ContainerState.Exited(containerId)
        }
    }

    private suspend fun ChangeableState<ContainerState.Removing>.removeContainer(
        containerId: String
    ): ChangedState<ContainerState> {
        // Delete container directory
        val containerDir = File(appContext.containersDir, containerId)
        deleteRecursively(containerDir)
        // Delete from database
        containerDao.deleteContainerById(containerId)
        return override {
            ContainerState.Removed(containerId)
        }
    }

    @Singleton
    @AssistedFactory
    interface Builder {
        fun build(
            @Assisted
            initialState: ContainerState,
        ): ContainerStateMachineFactory
    }
}