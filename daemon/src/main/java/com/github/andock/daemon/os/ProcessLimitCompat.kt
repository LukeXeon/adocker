package com.github.andock.daemon.os

import android.app.Application
import android.os.Build
import com.github.andock.daemon.R
import com.github.andock.daemon.shizuku.hasPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for handling Android 12+ phantom process restrictions using Shizuku
 */
@Singleton
class ProcessLimitCompat @Inject constructor(
    private val remoteProcessBuilder: RemoteProcessBuilder,
    private val application: Application,
) {
    /**
     * Disable phantom process killer
     * @return Result with success message or error
     */
    suspend fun unrestrict(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val command = when {
                Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> {
                    return@runCatching
                }

                !hasPermission -> {
                    throw SecurityException("Shizuku permission not granted")
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2 -> {
                    // Android 12L+ (API 32+)
                    application.getString(R.string.unrestrict_command_32)
                }

                else -> {
                    // Android 12 (API 31)
                    application.getString(R.string.unrestrict_command_31)
                }
            }
            executeCommand(command)
            Timber.i("Phantom process killer disabled successfully")
        }.fold(
            {
                true
            },
            { error ->
                Timber.e(error, "Failed to disable phantom process killer")
                false
            }
        )
    }

    /**
     * Check if phantom process killer is disabled
     */
    suspend fun isUnrestricted(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            when {
                Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> {
                    true
                }

                !hasPermission -> {
                    false
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2 -> {
                    val result = executeCommand(
                        application.getString(R.string.test_limit_command_32)
                    )
                    result == "false" || result == "null"
                }

                else -> {
                    val result = executeCommand(
                        application.getString(R.string.test_limit_command_31)
                    )
                    result.contains(
                        application.resources.getInteger(
                            R.integer.max_process_value
                        ).toString()
                    )
                }
            }
        }.getOrDefault(false)
    }

    /**
     * Get current phantom process limit
     */
    suspend fun getMaxCount(): Int = withContext(Dispatchers.IO) {
        runCatching {
            when {
                Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> {
                    application.resources.getInteger(R.integer.max_process_value)
                }

                !hasPermission -> {
                    null
                }

                else -> {
                    val result = executeCommand(application.getString(R.string.get_max_command))
                    // Parse output: "max_phantom_processes=32"
                    result.substringAfter("=", "").trim().toIntOrNull()
                }
            }
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
        val (exitCode, outputs) = supervisorScope {
            val jobs = arrayOf(process.inputStream, process.errorStream).map { out ->
                async {
                    out.bufferedReader().use { reader ->
                        reader.readText()
                    }
                }
            }
            process.await() to jobs.awaitAll()
        }
        val (output, error) = outputs
        if (exitCode != 0) {
            Timber.e("Command failed with exit code %d: %s", exitCode, error)
            throw RuntimeException("Command execution failed (exit=$exitCode): $error")
        }

        Timber.d("Command output: %s", output)
        return output.trim()
    }
}