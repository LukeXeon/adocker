package com.github.andock.daemon.containers

import com.freeletics.flowredux2.ChangeableState
import com.freeletics.flowredux2.ChangedState
import com.freeletics.flowredux2.ExecutionPolicy
import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import com.github.andock.daemon.app.AppContext
import com.github.andock.daemon.database.dao.ContainerDao
import com.github.andock.daemon.database.dao.LogLineDao
import com.github.andock.daemon.database.model.LogLineEntity
import com.github.andock.daemon.engine.PRootEngine
import com.github.andock.daemon.os.await
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.supervisorScope
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
    private val containerManager: ContainerManager,
    private val logLineDao: LogLineDao,
) : FlowReduxStateMachineFactory<ContainerState, ContainerOperation>() {

    init {
        initializeWith(reuseLastEmittedStateOnLaunch = false) { initialState }
        spec {
            inState<ContainerState.Created> {
                on<ContainerOperation.Start> {
                    override {
                        ContainerState.Starting(id)
                    }
                }
                on<ContainerOperation.Remove> {
                    override {
                        ContainerState.Removing(id)
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
                    snapshot.mainProcess.await()
                    toStoping()
                }
                untilIdentityChanges({ it.childProcesses }) {
                    onEnter {
                        anyExit()
                    }
                }
                on<ContainerOperation.Exec>(ExecutionPolicy.Unordered) {
                    execCommand(it)
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
                            id
                        )
                    }
                }
                on<ContainerOperation.Remove> {
                    override {
                        ContainerState.Removing(id)
                    }
                }
            }
            inState<ContainerState.Removing> {
                onEnter {
                    removeContainer(snapshot.id)
                }
            }
            inState<ContainerState.Dead> {
                on<ContainerOperation.Remove> {
                    override {
                        ContainerState.Removing(id)
                    }
                }
            }
        }
    }

    private suspend fun ChangeableState<ContainerState.Starting>.startContainer(): ChangedState<ContainerState> {
        val containerId = snapshot.id
        val config = containerDao.getContainerConfigById(snapshot.id)
        return if (config == null) {
            override {
                ContainerState.Removed(containerId)
            }
        } else {
            override {
                val process = prootEngine.startProcess(containerId, config = config)
                process.fold(
                    { mainProcess ->
                        val stdin = mainProcess.outputStream.bufferedWriter()
                        scope.launch {
                            containerDao.setContainerLastRun(
                                id = containerId,
                                timestamp = System.currentTimeMillis()
                            )
                            arrayOf(
                                mainProcess.inputStream to false,
                                mainProcess.errorStream to true
                            ).map { (input, isError) ->
                                launch {
                                    input.bufferedReader().useLines { lines ->
                                        lines.forEach { line ->
                                            logLineDao.append(
                                                LogLineEntity(
                                                    id = 0,
                                                    containerId = id,
                                                    timestamp = System.currentTimeMillis(),
                                                    isError = isError,
                                                    message = line
                                                )
                                            )
                                        }
                                    }
                                }
                            }.joinAll()
                        }
                        ContainerState.Running(
                            containerId,
                            mainProcess,
                            stdin
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
            it.destroy()
        }.forEach {
            it.await()
        }
        return override {
            ContainerState.Exited(id)
        }
    }

    private suspend fun ChangeableState<ContainerState.Removing>.removeContainer(
        containerId: String
    ): ChangedState<ContainerState> {
        // Delete container directory
        val containerDir = File(appContext.containersDir, containerId)
        containerDir.deleteRecursively()
        // Delete from database
        containerManager.removeContainer(containerId)
        return override {
            ContainerState.Removed(containerId)
        }
    }

    private suspend fun ChangeableState<ContainerState.Running>.anyExit(): ChangedState<ContainerState> {
        supervisorScope<Unit> {
            select {
                snapshot.childProcesses.forEach { process ->
                    launch {
                        process.await()
                    }.onJoin
                }
            }
        }
        return mutate {
            copy(
                childProcesses = buildList(childProcesses.size) {
                    addAll(
                        childProcesses.asSequence()
                            .filter { process -> process.isAlive }
                    )
                }
            )
        }
    }

    private fun ChangeableState<ContainerState.Running>.toStoping(): ChangedState<ContainerState> {
        return override {
            ContainerState.Stopping(
                id,
                buildList(childProcesses.size + 1) {
                    add(mainProcess)
                    addAll(childProcesses)
                }
            )
        }
    }

    private suspend fun ChangeableState<ContainerState.Running>.execCommand(exec: ContainerOperation.Exec): ChangedState<ContainerState> {
        val containerId = snapshot.id
        val config = containerDao.getContainerConfigById(snapshot.id)
        return if (config == null) {
            exec.process.completeExceptionally(
                IllegalStateException("Container not found: $containerId")
            )
            override {
                ContainerState.Removed(containerId)
            }
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