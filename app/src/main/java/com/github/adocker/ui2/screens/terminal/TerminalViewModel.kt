package com.github.adocker.ui2.screens.terminal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adocker.daemon.containers.ContainerManager
import com.github.adocker.daemon.containers.ContainerState
import com.github.adocker.daemon.database.model.ContainerEntity
import com.github.adocker.daemon.io.tailAsFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle, private val containerManager: ContainerManager
) : ViewModel() {

    private val containerId: String = savedStateHandle.get<String>("containerId") ?: ""

    private val _container = MutableStateFlow<ContainerEntity?>(null)
    val container: StateFlow<ContainerEntity?> = _container.asStateFlow()

    private val _outputLines = MutableStateFlow<List<String>>(emptyList())
    val outputLines: StateFlow<List<String>> = _outputLines.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var stdoutJob: Job? = null
    private var stderrJob: Job? = null

    init {
        loadContainer()
        startMonitoringContainer()
    }

    private fun startMonitoringContainer() {
        viewModelScope.launch {
            containerManager.containers.collect { containers ->
                val container = containers[containerId]
                val state = container?.state?.value

                if (state is ContainerState.Running) {
                    // Start tailing stdout and stderr if not already started
                    if (stdoutJob == null) {
                        stdoutJob = startTailingFlow(state.stdout, isError = false)
                    }
                    if (stderrJob == null) {
                        stderrJob = startTailingFlow(state.stderr, isError = true)
                    }
                    _isRunning.value = true
                } else {
                    // Stop tailing if container is not running
                    stopTailing()
                    _isRunning.value = false
                }
            }
        }
    }

    private fun startTailingFlow(file: File, isError: Boolean): Job {
        return viewModelScope.launch(Dispatchers.IO) {
            file.tailAsFlow(
                pollingDelay = 100, fromEnd = false, reOpen = false
            ).catch { e ->
                _error.value = "Tailer error: ${e.message}"
            }.collect { line ->
                    addOutput(if (isError) "[ERROR] $line" else line)
                }
        }
    }

    private fun stopTailing() {
        stdoutJob?.cancel()
        stdoutJob = null
        stderrJob?.cancel()
        stderrJob = null
    }

    private fun loadContainer() {
        viewModelScope.launch {
            val containers = containerManager.containers.value
            val container = containers[containerId]
            container?.getMetadata()?.onSuccess { entity ->
                _container.value = entity
            }
        }
    }

    fun executeCommand(command: String) {
        if (command.isBlank()) return

        viewModelScope.launch {
            addOutput("$ $command")

            try {
                // Get the container
                val containers = containerManager.containers.value
                val container = containers[containerId]

                if (container == null) {
                    addOutput("Error: Container not found.")
                    return@launch
                }

                // Check if container is running
                val state = container.state.value
                if (state !is ContainerState.Running) {
                    addOutput("Error: Container is not running. Please start the container first.")
                    return@launch
                }

                // Execute command in the container (output will be captured by Tailer)
                val result = container.exec(listOf("/bin/sh", "-c", command))

                result.onSuccess { containerProcess ->
                    // Wait for command to complete
                    withContext(Dispatchers.IO) {
                        containerProcess.job.join()

                        if (containerProcess.exitCode != 0) {
                            addOutput("Exit code: ${containerProcess.exitCode}")
                        }
                    }
                }.onFailure { e ->
                    addOutput("Error: ${e.message}")
                }

            } catch (e: Exception) {
                addOutput("Error: ${e.message}")
            }
        }
    }

    fun sendInput(input: String) {
        if (input.isBlank()) return

        viewModelScope.launch {
            try {
                // Get the container
                val containers = containerManager.containers.value
                val container = containers[containerId]
                val state = container?.state?.value

                if (state is ContainerState.Running) {
                    withContext(Dispatchers.IO) {
                        state.stdin.write(input)
                        if (!input.endsWith("\n")) {
                            state.stdin.newLine()
                        }
                        state.stdin.flush()
                    }
                    addOutput("> $input")
                } else {
                    addOutput("Error: Container is not running.")
                }
            } catch (e: Exception) {
                addOutput("Error sending input: ${e.message}")
            }
        }
    }

    fun clearOutput() {
        _outputLines.value = emptyList()
    }

    private fun addOutput(line: String) {
        _outputLines.value = _outputLines.value + line
    }

    fun stopContainer() {
        viewModelScope.launch {
            try {
                val containers = containerManager.containers.value
                val container = containers[containerId]

                container?.stop()
                addOutput("Container stopped")
            } catch (e: Exception) {
                _error.value = "Error stopping container: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTailing()
    }
}