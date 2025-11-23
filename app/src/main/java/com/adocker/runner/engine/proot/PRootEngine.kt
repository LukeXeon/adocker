package com.adocker.runner.engine.proot

import android.util.Log
import com.adocker.runner.core.config.Config
import com.adocker.runner.core.utils.FileUtils
import com.adocker.runner.core.utils.ProcessUtils
import com.adocker.runner.domain.model.Container
import com.adocker.runner.domain.model.ContainerConfig
import com.adocker.runner.domain.model.ExecResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

/**
 * PRoot execution engine - equivalent to udocker's PRootEngine
 *
 * PRoot allows running programs with a modified root directory without root privileges.
 * It uses ptrace to intercept system calls and translate paths.
 *
 * On Android 10+ (API 29+), binaries can only be executed from certain directories with
 * the correct SELinux context. The native library directory (/data/app/<pkg>/lib/<arch>)
 * has apk_data_file context which allows execution. Files in the app's data directory
 * (/data/data/<pkg>) have app_data_file context which blocks execution (execute_no_trans).
 *
 * Therefore, PRoot must be executed directly from the native library directory where
 * it was extracted from the APK's jniLibs folder.
 */
class PRootEngine(
    private val prootBinary: File,
    private val nativeLibDir: File? = prootBinary.parentFile
) {
    companion object {
        private const val TAG = "PRootEngine"

        // PRoot execution modes
        const val MODE_P1 = "P1"  // With SECCOMP
        const val MODE_P2 = "P2"  // Without SECCOMP (more compatible but slower)

        /**
         * Initialize PRoot engine with the binary from native libs directory.
         *
         * IMPORTANT: On Android 10+, PRoot must be executed directly from the
         * native library directory to avoid SELinux execute_no_trans denial.
         * Do NOT copy the binary to app data directory.
         */
        suspend fun initialize(nativeLibDir: File?): PRootEngine? = withContext(Dispatchers.IO) {
            if (nativeLibDir == null) {
                Log.e(TAG, "Native library directory is null")
                return@withContext null
            }

            // Execute PRoot directly from native lib dir (has apk_data_file SELinux context)
            val prootFile = File(nativeLibDir, "libproot.so")

            if (!prootFile.exists()) {
                Log.e(TAG, "PRoot binary not found at: ${prootFile.absolutePath}")
                return@withContext null
            }

            Log.d(TAG, "Initializing PRoot from native lib dir: ${prootFile.absolutePath}")
            PRootEngine(prootFile, nativeLibDir)
        }
    }

    /**
     * Build the PRoot command for running a container
     */
    fun buildCommand(
        container: Container,
        rootfsDir: File,
        command: List<String>? = null,
        mode: String = MODE_P1
    ): List<String> {
        val config = container.config
        val cmd = mutableListOf<String>()

        cmd.add(prootBinary.absolutePath)

        // Root emulation (fake root user)
        cmd.add("-0")

        // Set root directory
        cmd.add("-r")
        cmd.add(rootfsDir.absolutePath)

        // Working directory
        cmd.add("-w")
        cmd.add(config.workingDir)

        // Bind mounts
        config.binds.forEach { bind ->
            cmd.add("-b")
            val bindStr = if (bind.readOnly) {
                "${bind.hostPath}:${bind.containerPath}:ro"
            } else {
                "${bind.hostPath}:${bind.containerPath}"
            }
            cmd.add(bindStr)
        }

        // Essential bind mounts for Android
        addEssentialBinds(cmd, rootfsDir)

        // Note: Removed -k (kernel version spoofing) as it causes issues with Android PRoot
        // The -k option is for binding /proc/sys/kernel/* which doesn't work well on Android

        // Note: Removed -S (SECCOMP) option as it was incorrectly used.
        // The -S option in PRoot help is an alias for "-0 -r *path*", not for SECCOMP.
        // MODE_P1 and MODE_P2 distinction may need to be handled differently if needed.

        // The command to run
        val execCmd = command ?: buildExecCommand(config)
        cmd.addAll(execCmd)

        return cmd
    }

    /**
     * Add essential bind mounts for Android compatibility
     */
    private fun addEssentialBinds(cmd: MutableList<String>, rootfsDir: File) {
        // /dev bindings
        listOf("/dev/null", "/dev/zero", "/dev/random", "/dev/urandom").forEach { dev ->
            if (File(dev).exists()) {
                cmd.add("-b")
                cmd.add(dev)
            }
        }

        // /proc binding (limited on Android without root)
        cmd.add("-b")
        cmd.add("/proc")

        // /sys binding
        if (File("/sys").exists()) {
            cmd.add("-b")
            cmd.add("/sys")
        }

        // Android-specific bindings
        listOf("/system", "/vendor").forEach { path ->
            if (File(path).exists()) {
                cmd.add("-b")
                cmd.add("$path:$path:ro")
            }
        }

        // Bind /etc/resolv.conf for DNS
        val resolvConf = File(rootfsDir, "etc/resolv.conf")
        if (!resolvConf.exists()) {
            resolvConf.parentFile?.mkdirs()
            resolvConf.writeText("nameserver 8.8.8.8\nnameserver 8.8.4.4\n")
        }

        // Bind /etc/hosts
        val hostsFile = File(rootfsDir, "etc/hosts")
        if (!hostsFile.exists()) {
            hostsFile.parentFile?.mkdirs()
            hostsFile.writeText("127.0.0.1 localhost\n::1 localhost\n")
        }
    }

    /**
     * Build the execution command from container config
     */
    private fun buildExecCommand(config: ContainerConfig): List<String> {
        val cmd = mutableListOf<String>()

        // Entrypoint
        config.entrypoint?.let { cmd.addAll(it) }

        // Command
        if (cmd.isEmpty()) {
            cmd.addAll(config.cmd)
        } else {
            cmd.addAll(config.cmd)
        }

        return cmd.ifEmpty { listOf("/bin/sh") }
    }

    /**
     * Build environment variables for the process
     */
    fun buildEnvironment(container: Container): Map<String, String> {
        val env = mutableMapOf<String, String>()

        // Set PROOT_LOADER - must point to loader in native lib dir
        nativeLibDir?.let { libDir ->
            val loaderPath = File(libDir, "libproot_loader.so")
            if (loaderPath.exists()) {
                env["PROOT_LOADER"] = loaderPath.absolutePath
            }
        }

        // Set PROOT_TMP_DIR - PRoot needs a writable temporary directory
        // Use app's tmp directory which has write permissions
        val tmpDir = Config.tmpDir
        tmpDir.mkdirs()  // Ensure directory exists
        env["PROOT_TMP_DIR"] = tmpDir.absolutePath

        // Default environment
        env["HOME"] = "/root"
        env["USER"] = container.config.user.ifEmpty { "root" }
        env["HOSTNAME"] = container.config.hostname
        env["TERM"] = "xterm-256color"
        env["PATH"] = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
        env["LANG"] = "C.UTF-8"

        // Container-specific environment
        env.putAll(container.config.env)

        // Android-specific
        env["ANDROID_ROOT"] = "/system"
        env["ANDROID_DATA"] = "/data"

        return env
    }

    /**
     * Run a container and return immediately
     */
    suspend fun startContainer(
        container: Container,
        rootfsDir: File,
        mode: String = MODE_P1
    ): Result<Process> = withContext(Dispatchers.IO) {
        runCatching {
            val command = buildCommand(container, rootfsDir, mode = mode)
            val env = buildEnvironment(container)

            ProcessUtils.startInteractiveProcess(
                command = command,
                workingDir = rootfsDir,
                environment = env
            )
        }
    }

    /**
     * Execute a command in a container and wait for completion
     */
    suspend fun execInContainer(
        container: Container,
        rootfsDir: File,
        command: List<String>,
        mode: String = MODE_P1,
        timeout: Long = 0
    ): Result<ExecResult> = withContext(Dispatchers.IO) {
        runCatching {
            val prootCommand = buildCommand(container, rootfsDir, command, mode)
            val env = buildEnvironment(container)

            Log.d(TAG, "=== EXEC IN CONTAINER ===")
            Log.d(TAG, "Command to execute: $command")
            Log.d(TAG, "Mode: $mode")
            Log.d(TAG, "PRoot command size: ${prootCommand.size}")
            prootCommand.forEachIndexed { index, arg ->
                Log.d(TAG, "  [$index] = '$arg'")
            }
            Log.d(TAG, "========================")

            val result = ProcessUtils.execute(
                command = prootCommand,
                workingDir = rootfsDir,
                environment = env,
                timeout = timeout
            )

            ExecResult(
                exitCode = result.exitCode,
                output = result.stdout + result.stderr
            )
        }
    }

    /**
     * Execute a command and stream output
     */
    fun execStreaming(
        container: Container,
        rootfsDir: File,
        command: List<String>,
        mode: String = MODE_P1
    ): Flow<String> = flow {
        val prootCommand = buildCommand(container, rootfsDir, command, mode)
        val env = buildEnvironment(container)

        ProcessUtils.executeStreaming(
            command = prootCommand,
            workingDir = rootfsDir,
            environment = env
        ).collect { line ->
            emit(line)
        }
    }.flowOn(Dispatchers.IO)

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
        nativeLibDir?.let { libDir ->
            // The loader is packaged as libproot_loader.so in jniLibs
            val loaderPath = File(libDir, "libproot_loader.so")
            if (loaderPath.exists()) {
                env["PROOT_LOADER"] = loaderPath.absolutePath
                Log.d(TAG, "PROOT_LOADER set to: ${loaderPath.absolutePath}")
            } else {
                Log.w(TAG, "PRoot loader not found at: ${loaderPath.absolutePath}")
            }
        }

        // Set PROOT_TMP_DIR - PRoot needs a writable temporary directory
        val tmpDir = Config.tmpDir
        tmpDir.mkdirs()  // Ensure directory exists
        env["PROOT_TMP_DIR"] = tmpDir.absolutePath
        Log.d(TAG, "PROOT_TMP_DIR set to: ${tmpDir.absolutePath}")

        return env
    }

    /**
     * Check if PRoot binary is available and working
     */
    suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!prootBinary.exists()) {
                Log.w(TAG, "PRoot binary not found at: ${prootBinary.absolutePath}")
                return@withContext false
            }
            if (!prootBinary.canExecute()) {
                Log.w(TAG, "PRoot binary not executable: ${prootBinary.absolutePath}")
                return@withContext false
            }

            val env = buildProotEnvironment()
            Log.d(TAG, "Running proot --version with env: $env")

            val result = ProcessUtils.execute(
                command = listOf(prootBinary.absolutePath, "--version"),
                environment = env,
                timeout = 5000
            )

            val available =
                result.exitCode == 0 || result.stdout.contains("proot", ignoreCase = true)
            if (!available) {
                Log.w(
                    TAG,
                    "PRoot check failed. Exit code: ${result.exitCode}, stdout: ${result.stdout}, stderr: ${result.stderr}"
                )
            } else {
                Log.d(TAG, "PRoot available. Version output:\n ${result.stdout}")
            }
            available
        } catch (e: Exception) {
            Log.e(TAG, "PRoot availability check failed with exception", e)
            false
        }
    }

    /**
     * Get PRoot version
     */
    suspend fun getVersion(): String? = withContext(Dispatchers.IO) {
        try {
            val result = ProcessUtils.execute(
                command = listOf(prootBinary.absolutePath, "--version"),
                environment = buildProotEnvironment(),
                timeout = 5000
            )

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
            val lines = result.stdout.lines()
            val versionLine = lines.find { it.contains("|__|") && it.contains("|") }

            if (versionLine != null) {
                // Extract version after the last |
                val version = versionLine.substringAfterLast("|").trim()
                if (version.isNotEmpty()) {
                    return@withContext "PRoot $version"
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
                return@withContext "PRoot ($seccompSupport)"
            }

            // Last fallback: just return that PRoot is available
            "PRoot (version unknown)"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get PRoot version", e)
            null
        }
    }
}
