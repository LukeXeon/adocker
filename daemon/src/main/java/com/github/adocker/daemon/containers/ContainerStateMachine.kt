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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import java.io.File
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
class ContainerStateMachine @AssistedInject constructor(
    @Assisted
    initialState: ContainerState,
    private val containerDao: ContainerDao,
    private val appContext: AppContext,
    private val prootEngine: PRootEngine,
    private val scope: CoroutineScope,
) : FlowReduxStateMachineFactory<ContainerState, ContainerOperation>() {

    init {
        initializeWith(reuseLastEmittedStateOnLaunch = false) { initialState }
        spec {
            inState<ContainerState.Created> {
                on<ContainerOperation.Start> {
                    override {
                        ContainerState.Starting(containerId)
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
                    toStoping()
                }
                untilIdentityChanges({ it.childProcesses }) {
                    onEnter {
                        anyExit()
                    }
                }
                on<ContainerOperation.Exec> {
                    execProcess(it)
                }
                on<ContainerOperation.Stop> {
                    toStoping()
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
                val process = prootEngine.startProcess(containerId, config = config)
                process.fold(
                    { mainProcess ->
                        val stdin = mainProcess.stdin.bufferedWriter()
                        val logDir = File(appContext.logDir, containerId)
                        val stdout = File(logDir, AppContext.STDOUT)
                        val stderr = File(logDir, AppContext.STDERR)
                        arrayOf(
                            mainProcess.stdout to stdout,
                            mainProcess.stderr to stderr
                        ).forEach { (input, output) ->
                            scope.launch(Dispatchers.IO) {
                                input.use { input ->
                                    output.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                        }
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
        snapshot.processes.onEach {
            it.cancel()
        }.joinAll()
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

    private suspend fun ChangeableState<ContainerState.Running>.anyExit(): ChangedState<ContainerState> {
        select {
            snapshot.childProcesses.forEach { process ->
                process.job.onJoin {}
            }
        }
        return mutate {
            copy(
                childProcesses = buildList(childProcesses.size) {
                    addAll(
                        childProcesses.asSequence()
                            .filter { process -> process.job.isActive }
                    )
                }
            )
        }
    }

    private fun ChangeableState<ContainerState.Running>.toStoping(): ChangedState<ContainerState> {
        return override {
            ContainerState.Stopping(
                containerId,
                buildList(childProcesses.size + 1) {
                    add(mainProcess.job)
                    addAll(childProcesses.asSequence().map { it.job })
                }
            )
        }
    }

    private suspend fun ChangeableState<ContainerState.Running>.execProcess(exec: ContainerOperation.Exec): ChangedState<ContainerState> {
        val containerId = snapshot.containerId
        val config = containerDao.getContainerById(snapshot.containerId)?.config
        return if (config == null) {
            exec.process.completeExceptionally(
                IllegalStateException("Container not found: $containerId")
            )
            noChange()
        } else {
            mutate {
                val process = prootEngine.startProcess(
                    containerId,
                    exec.command,
                    config
                )
                process.fold(
                    { childProcess ->
                        exec.process.complete(childProcess)
                        copy(
                            childProcesses = buildList(childProcesses.size + 1) {
                                addAll(childProcesses)
                                add(childProcess)
                            }
                        )
                    },
                    { exception ->
                        exec.process.completeExceptionally(exception)
                        this
                    }
                )
            }
        }
    }

    @Singleton
    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted
            initialState: ContainerState,
        ): ContainerStateMachine
    }
}