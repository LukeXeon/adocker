package com.github.andock.daemon.os

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import timber.log.Timber
import java.io.File
import java.lang.reflect.Field
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicInteger


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
private val pidGen = AtomicInteger(1)
private val pidMap = WeakHashMap<Process, Int>()
private val pidField = HashMap<Class<*>, Result<Field>>()

val Process.id: Int
    get() {
        return synchronized(pidMap) {
            pidMap.getOrPut(this) {
                pidField.getOrPut(javaClass) {
                    javaClass.runCatching {
                        getDeclaredField("pid").apply {
                            isAccessible = true
                        }
                    }
                }.mapCatching { field ->
                    field.getInt(this@id)
                }.fold(
                    {
                        it
                    },
                    {
                        Timber.e(it)
                        val pid = pidRegex.find(
                            this@id.toString()
                        )?.groups?.get(1)?.value?.toIntOrNull()
                        pid ?: pidGen.getAndIncrement()
                    }
                )
            }
        }
    }

suspend inline fun Process.await(): Int {
    return runInterruptible(Dispatchers.IO) {
        waitFor()
    }
}