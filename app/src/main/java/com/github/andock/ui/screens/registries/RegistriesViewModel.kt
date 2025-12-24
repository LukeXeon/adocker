package com.github.andock.ui.screens.registries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.andock.daemon.registries.RegistryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegistriesViewModel @Inject constructor(
    private val registryManager: RegistryManager
) : ViewModel() {
    val sortedList = registryManager.sortedList

    suspend fun deleteCustomMirror(id: String) {
        registryManager.registries.value[id]?.remove()
    }

    fun checkAll() {
        viewModelScope.launch {
            registryManager.checkAll()
        }
    }
}