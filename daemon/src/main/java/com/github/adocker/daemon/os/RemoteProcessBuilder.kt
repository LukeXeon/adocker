package com.github.adocker.daemon.os

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.IBinder
import com.github.adocker.daemon.app.AppContext
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.UserServiceArgs
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class RemoteProcessBuilder @Inject constructor(
    appContext: AppContext
) {
    private val nextCode = AtomicInteger(1)

    private val userServiceArgs = UserServiceArgs(
        ComponentName(
            appContext.applicationInfo.packageName,
            RemoteProcessBuilderService::class.java.name
        )
    ).daemon(false)
        .processNameSuffix("process_launch_service")
        .debuggable(appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
        .version(
            @Suppress("DEPRECATION") appContext.packageInfo.versionCode
        )

    private val connected = MutableStateFlow<IRemoteProcessBuilderService?>(null)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(
            name: ComponentName,
            service: IBinder
        ) {
            connected.value = IRemoteProcessBuilderService.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            connected.value = null
        }
    }

    private suspend fun getService(): IRemoteProcessBuilderService {
        val service = connected.value
        if (service != null) {
            return service
        }
        Shizuku.bindUserService(
            userServiceArgs,
            connection
        )
        return connected.filterNotNull().first()
    }

    suspend fun newProcess(
        cmd: Array<String>,
        env: Array<String>,
        dir: String?
    ): RemoteProcess {
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

    /**
     * Request Shizuku permission
     */
    suspend fun requestPermission(): Boolean {
        when {
            isAvailable -> {
                return false
            }

            hasPermission -> {
                val code = nextCode.getAndIncrement()
                if (code == UShort.MAX_VALUE.toInt()) {
                    throw OutOfMemoryError("The request code is tired")
                }
                return suspendCancellableCoroutine { con ->
                    val l = object : Shizuku.OnRequestPermissionResultListener, CompletionHandler {
                        override fun onRequestPermissionResult(
                            requestCode: Int,
                            grantResult: Int
                        ) {
                            if (requestCode == code) {
                                con.resume(grantResult == PackageManager.PERMISSION_GRANTED)
                                Shizuku.removeRequestPermissionResultListener(this)
                            }
                        }

                        override fun invoke(p1: Throwable?) {
                            Shizuku.removeRequestPermissionResultListener(this)
                        }
                    }
                    Shizuku.addRequestPermissionResultListener(l)
                    con.invokeOnCancellation(l)
                    Shizuku.requestPermission(code)
                }
            }

            else -> {
                return true
            }
        }
    }
}