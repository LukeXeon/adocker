package com.adocker.runner.core.config

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
    @ApplicationContext private val context: Context
) {
    // Directories
    val baseDir: File = context.filesDir
    val containersDir: File = File(baseDir, DIR_CONTAINERS)
    val layersDir: File = File(baseDir, DIR_LAYERS)
    val reposDir: File = File(baseDir, DIR_REPOS)
    val binDir: File = File(baseDir, DIR_BIN)
    val tmpDir: File = File(baseDir, DIR_TMP)

    val nativeLibDir: File? = context.applicationInfo?.nativeLibraryDir?.let { File(it) }

    init {
        // Create directories on initialization
        listOf(containersDir, layersDir, reposDir, binDir, tmpDir).forEach { dir ->
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }
        Timber.d("AppConfig initialized: baseDir=${baseDir.absolutePath}")
    }

    fun getArchitecture(): String {
        return when (Build.SUPPORTED_ABIS.firstOrNull()) {
            "arm64-v8a" -> "arm64"
            "armeabi-v7a" -> "arm"
            "x86_64" -> "amd64"
            "x86" -> "386"
            else -> DEFAULT_ARCHITECTURE
        }
    }

    companion object {
        // Version info
        const val VERSION = "1.0.0"
        const val APP_NAME = "ADocker"

        // Docker Registry defaults
        const val DEFAULT_REGISTRY = "https://registry-1.docker.io"
        const val DEFAULT_REGISTRY_AUTH = "https://auth.docker.io"
        const val DEFAULT_REGISTRY_SERVICE = "registry.docker.io"

        // API versions
        const val DOCKER_REGISTRY_API_V2 = "/v2"

        // Default image settings
        const val DEFAULT_IMAGE_TAG = "latest"
        const val DEFAULT_ARCHITECTURE = "arm64"
        const val DEFAULT_OS = "linux"

        // Execution modes
        const val EXEC_MODE_PROOT = "P1"
        const val EXEC_MODE_PROOT_NOSECCOMP = "P2"

        // Timeouts (milliseconds)
        const val NETWORK_TIMEOUT = 30000L
        const val DOWNLOAD_TIMEOUT = 300000L

        // Buffer sizes
        const val DOWNLOAD_BUFFER_SIZE = 8192
        const val TAR_BUFFER_SIZE = 65536

        // Directories (relative to app's files directory)
        const val DIR_CONTAINERS = "containers"
        const val DIR_LAYERS = "layers"
        const val DIR_REPOS = "repos"
        const val DIR_BIN = "bin"
        const val DIR_TMP = "tmp"

        // File names
        const val CONTAINER_JSON = "container.json"
        const val IMAGE_JSON = "image.json"
        const val ROOTFS_DIR = "ROOT"

        // PRoot settings
        const val PROOT_BINARY = "proot"
    }
}
