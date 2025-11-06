package com.shibaprasadsahu.persistid.internal.backup

import com.shibaprasadsahu.persistid.BackupStrategy
import com.shibaprasadsahu.persistid.ExperimentalPersistIdApi
import com.shibaprasadsahu.persistid.PersistIdConfig
import com.shibaprasadsahu.persistid.internal.utils.Logger

/**
 * Internal backup manager interface - fully async
 */
internal interface BackupManager {
    suspend fun backup(identifier: String)
    suspend fun restore(): String?
    suspend fun clearBackup()
}

/**
 * Platform-specific backup manager factory
 */
@OptIn(ExperimentalPersistIdApi::class)
internal expect fun createBackupManager(
    platformContext: Any,
    config: PersistIdConfig,
    logger: Logger
): BackupManager
