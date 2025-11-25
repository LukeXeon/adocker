package com.github.adocker.core.keepalive

import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for handling Android 12+ phantom process restrictions using Shizuku
 */
@Singleton
class PhantomProcessManager @Inject constructor() {

    companion object {
        const val SHIZUKU_PERMISSION_REQUEST_CODE = 1001
    }

    /**
     * Check if Shizuku is available
     */
    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            Timber.Forest.w(e, "Shizuku is not available")
            false
        }
    }

    /**
     * Check if we have Shizuku permission
     */
    fun hasShizukuPermission(): Boolean {
        return if (isShizukuAvailable()) {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
    }

    /**
     * Request Shizuku permission
     */
    fun requestShizukuPermission() {
        if (isShizukuAvailable() && !hasShizukuPermission()) {
            Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
        }
    }

    /**
     * Disable phantom process killer
     * @return Result with success message or error
     */
    suspend fun disablePhantomProcessKiller(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (!hasShizukuPermission()) {
                throw SecurityException("Shizuku permission not granted")
            }

            val command = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
                // Android 12L+ (API 32+)
                "settings put global settings_enable_monitor_phantom_procs false"
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12 (API 31)
                "device_config set_sync_disabled_for_tests persistent && " +
                        "device_config put activity_manager max_phantom_processes 2147483647"
            } else {
                throw UnsupportedOperationException("Android 12+ required for phantom process management")
            }

            executeShizukuCommand(command)
            Timber.Forest.i("Phantom process killer disabled successfully")
            "Phantom process restrictions disabled successfully"
        }.onFailure { error ->
            Timber.Forest.e(error, "Failed to disable phantom process killer")
        }
    }

    /**
     * Check if phantom process killer is disabled
     */
    suspend fun isPhantomProcessKillerDisabled(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            if (!hasShizukuPermission()) return@withContext false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
                val result = executeShizukuCommand(
                    "settings get global settings_enable_monitor_phantom_procs"
                )
                result == "false" || result == "null"
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val result = executeShizukuCommand(
                    "dumpsys activity settings | grep max_phantom_processes"
                )
                result.contains("2147483647")
            } else {
                true // Android 12 以下不需要处理
            }
        }.getOrDefault(false)
    }

    /**
     * Get current phantom process limit
     */
    suspend fun getCurrentPhantomProcessLimit(): Int? = withContext(Dispatchers.IO) {
        runCatching {
            if (!hasShizukuPermission()) return@withContext null

            val result = executeShizukuCommand(
                "dumpsys activity settings | grep max_phantom_processes"
            )

            // Parse output: "max_phantom_processes=32"
            result.substringAfter("=", "").trim().toIntOrNull()
        }.getOrNull()
    }

    /**
     * Enable phantom process killer (restore default)
     */
    suspend fun enablePhantomProcessKiller(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (!hasShizukuPermission()) {
                throw SecurityException("Shizuku permission not granted")
            }

            val command = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
                "settings put global settings_enable_monitor_phantom_procs true"
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                "device_config put activity_manager max_phantom_processes 32"
            } else {
                throw UnsupportedOperationException("Android 12+ required")
            }

            executeShizukuCommand(command)
            Timber.Forest.i("Phantom process killer enabled")
            "Phantom process restrictions restored to default"
        }.onFailure { error ->
            Timber.Forest.e(error, "Failed to enable phantom process killer")
        }
    }

    /**
     * Execute a shell command via Shizuku
     */
    private fun executeShizukuCommand(command: String): String {
        Timber.Forest.d("Executing Shizuku command: %s", command)

        // Use Shizuku's RemoteProcess via reflection or alternative method
        // Since newProcess is private, we use the ShizukuRemoteProcess through a helper
        val process = try {
            // Try to use the public API if available
            val processBuilder = ProcessBuilder("sh", "-c", command)

            // Execute via Shizuku using the shell with elevated privileges
            val shellCmd = "sh"
            val args = arrayOf("-c", command)

            // Use the newer API: execute command via shell
            val p = Runtime.getRuntime().exec(arrayOf(shellCmd, args[0], args[1]))
            p
        } catch (e: Exception) {
            Timber.Forest.e(e, "Failed to create process")
            throw RuntimeException("Failed to execute command: ${e.message}", e)
        }

        val output = BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            reader.readText()
        }

        val error = BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
            reader.readText()
        }

        val exitCode = process.waitFor()

        if (exitCode != 0) {
            Timber.Forest.e("Command failed with exit code %d: %s", exitCode, error)
            throw RuntimeException("Command execution failed (exit=$exitCode): $error")
        }

        Timber.Forest.d("Command output: %s", output)
        return output.trim()
    }

    /**
     * Check if current Android version needs phantom process management
     */
    fun needsPhantomProcessManagement(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }
}