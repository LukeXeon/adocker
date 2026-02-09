package com.github.andock.daemon.engine

import android.app.Application
import com.github.andock.common.nativeLibDir
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PRootEnvironment @Inject constructor(
    appContext: Application,
) {
    /**
     * Execute PRoot directly from native lib dir (has apk_data_file SELinux context)
     * */
    val binary = File(appContext.nativeLibDir, "libproot.so")

    val values: Map<String, String> = run {
        val env = mutableMapOf<String, String>()
        // 64-bit loader
        val loaderPath = File(appContext.nativeLibDir, "libproot_loader.so")
        if (loaderPath.exists()) {
            env["PROOT_LOADER"] = loaderPath.absolutePath
        }

        // 32-bit loader (for running 32-bit programs on 64-bit devices)
        val loader32Path = File(appContext.nativeLibDir, "libproot_loader32.so")
        if (loader32Path.exists()) {
            env["PROOT_LOADER_32"] = loader32Path.absolutePath
        }

        // Set PROOT_TMP_DIR - PRoot needs a writable temporary directory
        // Use app's tmp directory which has write permissions
        val tmpDir = appContext.cacheDir
        tmpDir.mkdirs()  // Ensure directory exists
        env["PROOT_TMP_DIR"] = tmpDir.absolutePath

        // Default environment
        env["HOME"] = "/root"
        env["TERM"] = "xterm-256color"
        env["PATH"] = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
        env["LANG"] = "C.UTF-8"

        // Android-specific
        env["ANDROID_ROOT"] = "/system"
        env["ANDROID_DATA"] = "/data"
        return@run env
    }
}