package com.shibaprasadsahu.persistid.internal.storage

import com.shibaprasadsahu.persistid.ExperimentalPersistIdApi
import com.shibaprasadsahu.persistid.PersistIdConfig
import com.shibaprasadsahu.persistid.internal.utils.Logger
import kotlinx.coroutines.flow.Flow

/**
 * Internal storage provider interface - fully async
 */
internal interface StorageProvider {
    suspend fun getIdentifier(): String?
    suspend fun saveIdentifier(identifier: String)
    suspend fun hasIdentifier(): Boolean
    suspend fun clearIdentifier()
    fun observeIdentifier(): Flow<String?>

    // Platform-specific methods
    suspend fun saveBackupTimestamp(timestamp: Long)
    suspend fun getLastBackupTimestamp(): Long?
}

/**
 * Platform-specific storage factory
 */
@OptIn(ExperimentalPersistIdApi::class)
internal expect fun createStorage(
    platformContext: Any,
    config: PersistIdConfig,
    logger: Logger
): StorageProvider
