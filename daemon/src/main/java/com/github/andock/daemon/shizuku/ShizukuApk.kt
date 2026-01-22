package com.github.andock.daemon.shizuku

import android.app.Application
import android.content.Intent
import androidx.core.content.FileProvider
import com.github.andock.daemon.R
import com.github.andock.daemon.lazy.suspendLazy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShizukuApk @Inject constructor(
    private val application: Application
) {

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
}