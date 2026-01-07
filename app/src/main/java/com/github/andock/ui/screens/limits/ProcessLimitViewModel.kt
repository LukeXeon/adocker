package com.github.andock.ui.screens.limits

import androidx.lifecycle.ViewModel
import com.github.andock.daemon.os.ProcessLimitCompat
import com.github.andock.daemon.shizuku.ShizukuApk
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProcessLimitViewModel @Inject constructor(
    private val processLimitCompat: ProcessLimitCompat,
    private val shizukuApk: ShizukuApk,
) : ViewModel() {
    suspend fun isUnrestricted() = processLimitCompat.isUnrestricted()

    suspend fun getMaxCount() = processLimitCompat.getMaxCount()

    suspend fun unrestrict() = processLimitCompat.unrestrict()

    suspend fun getInstallIntent() = shizukuApk.getInstallIntent()

}