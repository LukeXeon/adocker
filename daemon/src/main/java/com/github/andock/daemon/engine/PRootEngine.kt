package com.github.andock.daemon.engine

import com.github.andock.daemon.app.AppContext
import com.github.andock.daemon.client.model.ContainerConfig
import com.github.andock.daemon.os.JobProcess
import com.github.andock.daemon.os.Process
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * PRoot execution engine
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
@Singleton
class PRootEngine @Inject constructor(
    private val appContext: AppContext,
    @param:Named("redirect")
    private val mapping: Map<String, String>,
    private val factory: JobProcess.Factory,
    private val prootVersion: PRootVersion
) {
    private val nativeLibDir = appContext.nativeLibDir

    /**
     * Execute PRoot directly from native lib dir (has apk_data_file SELinux context)
     * */
    private val prootBinary = File(nativeLibDir, "libproot.so")

    val version
        get() = prootVersion.value

    /**
     * Build the PRoot command for running a container
     */
    private fun buildCommand(
        container: ContainerConfig,
        rootfsDir: File,
        command: List<String>?,
    ): List<String> {
        val cmd = mutableListOf<String>()

        cmd.add(prootBinary.absolutePath)

        // Root emulation (fake root user)
        cmd.add("-0")

        // Set root directory
        cmd.add("-r")
        cmd.add(rootfsDir.absolutePath)

        // Working directory
        cmd.add("-w")
        cmd.add(container.workingDir)

        // Bind mounts
        container.binds.forEach { bind ->
            cmd.add("-b")
            val hostPath = mapping.getOrElse(bind.hostPath) { bind.hostPath }
            val bindStr = if (bind.readOnly) {
                "${hostPath}:${bind.containerPath}:ro"
            } else {
                "${hostPath}:${bind.containerPath}"
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
        val execCmd = command ?: buildExecCommand(container)
        cmd.addAll(execCmd)

        return cmd
    }

    /**
     * Build environment variables for the process
     */
    private fun buildEnvironment(container: ContainerConfig): Map<String, String> {
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
        val tmpDir = appContext.tmpDir
        tmpDir.mkdirs()  // Ensure directory exists
        env["PROOT_TMP_DIR"] = tmpDir.absolutePath

        // Default environment
        env["HOME"] = "/root"
        env["USER"] = container.user.ifEmpty { "root" }
        env["HOSTNAME"] = container.hostname
        env["TERM"] = "xterm-256color"
        env["PATH"] = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
        env["LANG"] = "C.UTF-8"

        // Container-specific environment
        env.putAll(container.env)

        // Android-specific
        env["ANDROID_ROOT"] = "/system"
        env["ANDROID_DATA"] = "/data"

        return env
    }

    private fun startProcess(
        container: ContainerConfig,
        rootfsDir: File,
        command: List<String>? = null
    ): Result<Process> = runCatching {
        val command = buildCommand(container, rootfsDir, command)
        val env = buildEnvironment(container)
        Timber.Forest.d("=== RUN IN CONTAINER ===")
        Timber.Forest.d("Command to execute: $command")
        Timber.Forest.d("PRoot command size: ${command.size}")
        command.forEachIndexed { index, arg ->
            Timber.Forest.d("  [$index] = '$arg'")
        }
        Timber.Forest.d("========================")
        Process(
            command = command,
            workingDir = rootfsDir,
            environment = env
        )
    }

    /**
     * Run a container and return immediately
     */
    fun startProcess(
        containerId: String,
        command: List<String>? = null,
        config: ContainerConfig = ContainerConfig(),
    ): Result<JobProcess> {
        val containerDir = File(appContext.containersDir, containerId)
        val rootfsDir = File(containerDir, AppContext.Companion.ROOTFS_DIR)
        if (!rootfsDir.exists()) {
            return Result.failure(IllegalStateException("Container rootfs not found"))
        }
        val process = startProcess(
            config,
            rootfsDir,
            command
        )
        return process.map {
            factory.create(it)
        }
    }


    companion object {
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

    }
}