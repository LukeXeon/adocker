package com.github.andock.daemon.logging

import org.slf4j.Marker
import org.slf4j.event.Level
import org.slf4j.helpers.LegacyAbstractLogger
import org.slf4j.helpers.MessageFormatter
import timber.log.Timber

/**
 * SLF4J Logger implementation that forwards to Timber
 */
class TimberLogger(private val tag: String) : LegacyAbstractLogger() {

    override fun getName(): String = tag

    // Let Timber decide whether to log based on planted trees
    // Always return true for SLF4J compatibility
    override fun isTraceEnabled(): Boolean = Timber.treeCount > 0
    override fun isDebugEnabled(): Boolean = Timber.treeCount > 0
    override fun isInfoEnabled(): Boolean = Timber.treeCount > 0
    override fun isWarnEnabled(): Boolean = Timber.treeCount > 0
    override fun isErrorEnabled(): Boolean = Timber.treeCount > 0

    override fun isTraceEnabled(marker: Marker?): Boolean = Timber.treeCount > 0
    override fun isDebugEnabled(marker: Marker?): Boolean = Timber.treeCount > 0
    override fun isInfoEnabled(marker: Marker?): Boolean = Timber.treeCount > 0
    override fun isWarnEnabled(marker: Marker?): Boolean = Timber.treeCount > 0
    override fun isErrorEnabled(marker: Marker?): Boolean = Timber.treeCount > 0

    override fun getFullyQualifiedCallerName(): String? = null

    override fun handleNormalizedLoggingCall(
        level: Level,
        marker: Marker?,
        messagePattern: String?,
        arguments: Array<out Any>?,
        throwable: Throwable?
    ) {
        val message = if (messagePattern != null && arguments != null) {
            MessageFormatter.arrayFormat(messagePattern, arguments).message
        } else {
            messagePattern ?: ""
        }

        // Use Timber's logging methods with throwable support
        when (level) {
            Level.TRACE -> if (throwable != null) {
                Timber.tag(tag).v(throwable)
            } else {
                Timber.tag(tag).v(message)
            }

            Level.DEBUG -> if (throwable != null) {
                Timber.tag(tag).d(throwable)
            } else {
                Timber.tag(tag).d(message)
            }

            Level.INFO -> if (throwable != null) {
                Timber.tag(tag).i(throwable)
            } else {
                Timber.tag(tag).i(message)
            }

            Level.WARN -> if (throwable != null) {
                Timber.tag(tag).w(throwable)
            } else {
                Timber.tag(tag).w(message)
            }

            Level.ERROR -> if (throwable != null) {
                Timber.tag(tag).e(throwable)
            } else {
                Timber.tag(tag).e(message)
            }
        }
    }
}
