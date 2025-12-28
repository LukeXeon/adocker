package com.github.andock.daemon.engine

import com.github.andock.daemon.app.AppContext
import com.github.andock.daemon.os.Process
import com.github.andock.daemon.os.ProcessAwaiter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PRootVersion @Inject constructor(
    private val appContext: AppContext,
    private val processAwaiter: ProcessAwaiter,
    scope: CoroutineScope
) {
    private val nativeLibDir = appContext.nativeLibDir
    private val prootBinary = File(nativeLibDir, "libproot.so")
    private val _value = MutableStateFlow<String?>(null)

    val value = _value.asStateFlow()

    init {
        scope.launch {
            _value.value = loadVersion()
        }
    }

    private suspend fun loadVersion(): String? {
        when {
            !prootBinary.canExecute() -> {
                Timber.Forest.d("PRoot binary not executable: ${prootBinary.absolutePath}")
            }

            !prootBinary.exists() -> {
                Timber.Forest.d("PRoot binary not found at: ${prootBinary.absolutePath}")
            }

            else -> {
                Timber.Forest.d("Initializing PRoot from native lib dir: ${prootBinary.absolutePath}")
                // List files in native lib dir for debugging
                nativeLibDir.listFiles()?.forEach { file ->
                    Timber.Forest.d("  Native lib: ${file.name} (${file.length()} bytes)")
                }
                val env = buildProotEnvironment()
                try {
                    Timber.Forest.d("Running proot --version with env: $env")
                    val process = Process(
                        command = listOf(prootBinary.absolutePath, "--version"),
                        environment = env,
                        redirectErrorStream = false
                    )
                    val (code, outputs) = supervisorScope {
                        val jobs = sequenceOf(
                            process.inputStream,
                            process.errorStream
                        ).map { stream ->
                            async(Dispatchers.IO) {
                                stream.bufferedReader().useLines { lines ->
                                    val builder = StringBuilder()
                                    lines.forEach { builder.appendLine(it) }
                                    return@useLines builder
                                }
                            }
                        }.toList()
                        processAwaiter.await(process) to jobs.awaitAll()
                    }
                    val (stdout, stderr) = outputs
                    val available = code == 0 || stdout.contains("proot", ignoreCase = true)
                    if (!available) {
                        Timber.Forest.w("PRoot check failed. Exit code: ${code}, stdout: ${stdout}, stderr: $stderr")
                    } else {
                        Timber.Forest.d("PRoot available. Version output:\n $stdout")
                    }
                    return parseVersion(stdout)
                } catch (e: Exception) {
                    Timber.Forest.e(
                        e,
                        "Failed to get PRoot version，PRoot availability check failed with exception"
                    )
                }
            }
        }
        return null
    }

    /**
     * Build environment for PRoot process itself.
     *
     * The PRoot loader must be in the native lib directory as libproot_loader.so.
     * This is required because:
     * 1. PRoot uses an unbundled loader architecture
     * 2. The loader must also be in a location with executable SELinux context
     */
    private fun buildProotEnvironment(): Map<String, String> {
        val env = mutableMapOf<String, String>()

        // 64-bit loader
        val loaderPath = File(nativeLibDir, "libproot_loader.so")
        if (loaderPath.exists()) {
            env["PROOT_LOADER"] = loaderPath.absolutePath
            Timber.Forest.d("PROOT_LOADER set to: ${loaderPath.absolutePath}")
        } else {
            Timber.Forest.w("PRoot loader not found at: ${loaderPath.absolutePath}")
        }

        // 32-bit loader (for running 32-bit programs on 64-bit devices)
        val loader32Path = File(nativeLibDir, "libproot_loader32.so")
        if (loader32Path.exists()) {
            env["PROOT_LOADER_32"] = loader32Path.absolutePath
            Timber.Forest.d("PROOT_LOADER_32 set to: ${loader32Path.absolutePath}")
        }

        // Set PROOT_TMP_DIR - PRoot needs a writable temporary directory
        val tmpDir = appContext.tmpDir
        tmpDir.mkdirs()  // Ensure directory exists
        env["PROOT_TMP_DIR"] = tmpDir.absolutePath
        Timber.Forest.d("PROOT_TMP_DIR set to: ${tmpDir.absolutePath}")

        return env
    }

    companion object {
        private fun parseVersion(stdout: CharSequence): String {
            // PRoot --version outputs ASCII art + version info
            // Example output:
            //  _____ _____              ___
            // |  __ \  __ \_____  _____|   |_
            // |   __/     /  _  \/  _  \    _|
            // |__|  |__|__\_____/\_____/\____| 84a5bdf8-dirty
            //
            // built-in accelerators: process_vm = no, seccomp_filter = yes
            // ...

            // Extract version from the ASCII art line (last line of the art, contains version)
            val lines = stdout.lines()
            val versionLine = lines.find { it.contains("|__|") && it.contains("|") }

            if (versionLine != null) {
                // Extract version after the last |
                val version = versionLine.substringAfterLast("|").trim()
                if (version.isNotEmpty()) {
                    return "PRoot $version"
                }
            }

            // Fallback: look for "built-in accelerators" line
            val acceleratorsLine = lines.find { it.contains("built-in accelerators") }
            if (acceleratorsLine != null) {
                // Extract accelerator info
                val seccompSupport = if (acceleratorsLine.contains("seccomp_filter = yes")) {
                    "seccomp enabled"
                } else {
                    "seccomp disabled"
                }
                return "PRoot ($seccompSupport)"
            }

            // Last fallback: just return that PRoot is available
            return "PRoot (version unknown)"
        }
    }
}