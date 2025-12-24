package com.github.adocker.ui.screens.registries

import androidx.lifecycle.ViewModel
import com.github.adocker.daemon.registries.RegistryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddMirrorViewModel @Inject constructor(
    private val registryManager: RegistryManager
) : ViewModel() {
    suspend fun addCustomMirror(
        name: String,
        url: String,
        token: String? = null,
        priority: Int = 50
    ) {
        registryManager.addCustomMirror(name, url, token, priority)
    }
}