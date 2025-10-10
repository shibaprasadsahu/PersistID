package com.shibaprasadsahu.persistid.internal.backup

import android.content.Context
import com.shibaprasadsahu.persistid.BackupStrategy
import com.shibaprasadsahu.persistid.ExperimentalPersistIdApi
import com.shibaprasadsahu.persistid.PersistIdConfig
import com.shibaprasadsahu.persistid.internal.utils.Logger

/**
 * Internal backup manager using Strategy pattern - fully async
 */
@OptIn(ExperimentalPersistIdApi::class)
internal interface BackupManager {
    suspend fun backup(identifier: String)
    suspend fun restore(): String?
    suspend fun clearBackup()

    companion object {
        fun create(
            context: Context,
            config: PersistIdConfig,
            logger: Logger
        ): BackupManager {
            return when (config.backupStrategy) {
                BackupStrategy.BLOCK_STORE -> BlockStoreBackup(context, logger)
                BackupStrategy.NONE -> NoOpBackup()
            }
        }
    }
}