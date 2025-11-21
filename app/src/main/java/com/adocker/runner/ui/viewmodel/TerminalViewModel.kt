package com.adocker.runner.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adocker.runner.data.repository.ContainerRepository
import com.adocker.runner.domain.model.Container
import com.adocker.runner.engine.executor.ContainerExecutor
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
    private val containerExecutor: ContainerExecutor?
) : ViewModel() {

    private val containerId: String = savedStateHandle.get<String>("containerId") ?: ""

    private val _container = MutableStateFlow<Container?>(null)
    val container: StateFlow<Container?> = _container.asStateFlow()

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

    fun startShell() {
        if (containerExecutor == null) {
            _error.value = "PRoot engine not available"
            return
        }

        viewModelScope.launch {
            try {
                _isRunning.value = true
                addOutput("Starting shell...")

                val container = _container.value ?: run {
                    _error.value = "Container not found"
                    _isRunning.value = false
                    return@launch
                }

                // Execute shell command
                containerExecutor.execStreaming(containerId, listOf("/bin/sh"))
                    ?.collect { line ->
                        addOutput(line)
                    }
                    ?: run {
                        _error.value = "Failed to start shell"
                    }

            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _isRunning.value = false
            }
        }
    }

    fun executeCommand(command: String) {
        if (command.isBlank()) return

        viewModelScope.launch {
            addOutput("$ $command")

            if (containerExecutor == null) {
                addOutput("Error: PRoot engine not available")
                return@launch
            }

            try {
                val result = containerExecutor.execInContainer(
                    containerId = containerId,
                    command = listOf("/bin/sh", "-c", command),
                    timeout = 30000
                )

                result.onSuccess { execResult ->
                    if (execResult.output.isNotBlank()) {
                        execResult.output.lines().forEach { line ->
                            addOutput(line)
                        }
                    }
                    if (execResult.exitCode != 0) {
                        addOutput("Exit code: ${execResult.exitCode}")
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
