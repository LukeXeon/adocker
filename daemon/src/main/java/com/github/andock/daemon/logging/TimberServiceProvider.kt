package com.github.andock.daemon.logging

import com.google.auto.service.AutoService
import org.slf4j.ILoggerFactory
import org.slf4j.IMarkerFactory
import org.slf4j.Logger
import org.slf4j.helpers.BasicMarkerFactory
import org.slf4j.helpers.NOPMDCAdapter
import org.slf4j.spi.MDCAdapter
import org.slf4j.spi.SLF4JServiceProvider
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function

/**
 * SLF4J 2.0 ServiceProvider implementation for Android
 *
 * This provider bridges SLF4J logging to Android's Log system.
 * It's automatically discovered via Java's ServiceLoader mechanism.
 * The @AutoService annotation generates the required META-INF/services file.
 */
@Suppress("unused")
@AutoService(SLF4JServiceProvider::class)
class TimberServiceProvider : BasicMarkerFactory(),
    SLF4JServiceProvider,
    ILoggerFactory,
    MDCAdapter by NOPMDCAdapter(),
    Function<String, Logger> {
    private val loggers = ConcurrentHashMap<String, Logger>()

    override fun getLogger(name: String): Logger {
        return loggers.computeIfAbsent(name, this)
    }

    override fun apply(loggerName: String): Logger {
        return TimberLogger(loggerName)
    }

    override fun getLoggerFactory(): ILoggerFactory = this

    override fun getMarkerFactory(): IMarkerFactory = this

    override fun getMDCAdapter(): MDCAdapter = this

    /**
     * Declare the version of the SLF4J API this implementation is compiled against.
     * The value of this field is modified with each major release.
     */
    override fun getRequestedApiVersion(): String = "2.0.99"

    override fun initialize() {
    }


}
