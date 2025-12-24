package com.github.andock.daemon.os

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for handling Android 12+ phantom process restrictions using Shizuku
 */
@Singleton
class PhantomProcessKillerCompat @Inject constructor(
    private val remoteProcessBuilder: RemoteProcessBuilder,
    private val processAwaiter: ProcessAwaiter,
) {
    /**
     * Disable phantom process killer
     * @return Result with success message or error
     */
    suspend fun unrestrict(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!remoteProcessBuilder.hasPermission) {
                throw SecurityException("Shizuku permission not granted")
            }
            val command = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2 -> {
                    // Android 12L+ (API 32+)
                    "settings put global settings_enable_monitor_phantom_procs false"
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    // Android 12 (API 31)
                    "device_config set_sync_disabled_for_tests persistent && " +
                            "device_config put activity_manager max_phantom_processes 2147483647"
                }

                else -> {
                    throw UnsupportedOperationException("Android 12+ required for phantom process management")
                }
            }
            executeCommand(command)
            Timber.i("Phantom process killer disabled successfully")
        }.onFailure { error ->
            Timber.e(error, "Failed to disable phantom process killer")
        }
    }

    /**
     * Check if phantom process killer is disabled
     */
    suspend fun isUnrestricted(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            when {
                !remoteProcessBuilder.hasPermission -> {
                    false
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2 -> {
                    val result = executeCommand(
                        "settings get global settings_enable_monitor_phantom_procs"
                    )
                    result == "false" || result == "null"
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    val result = executeCommand(
                        "dumpsys activity settings | grep max_phantom_processes"
                    )
                    result.contains("2147483647")
                }

                else -> {
                    true // Android 12 以下不需要处理
                }
            }
        }.getOrDefault(false)
    }

    /**
     * Get current phantom process limit
     */
    suspend fun getMaxCount(): Int = withContext(Dispatchers.IO) {
        runCatching {
            if (!remoteProcessBuilder.hasPermission) {
                return@runCatching null
            }
            val result = executeCommand(
                "dumpsys activity settings | grep max_phantom_processes"
            )
            // Parse output: "max_phantom_processes=32"
            result.substringAfter("=", "").trim().toIntOrNull()
        }.getOrNull() ?: 32
    }

    /**
     * Execute a shell command via Shizuku
     */
    private suspend fun executeCommand(command: String): String {
        Timber.d("Executing Shizuku command: %s", command)
        // Use Shizuku's RemoteProcess via reflection or alternative method
        // Since newProcess is private, we use the ShizukuRemoteProcess through a helper
        val process = try {
            // Execute via Shizuku using the shell with elevated privileges
            // Use the newer API: execute command via shell
            remoteProcessBuilder.newProcess(arrayOf("sh", "-c", command))
        } catch (e: Exception) {
            Timber.e(e, "Failed to create process")
            throw RuntimeException("Failed to execute command: ${e.message}", e)
        }

        val output = process.inputStream.bufferedReader().use { reader ->
            reader.readText()
        }

        val error = process.errorStream.bufferedReader().use { reader ->
            reader.readText()
        }

        val exitCode = processAwaiter.await(process)

        if (exitCode != 0) {
            Timber.e("Command failed with exit code %d: %s", exitCode, error)
            throw RuntimeException("Command execution failed (exit=$exitCode): $error")
        }

        Timber.d("Command output: %s", output)
        return output.trim()
    }
}