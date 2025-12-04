package com.github.adocker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adocker.daemon.os.PhantomProcessManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PhantomProcessViewModel @Inject constructor(
    private val phantomProcessManager: PhantomProcessManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhantomProcessUiState())
    val uiState: StateFlow<PhantomProcessUiState> = _uiState.asStateFlow()

    init {
        checkStatus()
    }

    fun checkStatus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isChecking = true,
                error = null,
                successMessage = null
            )

            try {
                val shizukuAvailable = phantomProcessManager.isAvailable()
                val shizukuPermissionGranted = phantomProcessManager.hasPermission()

                val phantomKillerDisabled = if (shizukuPermissionGranted) {
                    phantomProcessManager.isUnrestricted()
                } else {
                    false
                }

                val currentLimit = if (shizukuPermissionGranted) {
                    phantomProcessManager.getCurrentPhantomProcessLimit()
                } else {
                    null
                }

                _uiState.value = PhantomProcessUiState(
                    shizukuAvailable = shizukuAvailable,
                    shizukuPermissionGranted = shizukuPermissionGranted,
                    phantomKillerDisabled = phantomKillerDisabled,
                    currentLimit = currentLimit,
                    isChecking = false
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to check phantom process status")
                _uiState.value = _uiState.value.copy(
                    isChecking = false,
                    error = "Failed to check status: ${e.message}"
                )
            }
        }
    }

    fun requestShizukuPermission() {
        phantomProcessManager.requestPermission()
        // Delay to allow permission dialog result
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            checkStatus()
        }
    }

    fun disablePhantomKiller() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                error = null,
                successMessage = null
            )

            phantomProcessManager.disablePhantomProcessKiller()
                .onSuccess { message ->
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        successMessage = message
                    )
                    checkStatus()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        error = error.message ?: "Unknown error occurred"
                    )
                }
        }
    }

    fun enablePhantomKiller() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                error = null,
                successMessage = null
            )

            phantomProcessManager.enablePhantomProcessKiller()
                .onSuccess { message ->
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        successMessage = message
                    )
                    checkStatus()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        error = error.message ?: "Unknown error occurred"
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}
