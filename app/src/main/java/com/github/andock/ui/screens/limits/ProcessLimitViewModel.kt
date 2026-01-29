package com.github.andock.ui.screens.limits

import androidx.lifecycle.ViewModel
import com.github.andock.daemon.os.ProcessLimitCompat
import com.github.andock.daemon.shizuku.ShizukuApp
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProcessLimitViewModel @Inject constructor(
    val processLimitCompat: ProcessLimitCompat,
    val shizukuApp: ShizukuApp,
) : ViewModel()