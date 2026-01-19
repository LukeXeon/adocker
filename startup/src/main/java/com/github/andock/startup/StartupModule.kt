package com.github.andock.startup

import android.app.ActivityManager
import android.app.Application
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.core.content.getSystemService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.io.IOException
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object StartupModule {
    @Provides
    @Singleton
    fun tasks(
        tasks: Map<TaskInfo, TaskComputeTime>,
        @Named("process-name")
        processName: String,
        application: Application,
    ): Map<String, List<TaskComputeTime>> {
        val map = mutableMapOf<String, MutableList<TaskComputeTime>>()
        if (processName.startsWith(application.packageName)) {
            val processName = processName.removePrefix(application.packageName)
            tasks.forEach { (key, task) ->
                val tasks = map.getOrPut(key.trigger) { mutableListOf() }
                key.processes.forEach { process ->
                    if (process == processName) {
                        tasks.add(task)
                    }
                }
            }
        }
        return map
    }

    @Named("main-thread")
    @Provides
    @Singleton
    fun mainThread(): Handler {
        return Handler(Looper.getMainLooper())
    }

    /**
     * 获取当前进程名
     * @param context 上下文（建议用 Application Context）
     * @return 进程名（失败返回空字符串）
     */
    @Named("process-name")
    @Provides
    @Singleton
    fun processName(context: Application): String {
        // 方式1：Android 8.0+ 推荐使用（更高效）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return try {
                Application.getProcessName()
            } catch (e: Exception) {
                e.printStackTrace()
                // 降级到方式2
                getProcessNameByActivityManager(context)
            }
        }

        // 方式2：通用方式（适配所有版本）
        return getProcessNameByActivityManager(context)
    }

    /**
     * 通过 ActivityManager 获取进程名（通用方式）
     */
    private fun getProcessNameByActivityManager(context: Application): String {
        val currentPid = Process.myPid()
        val activityManager = context.getSystemService<ActivityManager>()!!
        // 遍历所有进程，匹配当前 PID
        val currentProcess = activityManager.runningAppProcesses?.find { processInfo ->
            processInfo.pid == currentPid
        }
        if (currentProcess != null) {
            currentProcess.processName ?: ""
        }
        // 兜底：读取 /proc/self/cmdline 文件（极少数情况备用）
        return getProcessNameByProcFile()
    }

    /**
     * 读取 /proc/self/cmdline 文件获取进程名（兜底方案）
     */
    private fun getProcessNameByProcFile(): String {
        return try {
            val cmdlineFile = File("/proc/self/cmdline")
            val bytes = cmdlineFile.readBytes()
            // 去除空字符（Linux 进程名以 \0 结尾）
            String(bytes).replace("\u0000", "").trim()
        } catch (e: IOException) {
            e.printStackTrace()
            ""
        }
    }
}