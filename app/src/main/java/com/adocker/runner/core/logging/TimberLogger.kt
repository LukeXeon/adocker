package com.adocker.runner.core.logging

import org.slf4j.Marker
import org.slf4j.event.Level
import org.slf4j.helpers.LegacyAbstractLogger
import org.slf4j.helpers.MessageFormatter
import timber.log.Timber

/**
 * SLF4J Logger implementation that forwards to Timber
 */
class TimberLogger(private val tag: String) : LegacyAbstractLogger() {

    companion object {
        private const val MAX_TAG_LENGTH = 23
    }

    override fun getName(): String = tag

    // Let Timber decide whether to log based on planted trees
    // Always return true for SLF4J compatibility
    override fun isTraceEnabled(): Boolean = Timber.treeCount > 0
    override fun isDebugEnabled(): Boolean = Timber.treeCount > 0
    override fun isInfoEnabled(): Boolean = Timber.treeCount > 0
    override fun isWarnEnabled(): Boolean = Timber.treeCount > 0
    override fun isErrorEnabled(): Boolean = Timber.treeCount > 0

    override fun isTraceEnabled(marker: Marker?): Boolean = isTraceEnabled()
    override fun isDebugEnabled(marker: Marker?): Boolean = isDebugEnabled()
    override fun isInfoEnabled(marker: Marker?): Boolean = isInfoEnabled()
    override fun isWarnEnabled(marker: Marker?): Boolean = isWarnEnabled()
    override fun isErrorEnabled(marker: Marker?): Boolean = isErrorEnabled()

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

        val finalMessage = message

        // Use Timber's logging methods with throwable support
        when (level) {
            Level.TRACE -> if (throwable != null) Timber.tag(tag).v(throwable) else Timber.tag(tag).v(finalMessage)
            Level.DEBUG -> if (throwable != null) Timber.tag(tag).d(throwable) else Timber.tag(tag).d(finalMessage)
            Level.INFO -> if (throwable != null) Timber.tag(tag).i(throwable) else Timber.tag(tag).i(finalMessage)
            Level.WARN -> if (throwable != null) Timber.tag(tag).w(throwable) else Timber.tag(tag).w(finalMessage)
            Level.ERROR -> if (throwable != null) Timber.tag(tag).e(throwable) else Timber.tag(tag).e(finalMessage)
        }
    }

    private fun sanitizeTag(tag: String): String {
        return if (tag.length > MAX_TAG_LENGTH) {
            tag.substring(0, MAX_TAG_LENGTH)
        } else {
            tag
        }
    }
}
