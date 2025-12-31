package com.github.andock.ui.screens.settings

import android.app.Application
import android.app.usage.StorageStatsManager
import android.os.Process
import android.os.UserHandle
import android.os.storage.StorageManager
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModel
import com.github.andock.daemon.app.AppContext
import com.github.andock.daemon.engine.PRootEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prootEngine: PRootEngine,
    private val appContext: AppContext,
    private val application: Application,
) : ViewModel() {
    private val _storageUsage = MutableStateFlow<Long?>(null)
    val storageUsage: StateFlow<Long?> = _storageUsage.asStateFlow()
    val prootVersion
        get() = prootEngine.version

    suspend fun loadStorageUsage() {
        val storageStatsManager = application.getSystemService<StorageStatsManager>() ?: return
        val storageManager = application.getSystemService<StorageManager>() ?: return
        withContext(Dispatchers.IO) {
            val storageVolume = storageManager.primaryStorageVolume
            val uuid = storageVolume.uuid?.let { UUID.fromString(it) }
                ?: StorageManager.UUID_DEFAULT
            val storageStats = storageStatsManager.queryStatsForPackage(
                uuid,
                application.packageName,
                UserHandle.getUserHandleForUid(Process.myUid())
            )
            _storageUsage.value = storageStats.dataBytes
        }
    }

    val packageInfo
        get() = appContext.packageInfo
}