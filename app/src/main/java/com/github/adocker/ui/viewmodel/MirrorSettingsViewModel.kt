package com.github.adocker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adocker.core.registry.MirrorHealthChecker
import com.github.adocker.core.registry.RegistryRepository
import com.github.adocker.core.database.model.MirrorEntity
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
    private val healthChecker: MirrorHealthChecker,
    val json: Json,
) : ViewModel() {

    val allMirrors: StateFlow<List<MirrorEntity>> = registrySettings.getAllMirrorsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RegistryRepository.BUILT_IN_MIRRORS
        )

    val isCheckingHealth: StateFlow<Boolean> = healthChecker.isChecking
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun addCustomMirror(name: String, url: String, token: String? = null, priority: Int = 50) {
        viewModelScope.launch {
            registrySettings.addCustomMirror(name, url, token, priority)
        }
    }

    fun deleteCustomMirror(mirror: MirrorEntity) {
        viewModelScope.launch {
            registrySettings.deleteCustomMirror(mirror)
        }
    }

    fun checkMirrorsNow() {
        registrySettings.checkMirrorsNow()
    }
}
