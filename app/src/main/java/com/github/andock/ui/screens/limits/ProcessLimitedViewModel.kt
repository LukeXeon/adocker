package com.github.andock.ui.screens.limits

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.andock.daemon.os.ProcessLimitCompat
import com.github.andock.daemon.os.RemoteProcessBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProcessLimitedViewModel @Inject constructor(
    private val remoteProcessBuilder: RemoteProcessBuilder,
    private val processLimitCompat: ProcessLimitCompat
) : ViewModel() {
    private val _stats = MutableStateFlow(ProcessLimitStats())
    val stats = _stats.asStateFlow()
    private suspend fun refresh() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            _stats.value = ProcessLimitStats(
                isAvailable = remoteProcessBuilder.isAvailable,
                hasPermission = remoteProcessBuilder.hasPermission,
                isUnrestricted = processLimitCompat.isUnrestricted(),
                currentLimit = processLimitCompat.getMaxCount()
            )
        }
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            viewModelScope.launch {
                while (isActive) {
                    refresh()
                    delay(1000)
                }
            }
        } else {
            _stats.value = ProcessLimitStats(
                isAvailable = false,
                hasPermission = false,
                isUnrestricted = true,
                currentLimit = 999999
            )
        }
    }

    suspend fun unrestrict(): Boolean {
        if (processLimitCompat.unrestrict()) {
            refresh()
            return true
        } else {
            return false
        }
    }

    fun requestPermission() {
        viewModelScope.launch {
            if (remoteProcessBuilder.requestPermission()) {
                refresh()
            }
        }
    }

}