package com.github.adocker.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adocker.daemon.database.model.ContainerEntity
import com.github.adocker.daemon.containers.ContainerRepository
import com.github.adocker.daemon.containers.ContainerExecutor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val containerRepository: ContainerRepository,
    private val containerExecutor: ContainerExecutor
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

    private var process: Process? = null
    private var stdin: BufferedWriter? = null
    private var stdout: BufferedReader? = null

    init {
        loadContainer()
    }

    private fun loadContainer() {
        viewModelScope.launch {
            _container.value = containerRepository.getContainerById(containerId)
        }
    }
    fun executeCommand(command: String) {
        if (command.isBlank()) return

        viewModelScope.launch {
            addOutput("$ $command")

            try {
                // Get the running container
                val runningContainer = containerExecutor.getAllRunningContainers()
                    .first()
                    .find { it.containerId == containerId }

                if (runningContainer == null || !runningContainer.isActive) {
                    addOutput("Error: Container is not running. Please start the container first.")
                    return@launch
                }

                // Execute command in the running container
                val result = runningContainer.execCommand(listOf("/bin/sh", "-c", command))

                result.onSuccess { process ->
                    withContext(Dispatchers.IO) {
                        val output = process.inputStream.bufferedReader().use { it.readText() }
                        val errorOutput = process.errorStream.bufferedReader().use { it.readText() }
                        process.waitFor()

                        withContext(Dispatchers.Main) {
                            if (output.isNotBlank()) {
                                output.lines().forEach { line ->
                                    addOutput(line)
                                }
                            }
                            if (errorOutput.isNotBlank()) {
                                errorOutput.lines().forEach { line ->
                                    addOutput(line)
                                }
                            }
                            if (process.exitValue() != 0) {
                                addOutput("Exit code: ${process.exitValue()}")
                            }
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
        viewModelScope.launch {
            try {
                stdin?.let {
                    withContext(Dispatchers.IO) {
                        it.write(input)
                        if (!input.endsWith("\n")) {
                            it.newLine()
                        }
                        it.flush()
                    }
                    addOutput("> $input")
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

    fun stopShell() {
        viewModelScope.launch {
            try {
                process?.destroy()
                process = null
                stdin = null
                stdout = null
                _isRunning.value = false
                addOutput("Shell terminated")
            } catch (e: Exception) {
                _error.value = "Error stopping shell: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        process?.destroy()
    }
}
