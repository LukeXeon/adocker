package com.adocker.runner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adocker.runner.core.config.RegistrySettingsManager
import com.adocker.runner.data.local.entity.MirrorEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MirrorSettingsViewModel @Inject constructor(
    private val registrySettings: RegistrySettingsManager
) : ViewModel() {

    val allMirrors: StateFlow<List<MirrorEntity>> = registrySettings.getAllMirrorsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RegistrySettingsManager.BUILT_IN_MIRRORS
        )

    val currentMirror: StateFlow<MirrorEntity?> = registrySettings.getCurrentMirrorFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun selectMirror(mirror: MirrorEntity) {
        viewModelScope.launch {
            registrySettings.setMirror(mirror)
        }
    }

    fun addCustomMirror(name: String, url: String) {
        viewModelScope.launch {
            registrySettings.addCustomMirror(name, url)
        }
    }

    fun deleteCustomMirror(mirror: MirrorEntity) {
        viewModelScope.launch {
            registrySettings.deleteCustomMirror(mirror)
        }
    }
}
