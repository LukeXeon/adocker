package com.github.adocker.daemon.containers

import com.freeletics.flowredux2.ChangeableState
import com.freeletics.flowredux2.ChangedState
import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.State
import com.freeletics.flowredux2.initializeWith
import com.github.adocker.daemon.app.AppContext
import com.github.adocker.daemon.database.dao.ContainerDao
import com.github.adocker.daemon.utils.deleteRecursively
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runInterruptible
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
    private val processStateMachineFactoryBuilder: ProcessStateMachineFactory.Builder,
) : FlowReduxStateMachineFactory<ContainerState, ContainerOperation>() {

    init {
        initializeWith { initialState }
        spec {
            inState<ContainerState> {
                condition({ it !is ContainerState.Running }) {
                    on<ContainerOperation.Exec> {
                        it.deferred.completeExceptionally(
                            IllegalStateException("Cannot exec: container is not running")
                        )
                        noChange()
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
                    startContainer()
                }
            }
            inState<ContainerState.Running> {
                onEnterStartStateMachine(
                    stateMachineFactoryBuilder = {
                        mainProcessStateMachine()
                    },
                    actionMapper = {},
                    cancelOnState = {
                        it.checkCancellation()
                    }
                ) { state ->
                    mainProcessStateUpdated(state)
                }
                onActionStartStateMachine<ContainerOperation.Exec, ProcessState, Unit>(
                    stateMachineFactoryBuilder = {
                        childProcessStateMachine(it)
                    },
                    actionMapper = {},
                    cancelOnState = {
                        it.checkCancellation()
                    }
                ) { state ->
                    childProcessStateUpdated(state)
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
                    val containerId = snapshot.containerId
                    override {
                        ContainerState.Removing(containerId)
                    }
                }
            }
        }
    }

    private suspend fun ChangeableState<ContainerState.Starting>.startContainer(): ChangedState<ContainerState> {
        val process = processBuilder.startProcess(snapshot.containerId)
        return process.fold(
            { mainProcess ->
                val stdin = mainProcess.outputStream.bufferedWriter()
                val logDir = File(appContext.logDir, snapshot.containerId)
                val stdout = File(logDir, AppContext.STDOUT)
                val stderr = File(logDir, AppContext.STDERR)
                override {
                    ContainerState.Running(
                        containerId,
                        mainProcess,
                        stdin,
                        stdout,
                        stderr
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

    private suspend fun ChangeableState<ContainerState.Stopping>.stopContainer(): ChangedState<ContainerState> {
        snapshot.mainProcess.destroy()
        snapshot.childProcesses.forEach {
            it.destroy()
        }
        withContext(Dispatchers.IO) {
            runInterruptible {
                snapshot.mainProcess.waitFor()
            }
            snapshot.childProcesses.forEach {
                runInterruptible {
                    it.waitFor()
                }
            }
        }
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
        return noChange()
    }

    private fun State<ContainerState.Running>.mainProcessStateMachine(): ProcessStateMachineFactory {
        return processStateMachineFactoryBuilder.build(
            ProcessState.Running(
                snapshot.mainProcess,
                snapshot.stdout,
                snapshot.stderr
            )
        )
    }

    private fun ProcessState.checkCancellation(): Boolean {
        return this is ProcessState.Exited || this is ProcessState.Abort
    }

    private fun ChangeableState<ContainerState.Running>.mainProcessStateUpdated(
        state: ProcessState
    ): ChangedState<ContainerState> {
        return if (state is ProcessState.Exited) {
            override {
                ContainerState.Stopping(
                    containerId,
                    mainProcess,
                    childProcesses
                )
            }
        } else {
            noChange()
        }
    }

    private fun State<ContainerState.Running>.childProcessStateMachine(
        exec: ContainerOperation.Exec
    ): ProcessStateMachineFactory {
        return processStateMachineFactoryBuilder.build(
            ProcessState.Starting(
                snapshot.containerId,
                exec.command,
                exec.deferred
            )
        )
    }

    private fun ChangeableState<ContainerState.Running>.childProcessStateUpdated(
        state: ProcessState
    ): ChangedState<ContainerState> {
        return when (state) {
            is ProcessState.Running -> {
                mutate {
                    copy(
                        childProcesses = buildSet(childProcesses.size + 1) {
                            addAll(childProcesses)
                            add(state.process)
                        }
                    )
                }
            }

            is ProcessState.Exited -> {
                mutate {
                    copy(
                        childProcesses = buildSet(childProcesses.size) {
                            addAll(childProcesses)
                            remove(state.process)
                        }
                    )
                }
            }

            else -> {
                noChange()
            }
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