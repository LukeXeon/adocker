package com.github.adocker.ui.screens.mirrors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adocker.daemon.database.model.MirrorEntity
import com.github.adocker.daemon.registry.MirrorHealthChecker
import com.github.adocker.daemon.registry.RegistryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class MirrorsViewModel @Inject constructor(
    private val registrySettings: RegistryRepository,
    healthChecker: MirrorHealthChecker,
) : ViewModel() {

    val allMirrors: StateFlow<List<MirrorEntity>> = registrySettings.getAllMirrors().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = RegistryRepository.BUILT_IN_MIRRORS
    )

    val isCheckingHealth: StateFlow<Boolean> = healthChecker.isChecking
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = false
        )

    // Track which mirrors are currently being checked
    val checkingMirrors: StateFlow<Set<String>> = healthChecker.checkingMirrors
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptySet()
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