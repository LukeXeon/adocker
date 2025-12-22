package com.github.adocker.ui.screens.mirrors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adocker.daemon.registries.RegistryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MirrorsViewModel @Inject constructor(
    private val registryManager: RegistryManager
) : ViewModel() {

    val mirrors = registryManager.registries

    fun addCustomMirror(name: String, url: String, token: String? = null, priority: Int = 50) {
        viewModelScope.launch {
            registryManager.addCustomMirror(name, url, token, priority)
        }
    }

    fun deleteCustomMirror(id: String) {
        viewModelScope.launch {
            registryManager.registries.value[id]?.delete()
        }
    }

    fun checkMirrorsNow() {
        viewModelScope.launch {
            registryManager.checkAllMirrors()
        }
    }
}