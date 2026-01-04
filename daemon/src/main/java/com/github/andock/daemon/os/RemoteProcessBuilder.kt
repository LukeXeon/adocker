package com.github.andock.daemon.os

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.github.andock.daemon.app.AppContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.UserServiceArgs
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.random.Random

@Singleton
class RemoteProcessBuilder @Inject constructor(
    appContext: AppContext,
) : Shizuku.OnRequestPermissionResultListener, ServiceConnection {
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

    /**
     * Check if Shizuku is available
     */
    val isAvailable: Boolean
        get() {
            if (Shizuku.isPreV11()) {
                Timber.w("Shizuku is pre v11")
                return false
            }
            return try {
                Shizuku.pingBinder()
            } catch (e: Exception) {
                Timber.w(e, "Shizuku is not available")
                false
            }
        }

    /**
     * Check if we have Shizuku permission
     */
    val hasPermission: Boolean
        get() {
            return if (isAvailable) {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } else {
                false
            }
        }

    private val requests = HashMap<Int, CancellableContinuation<Boolean>>()

    private fun toRequestCode(con: CancellableContinuation<Boolean>): Int {
        synchronized(requests) {
            while (true) {
                val code = Random.nextInt(1, UShort.MAX_VALUE.toInt())
                if (!requests.containsKey(code)) {
                    requests[code] = con
                    return code
                }
            }
        }
    }

    /**
     * Request Shizuku permission
     */
    suspend fun requestPermission(): Boolean {
        when {
            !isAvailable -> {
                return false
            }

            !hasPermission -> {
                return suspendCancellableCoroutine { con ->
                    val code = toRequestCode(con)
                    con.invokeOnCancellation {
                        synchronized(requests) {
                            requests.remove(code)
                        }
                    }
                    Shizuku.requestPermission(code)
                }
            }

            else -> {
                return true
            }
        }
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

    @Deprecated("Shizuku ues only", level = DeprecationLevel.HIDDEN)
    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        synchronized(requests) {
            requests.remove(requestCode)
        }?.resume(
            grantResult == PackageManager.PERMISSION_GRANTED
        )
    }

    init {
        Shizuku.addRequestPermissionResultListener(this)
    }
}