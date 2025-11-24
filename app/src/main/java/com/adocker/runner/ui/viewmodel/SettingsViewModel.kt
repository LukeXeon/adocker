package com.adocker.runner.ui.viewmodel

import android.content.pm.PackageInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adocker.runner.core.config.AppConfig
import com.adocker.runner.core.config.RegistrySettingsManager
import com.adocker.runner.core.utils.FileUtils
import com.adocker.runner.data.local.entity.MirrorEntity
import com.adocker.runner.engine.proot.PRootEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prootEngine: PRootEngine?,
    private val appConfig: AppConfig,
    private val registrySettings: RegistrySettingsManager
) : ViewModel() {

    private val _storageUsage = MutableStateFlow<Long?>(null)
    val storageUsage: StateFlow<Long?> = _storageUsage.asStateFlow()

    private val _prootVersion = MutableStateFlow<String?>(null)
    val prootVersion: StateFlow<String?> = _prootVersion.asStateFlow()

    private val _currentMirror = MutableStateFlow<MirrorEntity?>(null)
    val currentMirror: StateFlow<MirrorEntity?> = _currentMirror.asStateFlow()

    val availableMirrors: List<MirrorEntity> = RegistrySettingsManager.AVAILABLE_MIRRORS

    val architecture: String = appConfig.architecture
    val baseDir: String = appConfig.baseDir.absolutePath

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _storageUsage.value = FileUtils.getDirectorySize(appConfig.baseDir)
                _prootVersion.value = prootEngine?.getVersion()
                _currentMirror.value = registrySettings.getCurrentMirror()
            }
        }
    }

    fun setRegistryMirror(mirror: MirrorEntity) {
        viewModelScope.launch {
            registrySettings.setMirror(mirror)
            _currentMirror.value = mirror
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

                _storageUsage.value = FileUtils.getDirectorySize(appConfig.baseDir)
            }
            onComplete()
        }
    }

    fun refreshStorageUsage() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _storageUsage.value = FileUtils.getDirectorySize(appConfig.baseDir)
            }
        }
    }
}
