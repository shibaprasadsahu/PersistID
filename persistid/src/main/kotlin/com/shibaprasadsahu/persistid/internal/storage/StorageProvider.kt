package com.shibaprasadsahu.persistid.internal.storage

import android.content.Context
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

    companion object {

        @OptIn(ExperimentalPersistIdApi::class)
        fun create(
            context: Context,
            config: PersistIdConfig,
            logger: Logger
        ): StorageProvider {
            // Create DataStore storage synchronously (DataStore itself is lazy-initialized)
            return DataStoreStorage(context, logger)
        }
    }
}