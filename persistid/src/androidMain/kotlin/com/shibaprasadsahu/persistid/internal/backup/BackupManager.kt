package com.shibaprasadsahu.persistid.internal.backup

import android.content.Context
import com.shibaprasadsahu.persistid.BackupStrategy
import com.shibaprasadsahu.persistid.ExperimentalPersistIdApi
import com.shibaprasadsahu.persistid.PersistIdConfig
import com.shibaprasadsahu.persistid.internal.utils.Logger

/**
 * Android implementation of backup manager factory
 */
@OptIn(ExperimentalPersistIdApi::class)
internal actual fun createBackupManager(
    platformContext: Any,
    config: PersistIdConfig,
    logger: Logger
): BackupManager {
    require(platformContext is Context) { "Platform context must be Android Context" }

    return when (config.backupStrategy) {
        BackupStrategy.BLOCK_STORE -> BlockStoreBackup(platformContext, logger)
        BackupStrategy.NONE -> NoOpBackup()
    }
}
