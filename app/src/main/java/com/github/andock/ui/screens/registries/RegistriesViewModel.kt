package com.github.andock.ui.screens.registries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.andock.daemon.registries.RegistryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class RegistriesViewModel @Inject constructor(
    private val registryManager: RegistryManager,
    private val json: Json,
) : ViewModel() {
    val sortedList = registryManager.sortedList

    val bestServer = registryManager.bestServer

    suspend fun deleteCustomMirror(id: String) {
        registryManager.registries.value[id]?.remove()
    }

    suspend fun addScannedCode(
        scannedData: String,
    ): Boolean {
        json.runCatching {
            withContext(Dispatchers.IO) {
                decodeFromString<MirrorQrcode>(scannedData)
            }
        }.fold(
            { code ->
                val (name, url, token, priority) = code
                registryManager.addCustomMirror(name, url, token, priority)
                return true
            },
            {
                Timber.e(it)
                return false
            }
        )
    }

    suspend fun addCustomMirror(
        name: String,
        url: String,
        token: String? = null,
        priority: Int = 50
    ) {
        registryManager.addCustomMirror(name, url, token, priority)
    }

    fun checkAll() {
        viewModelScope.launch {
            registryManager.checkAll()
        }
    }
}