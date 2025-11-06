package com.shibaprasadsahu.persistid.internal.backup

import com.shibaprasadsahu.persistid.internal.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSUbiquitousKeyValueStore

/**
 * iOS backup using iCloud Key-Value Store
 * Similar to Google BlockStore but for iOS
 * Syncs up to 1MB across user's devices via iCloud
 */
internal class ICloudBackup(
    private val logger: Logger
) : BackupManager {

    private val kvStore = NSUbiquitousKeyValueStore.defaultStore

    override suspend fun backup(identifier: String) {
        withContext(Dispatchers.IO) {
            try {
                kvStore.setString(identifier, forKey = KEY_IDENTIFIER)
                val success = kvStore.synchronize()

                if (success) {
                    logger.info("Backed up to iCloud KeyValue Store")
                } else {
                    logger.warn("iCloud backup synchronize returned false")
                }
            } catch (e: Exception) {
                logger.error("iCloud backup failed", e)
                // Non-fatal, continue without backup
            }
        }
    }

    override suspend fun restore(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val identifier = kvStore.stringForKey(KEY_IDENTIFIER)

                if (identifier != null && identifier.isNotBlank()) {
                    logger.info("Restored from iCloud KeyValue Store")
                    identifier
                } else {
                    logger.verbose("No data in iCloud KeyValue Store")
                    null
                }
            } catch (e: Exception) {
                logger.error("iCloud restore failed", e)
                null
            }
        }
    }

    override suspend fun clearBackup() {
        withContext(Dispatchers.IO) {
            try {
                kvStore.removeObjectForKey(KEY_IDENTIFIER)
                kvStore.synchronize()
                logger.debug("Cleared iCloud KeyValue Store")
            } catch (e: Exception) {
                logger.error("iCloud clear failed", e)
                // Non-fatal
            }
        }
    }

    private companion object {
        private const val KEY_IDENTIFIER = "persistid_identifier"
    }
}
