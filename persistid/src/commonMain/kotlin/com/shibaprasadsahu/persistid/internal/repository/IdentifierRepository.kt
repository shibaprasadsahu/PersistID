package com.shibaprasadsahu.persistid.internal.repository

import kotlinx.coroutines.flow.Flow

/**
 * Internal repository interface - fully async
 */
internal interface IdentifierRepository {
    suspend fun getIdentifier(): String
    fun getIdentifierFlow(): Flow<String>
    suspend fun hasIdentifier(): Boolean
    suspend fun clearIdentifier()
    suspend fun regenerate(): String
    suspend fun forceBackup()
}