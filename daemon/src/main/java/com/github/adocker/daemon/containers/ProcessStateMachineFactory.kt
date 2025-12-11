package com.github.adocker.daemon.containers

import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.completeWith
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
class ProcessStateMachineFactory @AssistedInject constructor(
    @Assisted
    initialState: ProcessState,
    private val processBuilder: ContainerProcessBuilder
) : FlowReduxStateMachineFactory<ProcessState, Unit>() {

    init {
        initializeWith { initialState }
        spec {
            inState<ProcessState.Starting> {
                onEnter {
                    val process = processBuilder.startProcess(
                        snapshot.containerId,
                        snapshot.command
                    )
                    snapshot.deferred?.completeWith(process)
                    process.fold(
                        { process ->
                            override {
                                ProcessState.Running(
                                    process,
                                    stdout,
                                    stderr
                                )
                            }
                        },
                        { exception ->
                            override {
                                ProcessState.Abort(exception)
                            }
                        }
                    )
                }
            }
            inState<ProcessState.Running> {
                onEnter {
                    val (process, stdout, stderr) = snapshot
                    withContext(Dispatchers.IO) {
                        if (stdout != null) {
                            copyTo(process.inputStream, stdout)
                        }
                        if (stderr != null) {
                            copyTo(process.errorStream, stderr)
                        }
                        runInterruptible {
                            process.waitFor()
                        }
                    }
                    override {
                        ProcessState.Exited(process)
                    }
                }
            }
        }
    }

    private fun CoroutineScope.copyTo(input: InputStream, output: File) {
        launch {
            input.use { input ->
                output.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    @Singleton
    @AssistedFactory
    interface Builder {
        fun build(@Assisted initialState: ProcessState): ProcessStateMachineFactory
    }
}