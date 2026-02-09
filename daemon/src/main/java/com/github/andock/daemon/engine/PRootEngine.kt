package com.github.andock.daemon.engine

import android.app.Application
import com.github.andock.daemon.app.containersDir
import com.github.andock.daemon.images.models.ContainerConfig
import com.github.andock.daemon.os.Process
import com.github.andock.proot.PRoot
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
    private val appContext: Application,
    @param:Named("redirect")
    private val mapping: Map<String, String>,
) {

    val version = PRoot.getVersion()

    /**
     * Build the PRoot command for running a container
     */
    private fun buildCommand(
        container: ContainerConfig,
        rootfsDir: File,
        command: List<String>?,
    ): List<String> {
        val cmd = mutableListOf<String>()
        cmd.add(appContext.binary.absolutePath)

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
            val bindStr = "${hostPath}:${bind.containerPath}"
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
        val env = HashMap<String, String>(appContext.environment)
        // Default environment
        env["HOME"] = "/root"
        env["USER"] = container.user.ifEmpty { "root" }
        env["HOSTNAME"] = container.hostname
        // Container-specific environment
        env.putAll(container.env)
        return env
    }

    private fun startProcess(
        container: ContainerConfig,
        rootfsDir: File,
        command: List<String>? = null
    ): Result<Process> = runCatching {
        val command = buildCommand(container, rootfsDir, command)
        val env = buildEnvironment(container)
        Timber.d("=== RUN IN CONTAINER ===")
        Timber.d("Command to execute: $command")
        Timber.d("PRoot command size: ${command.size}")
        command.forEachIndexed { index, arg ->
            Timber.d("  [$index] = '$arg'")
        }
        Timber.d("========================")
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
    ): Result<Process> {
        val rootfsDir = File(appContext.containersDir, containerId)
        if (!rootfsDir.exists()) {
            return Result.failure(IllegalStateException("Container rootfs not found"))
        }
        return startProcess(
            config,
            rootfsDir,
            command
        )
    }


    companion object {
        /**
         * Build the execution command from container config
         */
        private fun buildExecCommand(config: ContainerConfig): List<String> {
            val cmd = mutableListOf<String>()
            cmd.addAll(config.entrypoint ?: emptyList())
            cmd.addAll(config.cmd)
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
                    cmd.add("$path:$path")
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