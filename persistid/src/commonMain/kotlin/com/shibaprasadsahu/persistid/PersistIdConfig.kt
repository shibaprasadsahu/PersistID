package com.shibaprasadsahu.persistid

/**
 * Configuration for PersistId SDK
 * Use Builder to create instance
 *
 * **⚠️ EXPERIMENTAL**: This API is experimental and subject to change.
 *
 * @since 0.1.0-alpha01
 */
@ExperimentalPersistIdApi
@ConsistentCopyVisibility
data class PersistIdConfig internal constructor(
    val backupStrategy: BackupStrategy,
    val logLevel: LogLevel,
    val enableBackgroundSync: Boolean
) {

    class Builder {
        private var backupStrategy: BackupStrategy = BackupStrategy.BLOCK_STORE
        private var logLevel: LogLevel = LogLevel.NONE
        private var enableBackgroundSync: Boolean = true

        /**
         * Set backup strategy
         * @param strategy BackupStrategy enum
         */
        fun setBackupStrategy(strategy: BackupStrategy) = apply {
            this.backupStrategy = strategy
        }

        /**
         * Set log level for debugging
         * @param level LogLevel enum (VERBOSE, DEBUG, INFO, WARN, ERROR, NONE)
         */
        fun setLogLevel(level: LogLevel) = apply {
            this.logLevel = level
        }

        /**
         * Enable or disable debug logging
         * @param enable true to enable INFO level logging, false for no logging
         * @deprecated Use setLogLevel() instead
         */
        @Deprecated("Use setLogLevel() instead", ReplaceWith("setLogLevel(if (enable) LogLevel.INFO else LogLevel.NONE)"))
        fun setLogging(enable: Boolean) = apply {
            this.logLevel = if (enable) LogLevel.INFO else LogLevel.NONE
        }

        /**
         * Enable or disable background backup sync with WorkManager
         * When enabled, backups sync every 24 hours automatically
         * @param enable true to enable background sync (default: true)
         */
        fun setBackgroundSync(enable: Boolean) = apply {
            this.enableBackgroundSync = enable
        }

        /**
         * Build configuration
         * @return PersistIdConfig instance
         */
        fun build(): PersistIdConfig {
            return PersistIdConfig(
                backupStrategy = backupStrategy,
                logLevel = logLevel,
                enableBackgroundSync = enableBackgroundSync
            )
        }
    }
}