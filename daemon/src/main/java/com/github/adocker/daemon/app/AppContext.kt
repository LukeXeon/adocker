package com.github.adocker.daemon.app

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ADocker configuration - dependency injected version
 *
 * All configuration is now properly managed through Hilt DI
 */
@Singleton
class AppContext @Inject constructor(
    @ApplicationContext context: Context
) {
    // Directories
    val baseDir = requireNotNull(context.dataDir)
    val containersDir = File(baseDir, DIR_CONTAINERS)
    val layersDir = File(baseDir, DIR_LAYERS)
    val tmpDir = requireNotNull(context.cacheDir)
    val logDir = File(tmpDir, LOG_DIR)
    val nativeLibDir = File(requireNotNull(context.applicationInfo.nativeLibraryDir))

    val socketFile = File(tmpDir, DOCKER_SOCK)
    val packageInfo = requireNotNull(
        context.packageManager.getPackageInfo(context.packageName, 0)
    )

    val applicationInfo = requireNotNull(
        packageInfo.applicationInfo
    )

    val isDebuggable: Boolean
        get() {
            return applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        }

    init {
        logDir.deleteRecursively()
        // Create directories on initialization
        listOf(
            containersDir,
            layersDir,
            logDir
        ).forEach { dir ->
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }

        Timber.d("AppConfig initialized: baseDir=${baseDir.absolutePath}")
    }


    companion object {

        // Docker Registry defaults
        const val DEFAULT_REGISTRY = "https://registry-1.docker.io"

        const val DEFAULT_ARCHITECTURE = "arm64"
        const val DEFAULT_OS = "linux"

        // Timeouts (milliseconds)
        const val NETWORK_TIMEOUT = 30000L
        const val DOWNLOAD_TIMEOUT = 300000L

        // Buffer sizes
        const val DOWNLOAD_BUFFER_SIZE = 8192

        // Directories (relative to app's files directory)
        const val DIR_CONTAINERS = "containers"
        const val DIR_LAYERS = "layers"
        const val ROOTFS_DIR = "rootfs"
        const val LOG_DIR = "log"

        const val DOCKER_SOCK = "docker.sock"

        const val STDOUT = "stdout"
        const val STDERR = "stderr"

        // OS and architecture

        val ARCHITECTURE = when (Build.SUPPORTED_ABIS.firstOrNull()) {
            "arm64-v8a" -> "arm64"
            "armeabi-v7a" -> "arm"
            "x86_64" -> "amd64"
            "x86" -> "386"
            else -> DEFAULT_ARCHITECTURE
        }

        val application by lazy(LazyThreadSafetyMode.PUBLICATION) {
            @SuppressLint("DiscouragedPrivateApi", "PrivateApi")
            Class.forName("android.app.ActivityThread")
                .getDeclaredMethod(
                    "currentApplication"
                ).apply {
                    isAccessible = true
                }.invoke(null) as Application
        }
    }
}