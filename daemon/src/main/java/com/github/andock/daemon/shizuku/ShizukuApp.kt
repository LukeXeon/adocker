package com.github.andock.daemon.shizuku

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.collection.MutableIntObjectMap
import androidx.core.content.FileProvider
import com.github.andock.daemon.R
import com.github.andock.daemon.lazy.suspendLazy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import rikka.shizuku.Shizuku
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random

@Singleton
class ShizukuApp @Inject constructor(
    private val application: Application,
    private val scope: CoroutineScope
) {
    private val mutex = Mutex()
    private val requests = MutableIntObjectMap<CompletableDeferred<Boolean>>()
    suspend fun requestPermission(): Boolean {
        when {
            !_isAlive -> {
                return false
            }

            !_hasPermission -> {
                var code = 0
                val deferred = CompletableDeferred<Boolean>()
                val context = currentCoroutineContext()
                mutex.withLock {
                    while (context.isActive) {
                        code = Random.nextInt(1, UShort.MAX_VALUE.toInt())
                        if (!requests.containsKey(code)) {
                            requests[code] = deferred
                            break
                        } else {
                            yield()
                        }
                    }
                }
                Shizuku.requestPermission(code)
                return deferred.await()
            }

            else -> {
                return true
            }
        }
    }

    private val _isInstalled: Boolean
        get() {
            val packageManager = application.packageManager
            val packageName = "moe.shizuku.privileged.api"
            return try {
                // 适配Android 33+（Tiramisu）的包可见性限制，添加PackageManager.GET_META_DATA不影响检查结果
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getPackageInfo(
                        packageName,
                        PackageManager.PackageInfoFlags.of(PackageManager.GET_META_DATA.toLong())
                    )
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
                }
                true // 无异常，说明包已安装
            } catch (e: PackageManager.NameNotFoundException) {
                Timber.e(e)
                false // 捕获异常，说明包未安装
            }
        }

    val isInstalled
        get() = _isInstalled

    /**
     * Check if Shizuku is available
     */
    private val _isAlive: Boolean
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
    private val _hasPermission: Boolean
        get() {
            return if (_isAlive) {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } else {
                false
            }
        }

    val isAvailable
        get() = _hasPermission

    private val apkFile = suspendLazy {
        val apkFile = File(application.cacheDir, "shizuku.apk")
        withContext(Dispatchers.IO) {
            application.resources.openRawResource(R.raw.shizuku).use { input ->
                apkFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        return@suspendLazy apkFile
    }

    suspend fun getInstallIntent(): Result<Intent> = withContext(Dispatchers.IO) {
        runCatching {
            val apkFile = apkFile.getValue()
            val apkUri = FileProvider.getUriForFile(
                application,
                "${application.packageName}.fileprovider",
                apkFile
            )
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    apkUri,
                    "application/vnd.android.package-archive"
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
        }
    }

    private data class RequestPermissionResult(
        val requestCode: Int,
        val grantResult: Int
    ) : AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<RequestPermissionResult>
    }


    init {
        Shizuku.addRequestPermissionResultListener(
            object : suspend (CoroutineScope) -> Unit,
                Shizuku.OnRequestPermissionResultListener {

                override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                    scope.launch(RequestPermissionResult(requestCode, grantResult), block = this)
                }

                override suspend fun invoke(scope: CoroutineScope) {
                    val (requestCode, grantResult) = scope.coroutineContext[RequestPermissionResult]
                        ?: return
                    mutex.withLock {
                        requests.remove(requestCode)
                    }?.complete(
                        grantResult == PackageManager.PERMISSION_GRANTED
                    )
                }
            }
        )
    }
}