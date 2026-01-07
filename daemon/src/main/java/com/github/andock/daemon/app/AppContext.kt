package com.github.andock.daemon.app

import android.app.ActivityThread
import android.app.Application
import android.content.pm.ApplicationInfo
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
    context: Application
) {
    // Directories
    val baseDir = requireNotNull(context.dataDir)
    val containersDir = File(baseDir, DIR_CONTAINERS)
    val layersDir = File(baseDir, DIR_LAYERS)
    val tmpDir = requireNotNull(context.cacheDir)
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

    internal fun initializeDirs() {
        listOf(
            containersDir,
            layersDir,
        ).forEach { dir ->
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }
        Timber.d("AppContext initialized: baseDir=${baseDir.absolutePath}")
    }

    companion object {
        private const val DOCKER_SOCK = "docker.sock"

        private const val DIR_CONTAINERS = "containers"

        private const val DIR_LAYERS = "layers"

        val application by lazy(LazyThreadSafetyMode.PUBLICATION) {
            ActivityThread.currentApplication()
        }
    }
}