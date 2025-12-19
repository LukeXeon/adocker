package com.github.adocker.daemon.containers

import com.freeletics.flowredux2.ChangeableState
import com.freeletics.flowredux2.ChangedState
import com.freeletics.flowredux2.ExecutionPolicy
import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import com.github.adocker.daemon.app.AppContext
import com.github.adocker.daemon.database.dao.ContainerDao
import com.github.adocker.daemon.io.deleteRecursively
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.io.IOException
import timber.log.Timber
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
                    snapshot.mainProcess.job.join()
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
        val config = containerDao.getContainerById(snapshot.id)?.config
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
                        logDir.mkdirs()
                        val stdout = File(logDir, AppContext.STDOUT)
                        val stderr = File(logDir, AppContext.STDERR)
                        scope.launch {
                            containerDao.setContainerLastRun(
                                containerId,
                                System.currentTimeMillis()
                            )
                        }
                        arrayOf(
                            mainProcess.stdout to stdout,
                            mainProcess.stderr to stderr
                        ).forEach { (input, output) ->
                            scope.launch(Dispatchers.IO) {
                                try {
                                    input.use { read ->
                                        output.outputStream().use { write ->
                                            read.copyTo(write)
                                        }
                                    }
                                } catch (e: IOException) {
                                    Timber.d(e)
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
            ContainerState.Exited(id)
        }
    }

    private suspend fun ChangeableState<ContainerState.Removing>.removeContainer(
        containerId: String
    ): ChangedState<ContainerState> {
        // Delete container directory
        val containerDir = File(appContext.containersDir, containerId)
        deleteRecursively(containerDir)
        // Delete from database
        containerManager.removeContainer(containerId)
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
                id,
                buildList(childProcesses.size + 1) {
                    add(mainProcess.job)
                    addAll(childProcesses.asSequence().map { it.job })
                }
            )
        }
    }

    private suspend fun ChangeableState<ContainerState.Running>.execCommand(exec: ContainerOperation.Exec): ChangedState<ContainerState> {
        val containerId = snapshot.id
        val config = containerDao.getContainerById(snapshot.id)?.config
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