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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import java.io.File
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
class ContainerStateMachine @AssistedInject constructor(
    @Assisted
    private val initialState: ContainerState,
    private val containerDao: ContainerDao,
    private val appContext: AppContext,
    private val processBuilder: ContainerProcessBuilder,
    private val scope: CoroutineScope,
    private val subStateMachineFactory: SubProcessStateMachine.Factory,
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
                onEnter {
                    awaitContainer()
                }
                onActionStartStateMachine<ContainerOperation.Exec, SubProcessState, Unit>(
                    stateMachineFactoryBuilder = { action ->
                        subProcessStateMachine(action)
                    },
                    actionMapper = {},
                    cancelOnState = {
                        it is SubProcessState.Exited || it is SubProcessState.Error
                    }
                ) {
                    subProcessStateChanged(it)
                }
                on<ContainerOperation.Stop> {
                    override {
                        ContainerState.Stopping(
                            containerId,
                            job,
                            subProcesses,
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
            { process ->
                val stdin = process.outputStream.bufferedWriter()
                val logDir = File(appContext.logDir, snapshot.containerId)
                val stdout = File(logDir, AppContext.STDOUT)
                val stderr = File(logDir, AppContext.STDERR)
                val job = scope.launch {
                    arrayOf(
                        stdout to process.inputStream,
                        stderr to process.errorStream
                    ).forEach {
                        val (file, stream) = it
                        launch {
                            stream.use { input ->
                                file.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                    try {
                        runInterruptible {
                            process.waitFor()
                        }
                    } catch (e: CancellationException) {
                        process.destroy()
                        throw e
                    } finally {
                        process.waitFor()
                    }
                }
                override {
                    ContainerState.Running(
                        containerId,
                        job,
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

    private suspend fun ChangeableState<ContainerState.Running>.awaitContainer(): ChangedState<ContainerState> {
        snapshot.job.join()
        return override {
            ContainerState.Stopping(
                containerId,
                job,
                subProcesses,
            )
        }
    }

    private suspend fun ChangeableState<ContainerState.Stopping>.stopContainer(): ChangedState<ContainerState> {
        snapshot.job.cancel()
        snapshot.subProcesses.forEach {
            it.destroy()
        }
        snapshot.job.join()
        snapshot.subProcesses.forEach {
            runInterruptible {
                it.waitFor()
            }
        }
        return override {
            ContainerState.Exited(containerId)
        }
    }

    private suspend fun ChangeableState<ContainerState.Removing>.removeContainer(containerId: String): ChangedState<ContainerState> {
        // Delete container directory
        val containerDir = File(appContext.containersDir, containerId)
        deleteRecursively(containerDir)
        // Delete from database
        containerDao.deleteContainerById(containerId)
        return noChange()
    }

    private fun ChangeableState<ContainerState.Running>.subProcessStateChanged(state: SubProcessState): ChangedState<ContainerState> {
        return when (state) {
            is SubProcessState.Running -> {
                mutate {
                    copy(
                        subProcesses = buildSet {
                            addAll(subProcesses)
                            add(state.process)
                        }
                    )
                }
            }

            is SubProcessState.Exited -> {
                mutate {
                    copy(
                        subProcesses = buildSet {
                            addAll(subProcesses)
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

    private fun State<ContainerState.Running>.subProcessStateMachine(
        exec: ContainerOperation.Exec
    ): FlowReduxStateMachineFactory<SubProcessState, Unit> {
        return subStateMachineFactory.create(
            SubProcessState.Creating(
                snapshot.containerId,
                exec.command,
                exec.deferred
            )
        )
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