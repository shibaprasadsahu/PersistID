package com.shibaprasadsahu.persistid.internal.utils

import android.util.Log
import com.shibaprasadsahu.persistid.LogLevel

/**
 * Internal logger with log level support
 */
internal class Logger(
    private val minLevel: LogLevel
) {

    fun verbose(message: String) {
        if (shouldLog(LogLevel.VERBOSE)) {
            Log.v(TAG, message)
        }
    }

    fun debug(message: String) {
        if (shouldLog(LogLevel.DEBUG)) {
            Log.d(TAG, message)
        }
    }

    fun info(message: String) {
        if (shouldLog(LogLevel.INFO)) {
            Log.i(TAG, message)
        }
    }

    fun warn(message: String, throwable: Throwable? = null) {
        if (shouldLog(LogLevel.WARN)) {
            Log.w(TAG, message, throwable)
        }
    }

    fun error(message: String, throwable: Throwable? = null) {
        if (shouldLog(LogLevel.ERROR)) {
            Log.e(TAG, message, throwable)
        }
    }

    private fun shouldLog(level: LogLevel): Boolean {
        if (minLevel == LogLevel.NONE) return false
        return level.ordinal >= minLevel.ordinal
    }

    private companion object {
        private const val TAG = "PersistId"
    }
}