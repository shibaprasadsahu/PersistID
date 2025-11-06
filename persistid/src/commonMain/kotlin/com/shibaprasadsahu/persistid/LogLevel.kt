package com.shibaprasadsahu.persistid

/**
 * Log level for PersistId logging
 *
 * **⚠️ EXPERIMENTAL**: This API is experimental and subject to change.
 *
 * @since 0.1.0-alpha01
 */
enum class LogLevel {
    /**
     * Verbose logging - all details
     */
    VERBOSE,

    /**
     * Debug logging - detailed information for debugging
     */
    DEBUG,

    /**
     * Info logging - general information
     */
    INFO,

    /**
     * Warning logging - potential issues
     */
    WARN,

    /**
     * Error logging - errors only
     */
    ERROR,

    /**
     * No logging
     */
    NONE
}
