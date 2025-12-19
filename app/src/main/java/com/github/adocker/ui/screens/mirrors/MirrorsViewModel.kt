package com.github.adocker.ui.screens.mirrors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adocker.daemon.mirrors.MirrorManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MirrorsViewModel @Inject constructor(
    private val mirrorManager: MirrorManager
) : ViewModel() {

    val mirrors = mirrorManager.mirrors

    fun addCustomMirror(name: String, url: String, token: String? = null, priority: Int = 50) {
        viewModelScope.launch {
            mirrorManager.addCustomMirror(name, url, token, priority)
        }
    }

    fun deleteCustomMirror(id: String) {
        viewModelScope.launch {
            mirrorManager.mirrors.value[id]?.delete()
        }
    }

    fun checkMirrorsNow() {
        viewModelScope.launch {
            mirrorManager.checkAllMirrors()
        }
    }
}