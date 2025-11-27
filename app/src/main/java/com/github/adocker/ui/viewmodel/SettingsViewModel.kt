package com.github.adocker.ui.viewmodel

import android.content.Context
import android.content.pm.PackageInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adocker.daemon.config.AppConfig
import com.github.adocker.daemon.containers.PRootEngine
import com.github.adocker.daemon.utils.getDirectorySize
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prootEngine: PRootEngine,
    private val appConfig: AppConfig,
    @ApplicationContext val context: Context,
) : ViewModel() {

    private val _storageUsage = MutableStateFlow<Long?>(null)
    val storageUsage: StateFlow<Long?> = _storageUsage.asStateFlow()

    private val _prootVersion = MutableStateFlow<String?>(null)
    val prootVersion: StateFlow<String?> = _prootVersion.asStateFlow()

    val architecture = AppConfig.ARCHITECTURE
    val baseDir: String = appConfig.baseDir.absolutePath

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _storageUsage.value = getDirectorySize(appConfig.baseDir)
                _prootVersion.value = prootEngine.version
            }
        }
    }

    val packageInfo: PackageInfo
        get() = appConfig.packageInfo

    fun clearAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // Clear containers
                appConfig.containersDir.deleteRecursively()
                appConfig.containersDir.mkdirs()

                // Clear layers
                appConfig.layersDir.deleteRecursively()
                appConfig.layersDir.mkdirs()

                // Clear temp
                appConfig.tmpDir.deleteRecursively()
                appConfig.tmpDir.mkdirs()

                _storageUsage.value = getDirectorySize(appConfig.baseDir)
            }
            onComplete()
        }
    }

    fun refreshStorageUsage() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _storageUsage.value = getDirectorySize(appConfig.baseDir)
            }
        }
    }
}
