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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PRootVersion @Inject constructor(
    private val appContext: AppContext,
    private val prootEnv: PRootEnvironment,
    private val processAwaiter: ProcessAwaiter,
    scope: CoroutineScope
) {
    private val _value = MutableStateFlow<String?>(null)

    val value = _value.asStateFlow()

    init {
        scope.launch {
            _value.value = loadVersion()
        }
    }

    private suspend fun loadVersion(): String? {
        val prootBinary = prootEnv.binary
        val nativeLibDir = appContext.nativeLibDir
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
                val env = prootEnv.values
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