package com.github.andock.daemon.os

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.reflect.Field


/**
 * Start process
 */
fun Process(
    command: List<String>,
    workingDir: File? = null,
    environment: Map<String, String> = emptyMap(),
    redirectErrorStream: Boolean = true
): Process {
    val processBuilder = ProcessBuilder(command).apply {
        workingDir?.let { directory(it) }
        environment().putAll(environment)
        redirectErrorStream(redirectErrorStream)
    }
    return processBuilder.start()
}

private val pidRegex = Regex("pid=(\\d+)")

private val pidField = arrayOfNulls<Any?>(1)

val Process.pid: Int
    get() {
        val field = synchronized<Field?>(pidField) {
            var field = pidField[0]
            if (field is Unit) {
                return@synchronized null
            } else {
                field = javaClass.runCatching {
                    getDeclaredField("pid").apply {
                        isAccessible = true
                    }
                }.getOrNull()
                if (field == null) {
                    pidField[0] = Unit
                    return@synchronized field
                }
                return@synchronized null
            }
        }
        var pid = field?.runCatching {
            getInt(this@pid)
        }?.getOrNull()
        if (pid != null) {
            return pid
        }
        pid = pidRegex.find(
            this.toString()
        )?.groups?.get(1)?.value?.toIntOrNull()
        if (pid != null) {
            return pid
        }
        return 0
    }

suspend fun Process.await(): Int {
    return withContext(Dispatchers.IO) {
        runInterruptible {
            waitFor()
        }
    }
}