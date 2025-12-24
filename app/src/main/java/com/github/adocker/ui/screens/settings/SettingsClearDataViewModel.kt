package com.github.adocker.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.github.adocker.daemon.app.AppContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsClearDataViewModel @Inject constructor(
    private val appContext: AppContext
) : ViewModel() {
    suspend fun clearAllData() {
        withContext(Dispatchers.IO + NonCancellable) {
            // Clear containers
            appContext.containersDir.deleteRecursively()
            appContext.containersDir.mkdirs()

            // Clear layers
            appContext.layersDir.deleteRecursively()
            appContext.layersDir.mkdirs()

            // Clear temp
            appContext.tmpDir.deleteRecursively()
            appContext.tmpDir.mkdirs()
        }
    }
}