package com.shibaprasadsahu.persistid.internal.storage

import com.liftric.kvault.KVault
import com.shibaprasadsahu.persistid.internal.utils.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS storage using Keychain via KVault
 * Provides persistent, secure storage that survives app uninstall
 */
internal class KeychainStorage(
    private val logger: Logger
) : StorageProvider {

    private val vault = KVault()
    private val identifierFlow = MutableStateFlow<String?>(null)

    init {
        // Load initial value
        identifierFlow.value = vault.string(KEY_IDENTIFIER)
    }

    override suspend fun getIdentifier(): String? {
        return try {
            vault.string(KEY_IDENTIFIER)?.also {
                logger.verbose("Retrieved from Keychain")
            }
        } catch (e: Exception) {
            logger.error("Keychain read failed", e)
            null
        }
    }

    override suspend fun saveIdentifier(identifier: String) {
        try {
            vault.set(KEY_IDENTIFIER, identifier)
            identifierFlow.value = identifier
            logger.verbose("Saved to Keychain")
        } catch (e: Exception) {
            logger.error("Keychain save failed", e)
            throw e
        }
    }

    override suspend fun hasIdentifier(): Boolean {
        return try {
            vault.existsObject(KEY_IDENTIFIER)
        } catch (e: Exception) {
            logger.error("Keychain check failed", e)
            false
        }
    }

    override suspend fun clearIdentifier() {
        try {
            vault.deleteObject(KEY_IDENTIFIER)
            identifierFlow.value = null
            logger.verbose("Cleared Keychain")
        } catch (e: Exception) {
            logger.error("Keychain clear failed", e)
            throw e
        }
    }

    override fun observeIdentifier(): Flow<String?> {
        return identifierFlow.asStateFlow()
    }

    override suspend fun saveBackupTimestamp(timestamp: Long) {
        try {
            vault.set(KEY_LAST_BACKUP, timestamp)
            logger.verbose("Saved backup timestamp")
        } catch (e: Exception) {
            logger.error("Failed to save backup timestamp", e)
        }
    }

    override suspend fun getLastBackupTimestamp(): Long? {
        return try {
            vault.long(KEY_LAST_BACKUP)
        } catch (e: Exception) {
            logger.error("Failed to get backup timestamp", e)
            null
        }
    }

    private companion object {
        private const val KEY_IDENTIFIER = "persistid_identifier"
        private const val KEY_LAST_BACKUP = "last_backup_timestamp"
    }
}
