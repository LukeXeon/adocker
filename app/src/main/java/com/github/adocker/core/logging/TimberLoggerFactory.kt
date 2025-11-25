package com.github.adocker.core.logging

import org.slf4j.ILoggerFactory
import org.slf4j.Logger
import java.util.concurrent.ConcurrentHashMap

/**
 * SLF4J LoggerFactory implementation for Android
 */
class TimberLoggerFactory : ILoggerFactory {
    private val loggers = ConcurrentHashMap<String, Logger>()

    override fun getLogger(name: String): Logger {
        return loggers.computeIfAbsent(name) { loggerName ->
            TimberLogger(loggerName)
        }
    }
}
