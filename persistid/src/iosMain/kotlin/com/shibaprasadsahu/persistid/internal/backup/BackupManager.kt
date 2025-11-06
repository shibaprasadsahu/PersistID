package com.shibaprasadsahu.persistid.internal.backup

import com.shibaprasadsahu.persistid.BackupStrategy
import com.shibaprasadsahu.persistid.ExperimentalPersistIdApi
import com.shibaprasadsahu.persistid.PersistIdConfig
import com.shibaprasadsahu.persistid.internal.utils.Logger

/**
 * iOS implementation of backup manager factory
 */
@OptIn(ExperimentalPersistIdApi::class)
internal actual fun createBackupManager(
    platformContext: Any,
    config: PersistIdConfig,
    logger: Logger
): BackupManager {
    return when (config.backupStrategy) {
        BackupStrategy.BLOCK_STORE -> ICloudBackup(logger)
        BackupStrategy.NONE -> NoOpBackup()
    }
}
