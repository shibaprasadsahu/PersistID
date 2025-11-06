package com.shibaprasadsahu.persistid.internal.sync

import com.shibaprasadsahu.persistid.internal.utils.Logger

/**
 * Platform-specific backup scheduler
 * - Android: Uses WorkManager for periodic 24h sync
 * - iOS: No-op (background sync not needed as Keychain + iCloud handle persistence)
 */
internal expect class BackupScheduler(platformContext: Any, logger: Logger) {
    /**
     * Schedule immediate backup with retry
     */
    fun scheduleImmediateBackup()

    /**
     * Schedule periodic 24-hour backup
     */
    fun schedulePeriodicBackup()
}
