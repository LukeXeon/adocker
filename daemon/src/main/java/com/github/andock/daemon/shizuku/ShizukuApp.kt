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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class ShizukuApp @Inject constructor(
    private val application: Application,
) {
    private val requests = MutableIntObjectMap<CompletableDeferred<Boolean>>()
    suspend fun requestPermission(): Boolean {
        when {
            !_isAlive -> {
                return false
            }

            !_hasPermission -> {
                var code: Int
                val deferred = CompletableDeferred<Boolean>()
                synchronized(this) {
                    while (true) {
                        code = Random.nextInt(1, UShort.MAX_VALUE.toInt())
                        if (!requests.containsKey(code)) {
                            requests[code] = deferred
                            break
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

    init {
        Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
            synchronized(this) {
                requests.remove(requestCode)
            }?.complete(
                grantResult == PackageManager.PERMISSION_GRANTED
            )
        }
    }
}