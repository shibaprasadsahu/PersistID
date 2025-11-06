package com.shibaprasadsahu.persistid.internal.sync

import com.shibaprasadsahu.persistid.internal.utils.Logger

/**
 * iOS implementation of backup scheduler - No-op
 * iOS doesn't need background sync as:
 * - Keychain persists data automatically
 * - iCloud KeyValue Store syncs automatically when available
 * - No explicit scheduling needed
 */
internal actual class BackupScheduler actual constructor(
    platformContext: Any,
    private val logger: Logger
) {

    /**
     * No-op on iOS - Not needed
     */
    actual fun scheduleImmediateBackup() {
        logger.verbose("iOS: Background sync not needed (Keychain + iCloud handle persistence)")
    }

    /**
     * No-op on iOS - Not needed
     */
    actual fun schedulePeriodicBackup() {
        logger.verbose("iOS: Background sync not needed (Keychain + iCloud handle persistence)")
    }
}
