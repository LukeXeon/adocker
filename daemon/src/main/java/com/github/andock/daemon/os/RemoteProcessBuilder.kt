package com.github.andock.daemon.os

import android.app.Application
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import com.github.andock.common.isDebuggable
import com.github.andock.common.packageInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.UserServiceArgs
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteProcessBuilder @Inject constructor(
    appContext: Application,
) : ServiceConnection {
    private val userServiceArgs = UserServiceArgs(
        ComponentName(
            appContext.applicationInfo.packageName,
            RemoteProcessBuilderService::class.java.name
        )
    ).daemon(false)
        .processNameSuffix("privilege")
        .debuggable(appContext.isDebuggable)
        .version(
            @Suppress("DEPRECATION")
            appContext.packageInfo.versionCode
        )

    private val connected = MutableStateFlow<IRemoteProcessBuilderService?>(null)
    private suspend fun getService(): IRemoteProcessBuilderService {
        val service = connected.value
        if (service != null) {
            return service
        }
        Shizuku.bindUserService(
            userServiceArgs,
            this
        )
        return connected.filterNotNull().first()
    }

    suspend fun newProcess(
        cmd: Array<String>,
        env: Array<String> = emptyArray(),
        dir: String? = null
    ): Process {
        return RemoteProcess(getService().newProcess(cmd, env, dir))
    }


    @Deprecated("Shizuku ues only", level = DeprecationLevel.HIDDEN)
    override fun onServiceConnected(
        name: ComponentName,
        service: IBinder
    ) {
        connected.value = IRemoteProcessBuilderService.Stub.asInterface(service)
    }

    @Deprecated("Shizuku ues only", level = DeprecationLevel.HIDDEN)
    override fun onServiceDisconnected(name: ComponentName) {
        connected.value = null
    }
}