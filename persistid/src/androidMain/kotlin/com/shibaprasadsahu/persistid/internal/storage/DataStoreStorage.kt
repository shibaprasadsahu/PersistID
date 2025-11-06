package com.shibaprasadsahu.persistid.internal.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shibaprasadsahu.persistid.internal.utils.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

/**
 * Modern async storage using DataStore Preferences
 * Thread-safe, async, and efficient
 */
internal class DataStoreStorage(
    private val context: Context,
    private val logger: Logger
) : StorageProvider {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        name = DATASTORE_NAME
    )

    override suspend fun getIdentifier(): String? {
        return try {
            context.dataStore.data
                .map { preferences -> preferences[KEY_IDENTIFIER] }
                .firstOrNull()
                .also {
                    if (it != null) {
                        logger.verbose("Retrieved from DataStore")
                    }
                }
        } catch (e: Exception) {
            logger.error("DataStore read failed", e)
            null
        }
    }

    override suspend fun saveIdentifier(identifier: String) {
        try {
            context.dataStore.edit { preferences ->
                preferences[KEY_IDENTIFIER] = identifier
            }
            logger.verbose("Saved to DataStore")
        } catch (e: Exception) {
            logger.error("DataStore save failed", e)
            throw e
        }
    }

    override suspend fun hasIdentifier(): Boolean {
        return try {
            context.dataStore.data
                .map { preferences -> preferences.contains(KEY_IDENTIFIER) }
                .firstOrNull() ?: false
        } catch (e: Exception) {
            logger.error("DataStore check failed", e)
            false
        }
    }

    override suspend fun clearIdentifier() {
        try {
            context.dataStore.edit { preferences ->
                preferences.remove(KEY_IDENTIFIER)
            }
            logger.verbose("Cleared DataStore")
        } catch (e: Exception) {
            logger.error("DataStore clear failed", e)
            throw e
        }
    }

    override fun observeIdentifier(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_IDENTIFIER]
        }
    }

    suspend fun saveBackupTimestamp(timestamp: Long) {
        try {
            context.dataStore.edit { preferences ->
                preferences[KEY_LAST_BACKUP] = timestamp
            }
            logger.verbose("Saved backup timestamp")
        } catch (e: Exception) {
            logger.error("Failed to save backup timestamp", e)
        }
    }

    suspend fun getLastBackupTimestamp(): Long? {
        return try {
            context.dataStore.data
                .map { preferences -> preferences[KEY_LAST_BACKUP] }
                .firstOrNull()
        } catch (e: Exception) {
            logger.error("Failed to get backup timestamp", e)
            null
        }
    }

    private companion object {
        private const val DATASTORE_NAME = "persistid_datastore"
        private val KEY_IDENTIFIER = stringPreferencesKey("device_identifier")
        private val KEY_LAST_BACKUP = longPreferencesKey("last_backup_timestamp")
    }
}
