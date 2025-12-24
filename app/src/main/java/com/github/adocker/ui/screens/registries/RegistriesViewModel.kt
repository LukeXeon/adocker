package com.github.adocker.ui.screens.registries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adocker.daemon.registries.RegistryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegistriesViewModel @Inject constructor(
    private val registryManager: RegistryManager
) : ViewModel() {

    val registries = registryManager.registries



    suspend fun deleteCustomMirror(id: String) {
        registryManager.registries.value[id]?.remove()
    }

    fun checkAll() {
        viewModelScope.launch {
            registryManager.checkAll()
        }
    }
}