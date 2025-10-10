package com.shibaprasadsahu.persistid.internal.backup

/**
 * No-op backup provider
 */
internal class NoOpBackup(
) : BackupManager {

    override suspend fun backup(identifier: String) {
        // logger.log("Backup disabled")
    }

    override suspend fun restore(): String? {
        return null
    }

    override suspend fun clearBackup() {
        // logger.log("Backup disabled")
    }
}