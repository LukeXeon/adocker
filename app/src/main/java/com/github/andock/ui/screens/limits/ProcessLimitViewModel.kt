package com.github.andock.ui.screens.limits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.andock.daemon.os.ProcessLimitCompat
import com.github.andock.daemon.os.RemoteProcessBuilder
import com.github.andock.daemon.utils.ShizukuApk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProcessLimitViewModel @Inject constructor(
    private val remoteProcessBuilder: RemoteProcessBuilder,
    private val processLimitCompat: ProcessLimitCompat,
    private val shizukuApk: ShizukuApk,
) : ViewModel() {
    private val _stats = MutableStateFlow(ProcessLimitStats())
    val stats = _stats.asStateFlow()
    private suspend fun refresh() {
        _stats.value = ProcessLimitStats(
            isAvailable = remoteProcessBuilder.isAvailable,
            hasPermission = remoteProcessBuilder.hasPermission,
            isUnrestricted = processLimitCompat.isUnrestricted(),
            currentLimit = processLimitCompat.getMaxCount()
        )
    }

    suspend fun scheduleRefresh() {
        val context = currentCoroutineContext()
        while (context.isActive) {
            refresh()
            delay(1000)
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

    suspend fun getInstallIntent() = shizukuApk.getInstallIntent()

}