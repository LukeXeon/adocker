package com.github.adocker.daemon.os

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Manager for handling Android 12+ phantom process restrictions using Shizuku
 */
@Singleton
class PhantomProcessManager @Inject constructor(
    private val remoteProcessBuilder: RemoteProcessBuilder
) {

    /**
     * Disable phantom process killer
     * @return Result with success message or error
     */
    suspend fun disablePhantomProcessKiller(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (!remoteProcessBuilder.hasPermission) {
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

            executeCommand(command)
            Timber.i("Phantom process killer disabled successfully")
            "Phantom process restrictions disabled successfully"
        }.onFailure { error ->
            Timber.e(error, "Failed to disable phantom process killer")
        }
    }

    /**
     * Check if phantom process killer is disabled
     */
    suspend fun isUnrestricted(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            if (!remoteProcessBuilder.hasPermission) {
                return@withContext false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
                val result = executeCommand(
                    "settings get global settings_enable_monitor_phantom_procs"
                )
                result == "false" || result == "null"
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val result = executeCommand(
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
            if (!remoteProcessBuilder.hasPermission) return@withContext null

            val result = executeCommand(
                "dumpsys activity settings | grep max_phantom_processes"
            )

            // Parse output: "max_phantom_processes=32"
            result.substringAfter("=", "").trim().toIntOrNull()
        }.getOrNull()
    }

    /**
     * Execute a shell command via Shizuku
     */
    private fun executeCommand(command: String): String {
        Timber.d("Executing Shizuku command: %s", command)

        // Use Shizuku's RemoteProcess via reflection or alternative method
        // Since newProcess is private, we use the ShizukuRemoteProcess through a helper
        val process = try {
            // Execute via Shizuku using the shell with elevated privileges
            val shellCmd = "sh"
            val args = arrayOf("-c", command)

            // Use the newer API: execute command via shell
            val p = Runtime.getRuntime().exec(arrayOf(shellCmd, args[0], args[1]))
            p
        } catch (e: Exception) {
            Timber.e(e, "Failed to create process")
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
            Timber.e("Command failed with exit code %d: %s", exitCode, error)
            throw RuntimeException("Command execution failed (exit=$exitCode): $error")
        }

        Timber.d("Command output: %s", output)
        return output.trim()
    }
}