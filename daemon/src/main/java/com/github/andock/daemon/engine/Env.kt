package com.github.andock.daemon.engine

import android.content.Context
import com.github.andock.common.nativeLibDir
import com.github.andock.daemon.app.socketFile
import java.io.File


val Context.binary
    get() = File(nativeLibDir, "libproot.so")

val Context.environment: Map<String, String>
    get() {
        val env = mutableMapOf<String, String>()
        // 64-bit loader
        val loaderPath = File(nativeLibDir, "libproot_loader.so")
        if (loaderPath.exists()) {
            env["PROOT_LOADER"] = loaderPath.absolutePath
        }

        // 32-bit loader (for running 32-bit programs on 64-bit devices)
        val loader32Path = File(nativeLibDir, "libproot_loader32.so")
        if (loader32Path.exists()) {
            env["PROOT_LOADER_32"] = loader32Path.absolutePath
        }

        // Set PROOT_TMP_DIR - PRoot needs a writable temporary directory
        // Use app's tmp directory which has write permissions
        val tmpDir = cacheDir
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
        return env
    }

/** Standard Docker socket path on Linux */
private const val DOCKER_SOCK_PATH = "/var/run/docker.sock"

val Context.redirect
    get() = mapOf(DOCKER_SOCK_PATH to socketFile.absolutePath)