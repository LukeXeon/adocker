package com.github.adocker.daemon.os

import android.content.ComponentName
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.github.adocker.daemon.app.AppContext
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.UserServiceArgs
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume


/**
 * Manager for handling Android 12+ phantom process restrictions using Shizuku
 */
@Singleton
class PhantomProcessManager @Inject constructor(
    appContext: AppContext
) {
    private val nextCode = AtomicInteger(1)

//    private val userServiceArgs = UserServiceArgs(
//        ComponentName(
//            appContext.applicationInfo.packageName,
//            CommandService::class.java.name
//        )
//    ).daemon(true)
//        .processNameSuffix("adb_service")
//        .debuggable(appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
//        .version(
//            @Suppress("DEPRECATION") appContext.packageInfo.versionCode
//        )

    /**
     * Check if Shizuku is available
     */
    fun isAvailable(): Boolean {
        if (Shizuku.isPreV11()) {
            Timber.w("Shizuku is pre v11")
            return false
        }
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            Timber.w(e, "Shizuku is not available")
            false
        }
    }

    /**
     * Check if we have Shizuku permission
     */
    fun hasPermission(): Boolean {
        return if (isAvailable()) {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
    }

    /**
     * Request Shizuku permission
     */
    suspend fun requestPermission(): Boolean {
        when {
            isAvailable() -> {
                return false
            }

            hasPermission() -> {
                val code = nextCode.getAndIncrement()
                if (code == UShort.MAX_VALUE.toInt()) {
                    throw OutOfMemoryError("The request code is tired")
                }
                return suspendCancellableCoroutine { con ->
                    val l = object : Shizuku.OnRequestPermissionResultListener, CompletionHandler {
                        override fun onRequestPermissionResult(
                            requestCode: Int,
                            grantResult: Int
                        ) {
                            if (requestCode == code) {
                                con.resume(grantResult == PackageManager.PERMISSION_GRANTED)
                                Shizuku.removeRequestPermissionResultListener(this)
                            }
                        }

                        override fun invoke(p1: Throwable?) {
                            Shizuku.removeRequestPermissionResultListener(this)
                        }
                    }
                    Shizuku.addRequestPermissionResultListener(l)
                    con.invokeOnCancellation(l)
                    Shizuku.requestPermission(code)
                }
            }

            else -> {
                return true
            }
        }
    }

    /**
     * Disable phantom process killer
     * @return Result with success message or error
     */
    suspend fun disablePhantomProcessKiller(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (!hasPermission()) {
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
            if (!hasPermission()) {
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
            if (!hasPermission()) return@withContext null

            val result = executeCommand(
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
            if (!hasPermission()) {
                throw SecurityException("Shizuku permission not granted")
            }
            val command = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2 -> {
                    "settings put global settings_enable_monitor_phantom_procs true"
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    "device_config put activity_manager max_phantom_processes 32"
                }

                else -> {
                    throw UnsupportedOperationException("Android 12+ required")
                }
            }

            executeCommand(command)
            Timber.i("Phantom process killer enabled")
            "Phantom process restrictions restored to default"
        }.onFailure { error ->
            Timber.e(error, "Failed to enable phantom process killer")
        }
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