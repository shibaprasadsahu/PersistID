package com.shibaprasadsahu.persistid.internal.repository

import com.shibaprasadsahu.persistid.internal.backup.BackupManager
import com.shibaprasadsahu.persistid.internal.generator.IdentifierGenerator
import com.shibaprasadsahu.persistid.internal.storage.DataStoreStorage
import com.shibaprasadsahu.persistid.internal.storage.StorageProvider
import com.shibaprasadsahu.persistid.internal.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Internal repository implementation - fully async with proper concurrency control
 */
internal class IdentifierRepositoryImpl internal constructor(
    private val storage: StorageProvider,
    private val backupManager: BackupManager,
    private val generator: IdentifierGenerator,
    private val logger: Logger
) : IdentifierRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    // Hot flow that caches the identifier
    private val identifierFlow: StateFlow<String?> = storage.observeIdentifier()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    override suspend fun getIdentifier(): String = withContext(Dispatchers.Default) {
        mutex.withLock {
            // Layer 1: Cache (99% hit rate, instant)
            identifierFlow.value?.let {
                logger.verbose("Retrieved from cache")
                return@withContext it
            }

            // Layer 2: DataStore (local storage)
            storage.getIdentifier()?.let {
                logger.debug("Retrieved from DataStore")
                logger.debug("ID: $it")
                return@withContext it
            }

            // Layer 3: Backup restore (BlockStore/AutoBackup)
            restoreFromBackup()?.let {
                return@withContext it
            }

            // Layer 4: Generate new platform-specific ID
            logger.info("No existing ID, generating new one")
            generateAndStoreToAllLayers()
        }
    }

    override fun getIdentifierFlow(): Flow<String> {
        return identifierFlow.filterNotNull()
    }

    override suspend fun hasIdentifier(): Boolean = withContext(Dispatchers.IO) {
        storage.hasIdentifier()
    }

    override suspend fun clearIdentifier() = withContext(Dispatchers.IO) {
        mutex.withLock {
            logger.debug("Clearing identifier")

            // Run clear operations in parallel
            val storageJob = async { storage.clearIdentifier() }
            val backupJob = async { backupManager.clearBackup() }

            storageJob.await()
            backupJob.await()
        }
    }

    override suspend fun regenerate(): String = withContext(Dispatchers.Default) {
        mutex.withLock {
            logger.debug("Regenerating identifier")

            // Clear existing ID first
            val storageJob = async(Dispatchers.IO) { storage.clearIdentifier() }
            val backupJob = async(Dispatchers.IO) { backupManager.clearBackup() }

            storageJob.await()
            backupJob.await()

            // Generate and store new ID
            generateAndStoreToAllLayers()
        }
    }

    override suspend fun forceBackup(): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val currentId = storage.getIdentifier()
            if (currentId == null) {
                logger.verbose("No ID to backup")
                return@withContext
            }

            // Check if we should backup based on last backup time
            val lastBackup =
                (storage as? DataStoreStorage)?.getLastBackupTimestamp()

            if (shouldBackup(System.currentTimeMillis(), lastBackup)) {
                val hoursSince =
                    lastBackup?.let { (System.currentTimeMillis() - it) / (1000 * 60 * 60) }
                logger.debug("Force backup: Last backup ${hoursSince?.let { "${it}h ago" } ?: "never"}")

                backupManager.backup(currentId)
                (storage as? DataStoreStorage)?.saveBackupTimestamp(
                    System.currentTimeMillis()
                )
            } else {
                logger.verbose("Force backup skipped: Recent backup exists")
            }
        }
    }

    /**
     * Check if backup is needed based on last backup timestamp
     */
    private fun shouldBackup(currentTime: Long, lastBackup: Long?): Boolean {
        // No previous backup -> BACKUP
        if (lastBackup == null) return true

        // Last backup failed (timestamp = 0) -> BACKUP
        if (lastBackup == 0L) return true

        // More than 24 hours since last backup -> BACKUP
        val hoursSinceBackup = (currentTime - lastBackup) / (1000 * 60 * 60)
        return hoursSinceBackup >= 24
    }

    /**
     * Generate new platform-specific ID and save to ALL layers simultaneously
     * This ensures maximum persistence across uninstall, factory reset, etc.
     */
    private suspend fun generateAndStoreToAllLayers(): String = withContext(Dispatchers.Default) {
        val newId = generator.generate()
        logger.info("Generated new ID (source: ${getIdSource(newId)})")
        logger.debug("ID: $newId")

        // Save to DataStore first
        storage.saveIdentifier(newId)
        logger.debug("Saved to DataStore")

        // ALWAYS backup newly generated IDs
        backupManager.backup(newId)

        // Mark backup timestamp
        (storage as? DataStoreStorage)?.saveBackupTimestamp(
            System.currentTimeMillis()
        )

        newId
    }

    /**
     * Restore from backup layers (Layer 3)
     * Saves to DataStore but DOES NOT backup again (already in backup)
     */
    private suspend fun restoreFromBackup(): String? = withContext(Dispatchers.IO) {
        try {
            val id = backupManager.restore()
            if (id != null) {
                logger.info("Restored from backup")
                logger.debug("ID: $id")

                // Save to DataStore for fast local access
                storage.saveIdentifier(id)

                // Mark as backed up (since we just got it from backup)
                // This prevents immediately re-backing up to BlockStore
                (storage as? DataStoreStorage)?.saveBackupTimestamp(
                    System.currentTimeMillis()
                )

                return@withContext id
            }
            null
        } catch (e: Exception) {
            logger.error("Backup restore failed", e)
            null
        }
    }

    /**
     * Get ID source for logging
     */
    private fun getIdSource(id: String): String {
        return when {
            android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O ->
                "Build.SERIAL"

            id.length == 16 && id.all { it.isLetterOrDigit() } ->
                "ANDROID_ID"

            else ->
                "UUID"
        }
    }

    /**
     * Pre-initialize the repository by loading identifier in background
     */
    suspend fun preload() = withContext(Dispatchers.IO) {
        try {
            getIdentifier()
            logger.verbose("Repository preloaded")
        } catch (e: Exception) {
            logger.error("Failed to preload", e)
        }
    }

}