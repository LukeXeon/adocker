package com.github.adocker.ui2.screens.settings

import android.content.Context
import android.content.pm.PackageInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adocker.daemon.app.AppContext
import com.github.adocker.daemon.containers.PRootEngine
import com.github.adocker.daemon.io.getDirectorySize
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prootEngine: PRootEngine,
    private val appContext: AppContext,
    @param:ApplicationContext val context: Context,
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

    val packageInfo: PackageInfo
        get() = appContext.packageInfo

    fun clearAllData(onComplete: suspend () -> Unit) {
        viewModelScope.launch {
            // Clear containers
            appContext.containersDir.deleteRecursively()
            appContext.containersDir.mkdirs()

            // Clear layers
            appContext.layersDir.deleteRecursively()
            appContext.layersDir.mkdirs()

            // Clear temp
            appContext.tmpDir.deleteRecursively()
            appContext.tmpDir.mkdirs()

            _storageUsage.value = getDirectorySize(appContext.baseDir)
            onComplete()
        }
    }
}