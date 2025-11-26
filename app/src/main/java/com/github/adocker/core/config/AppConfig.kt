package com.github.adocker.core.config

import android.content.Context
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
class AppConfig @Inject constructor(
    @ApplicationContext context: Context
) {
    // Directories
    val baseDir = requireNotNull(context.filesDir)
    val containersDir = File(baseDir, DIR_CONTAINERS)
    val layersDir = File(baseDir, DIR_LAYERS)
    val reposDir = File(baseDir, DIR_REPOS)
    val binDir = File(baseDir, DIR_BIN)
    val tmpDir = File(baseDir, DIR_TMP)
    val nativeLibDir = File(requireNotNull(context.applicationInfo.nativeLibraryDir))
    val packageInfo = requireNotNull(
        context.packageManager.getPackageInfo(context.packageName, 0)
    )

    init {
        // Create directories on initialization
        listOf(
            containersDir,
            layersDir,
            reposDir,
            binDir,
            tmpDir
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
        const val DEFAULT_REGISTRY_SERVICE = "registry.docker.io"

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
        const val DIR_REPOS = "repos"
        const val DIR_BIN = "bin"
        const val DIR_TMP = "tmp"
        const val ROOTFS_DIR = "ROOT"
        val ARCHITECTURE = when (Build.SUPPORTED_ABIS.firstOrNull()) {
            "arm64-v8a" -> "arm64"
            "armeabi-v7a" -> "arm"
            "x86_64" -> "amd64"
            "x86" -> "386"
            else -> DEFAULT_ARCHITECTURE
        }
    }
}
