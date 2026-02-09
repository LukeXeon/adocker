package com.github.andock.ui.screens.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import com.github.andock.common.packageInfo
import com.github.andock.daemon.engine.PRootEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prootEngine: PRootEngine,
    private val appContext: Application,
) : ViewModel() {
    val prootVersion
        get() = prootEngine.version

    val packageInfo
        get() = appContext.packageInfo
}