package com.adocker.runner.core.logging

import android.util.Log
import org.slf4j.Logger
import org.slf4j.Marker
import org.slf4j.event.Level
import org.slf4j.helpers.LegacyAbstractLogger
import org.slf4j.helpers.MessageFormatter

/**
 * SLF4J Logger implementation that forwards to Android Log
 */
class AndroidLogger(private val tag: String) : LegacyAbstractLogger() {

    companion object {
        private const val MAX_TAG_LENGTH = 23
    }

    override fun getName(): String = tag

    override fun isTraceEnabled(): Boolean = Log.isLoggable(tag, Log.VERBOSE)
    override fun isDebugEnabled(): Boolean = Log.isLoggable(tag, Log.DEBUG)
    override fun isInfoEnabled(): Boolean = Log.isLoggable(tag, Log.INFO)
    override fun isWarnEnabled(): Boolean = Log.isLoggable(tag, Log.WARN)
    override fun isErrorEnabled(): Boolean = Log.isLoggable(tag, Log.ERROR)

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

        val finalMessage = if (throwable != null) {
            "$message\n${Log.getStackTraceString(throwable)}"
        } else {
            message
        }

        when (level) {
            Level.TRACE -> Log.v(tag, finalMessage)
            Level.DEBUG -> Log.d(tag, finalMessage)
            Level.INFO -> Log.i(tag, finalMessage)
            Level.WARN -> Log.w(tag, finalMessage)
            Level.ERROR -> Log.e(tag, finalMessage)
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
