package com.github.adocker.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adocker.daemon.app.AppContext
import com.github.adocker.daemon.containers.PRootEngine
import com.github.adocker.daemon.io.getDirectorySize
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prootEngine: PRootEngine,
    private val appContext: AppContext,
) : ViewModel() {
    private val _storageUsage = MutableStateFlow<Long?>(null)
    val storageUsage: StateFlow<Long?> = _storageUsage.asStateFlow()
    val prootVersion: String?
        get() {
            return prootEngine.version
        }

    init {
        viewModelScope.launch {
            loadStorageUsage()
        }
    }

    suspend fun loadStorageUsage() {
        _storageUsage.value = getDirectorySize(appContext.baseDir)
    }

    val packageInfo
        get() = appContext.packageInfo
}