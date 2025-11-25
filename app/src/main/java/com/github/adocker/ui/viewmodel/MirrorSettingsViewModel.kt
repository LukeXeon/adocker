package com.github.adocker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adocker.data.repository.RegistryRepository
import com.github.adocker.data.local.model.MirrorEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class MirrorSettingsViewModel @Inject constructor(
    private val registrySettings: RegistryRepository,
    val json: Json,
) : ViewModel() {

    val allMirrors: StateFlow<List<MirrorEntity>> = registrySettings.getAllMirrorsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RegistryRepository.BUILT_IN_MIRRORS
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

    fun addCustomMirror(name: String, url: String, token: String? = null) {
        viewModelScope.launch {
            registrySettings.addCustomMirror(name, url, token)
        }
    }

    fun deleteCustomMirror(mirror: MirrorEntity) {
        viewModelScope.launch {
            registrySettings.deleteCustomMirror(mirror)
        }
    }
}
