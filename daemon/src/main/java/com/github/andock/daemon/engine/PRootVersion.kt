package com.github.andock.daemon.engine

import com.github.andock.daemon.app.AppContext
import com.github.andock.daemon.os.JobProcess
import com.github.andock.daemon.os.Process
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PRootVersion @Inject constructor(
    private val appContext: AppContext,
    private val prootEnv: PRootEnvironment,
    private val factory: JobProcess.Factory,
    scope: CoroutineScope
) {
    val value by lazy {
        val state = MutableStateFlow<String?>(null)
        scope.launch {
            while (state.value == null) {
                state.value = withTimeoutOrNull(1000) {
                    loadVersion()
                }
            }
        }
        state.asStateFlow()
    }

    private suspend fun loadVersion(): String? {
        val prootBinary = prootEnv.binary
        val nativeLibDir = appContext.nativeLibDir
        when {
            !prootBinary.canExecute() -> {
                Timber.d("PRoot binary not executable: ${prootBinary.absolutePath}")
            }

            !prootBinary.exists() -> {
                Timber.d("PRoot binary not found at: ${prootBinary.absolutePath}")
            }

            else -> {
                Timber.d("Initializing PRoot from native lib dir: ${prootBinary.absolutePath}")
                // List files in native lib dir for debugging
                nativeLibDir.listFiles()?.forEach { file ->
                    Timber.d("  Native lib: ${file.name} (${file.length()} bytes)")
                }
                val env = prootEnv.values
                val process = factory.create(
                    Process(
                        command = listOf(prootBinary.absolutePath, "--version"),
                        environment = env,
                        redirectErrorStream = false
                    )
                )
                try {
                    Timber.d("Running proot --version with env: $env")
                    val (code, outputs) = supervisorScope {
                        val jobs = sequenceOf(
                            process.stdout,
                            process.stderr
                        ).map { stream ->
                            async(Dispatchers.IO) {
                                stream.bufferedReader().useLines { lines ->
                                    val builder = StringBuilder()
                                    lines.forEach { builder.appendLine(it) }
                                    return@useLines builder
                                }
                            }
                        }.toList()
                        process.job.await() to jobs.awaitAll()
                    }
                    val (stdout, stderr) = outputs
                    val available = code == 0 || stdout.contains("proot", ignoreCase = true)
                    if (!available) {
                        Timber.w("PRoot check failed. Exit code: ${code}, stdout: ${stdout}, stderr: $stderr")
                    } else {
                        Timber.d("PRoot available. Version output:\n $stdout")
                    }
                    return parseVersion(stdout)
                } catch (e: Exception) {
                    Timber.e(
                        e,
                        "Failed to get PRoot version，PRoot availability check failed with exception"
                    )
                } finally {
                    process.job.cancel()
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