package com.github.adocker.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adocker.daemon.os.PhantomProcessKillerCompat
import com.github.adocker.daemon.os.RemoteProcessBuilder
import com.github.adocker.ui.screens.settings.PhantomProcessUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PhantomProcessViewModel @Inject constructor(
    private val remoteProcessBuilder: RemoteProcessBuilder,
    private val phantomProcessKillerCompat: PhantomProcessKillerCompat
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
                val shizukuAvailable = remoteProcessBuilder.isAvailable
                val shizukuPermissionGranted = remoteProcessBuilder.hasPermission

                val phantomKillerDisabled = if (shizukuPermissionGranted) {
                    phantomProcessKillerCompat.isUnrestricted()
                } else {
                    false
                }

                val currentLimit = if (shizukuPermissionGranted) {
                    phantomProcessKillerCompat.getMaxCount()
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
                Timber.Forest.e(e, "Failed to check phantom process status")
                _uiState.value = _uiState.value.copy(
                    isChecking = false,
                    error = "Failed to check status: ${e.message}"
                )
            }
        }
    }

    fun requestShizukuPermission() {
        viewModelScope.launch {
            remoteProcessBuilder.requestPermission()
            // Delay to allow permission dialog result
            delay(500)
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

            phantomProcessKillerCompat.unrestrict()
                .onSuccess { message ->
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        successMessage = "Phantom process restrictions disabled successfully"
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

    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}