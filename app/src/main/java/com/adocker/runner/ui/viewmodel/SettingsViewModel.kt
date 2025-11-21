package com.adocker.runner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adocker.runner.core.config.Config
import com.adocker.runner.core.config.RegistryMirror
import com.adocker.runner.core.config.RegistrySettings
import com.adocker.runner.core.utils.FileUtils
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
    private val prootEngine: PRootEngine?
) : ViewModel() {

    private val _storageUsage = MutableStateFlow<Long?>(null)
    val storageUsage: StateFlow<Long?> = _storageUsage.asStateFlow()

    private val _prootVersion = MutableStateFlow<String?>(null)
    val prootVersion: StateFlow<String?> = _prootVersion.asStateFlow()

    private val _currentMirror = MutableStateFlow<RegistryMirror?>(null)
    val currentMirror: StateFlow<RegistryMirror?> = _currentMirror.asStateFlow()

    val availableMirrors: List<RegistryMirror> = RegistrySettings.AVAILABLE_MIRRORS

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _storageUsage.value = FileUtils.getDirectorySize(Config.baseDir)
                _prootVersion.value = prootEngine?.getVersion()
                _currentMirror.value = RegistrySettings.getCurrentMirror()
            }
        }
    }

    fun setRegistryMirror(mirror: RegistryMirror) {
        viewModelScope.launch {
            RegistrySettings.setMirror(mirror)
            _currentMirror.value = mirror
        }
    }

    fun clearAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // Clear containers
                Config.containersDir.deleteRecursively()
                Config.containersDir.mkdirs()

                // Clear layers
                Config.layersDir.deleteRecursively()
                Config.layersDir.mkdirs()

                // Clear temp
                Config.tmpDir.deleteRecursively()
                Config.tmpDir.mkdirs()

                _storageUsage.value = FileUtils.getDirectorySize(Config.baseDir)
            }
            onComplete()
        }
    }

    fun refreshStorageUsage() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _storageUsage.value = FileUtils.getDirectorySize(Config.baseDir)
            }
        }
    }
}
