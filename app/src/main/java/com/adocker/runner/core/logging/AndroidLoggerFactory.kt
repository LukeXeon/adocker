package com.adocker.runner.core.logging

import org.slf4j.ILoggerFactory
import org.slf4j.Logger
import java.util.concurrent.ConcurrentHashMap

/**
 * SLF4J LoggerFactory implementation for Android
 */
class AndroidLoggerFactory : ILoggerFactory {

    private val loggers = ConcurrentHashMap<String, Logger>()

    override fun getLogger(name: String): Logger {
        return loggers.computeIfAbsent(name) { loggerName ->
            // Simplify class names to fit Android's tag length limit
            val tag = simplifyName(loggerName)
            AndroidLogger(tag)
        }
    }

    private fun simplifyName(name: String): String {
        // For class names like "com.adocker.runner.SomeClass", use "SomeClass"
        // If still too long, use first 23 chars
        val simpleName = name.substringAfterLast('.')
        return if (simpleName.length <= 23) {
            simpleName
        } else {
            simpleName.substring(0, 23)
        }
    }
}
