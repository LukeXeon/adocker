package com.adocker.runner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adocker.runner.core.config.RegistryMirror
import com.adocker.runner.core.config.RegistrySettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MirrorSettingsViewModel @Inject constructor() : ViewModel() {

    val allMirrors: StateFlow<List<RegistryMirror>> = RegistrySettings.getAllMirrorsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RegistrySettings.BUILT_IN_MIRRORS
        )

    val currentMirror: StateFlow<RegistryMirror?> = RegistrySettings.getCurrentMirrorFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun selectMirror(mirror: RegistryMirror) {
        viewModelScope.launch {
            RegistrySettings.setMirror(mirror)
        }
    }

    fun addCustomMirror(name: String, url: String) {
        viewModelScope.launch {
            RegistrySettings.addCustomMirror(name, url)
        }
    }

    fun deleteCustomMirror(mirror: RegistryMirror) {
        viewModelScope.launch {
            RegistrySettings.deleteCustomMirror(mirror)
        }
    }
}
