package com.shibaprasadsahu.persistid.internal.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shibaprasadsahu.persistid.ExperimentalPersistIdApi
import com.shibaprasadsahu.persistid.LogLevel
import com.shibaprasadsahu.persistid.PersistId
import com.shibaprasadsahu.persistid.internal.PersistIdImpl
import com.shibaprasadsahu.persistid.internal.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background worker for periodic backup synchronization
 * Runs every 24 hours to ensure backups are up-to-date
 */
@OptIn(ExperimentalPersistIdApi::class)
internal class BackupSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val logger = Logger(LogLevel.INFO)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Check if PersistId is initialized
            if (!PersistId.isInitialized()) {
                logger.verbose("PersistId not initialized, skipping backup sync")
                return@withContext Result.failure()
            }

            val persistId = PersistId.getInstance()

            // Get current identifier (this will load from cache/storage)
            val identifier = persistId.getIdentifier()

            // Force re-backup to all layers
            // This ensures cloud backups stay fresh
            val impl = persistId as? PersistIdImpl
            impl?.forceBackup()

            logger.verbose("Background backup sync completed")
            Result.success()
        } catch (e: Exception) {
            logger.error("Background backup sync failed", e)
            // Retry with exponential backoff
            if (runAttemptCount < MAX_RETRIES) {
                logger.debug("Retrying backup sync (attempt ${runAttemptCount + 1}/$MAX_RETRIES)")
                Result.retry()
            } else {
                logger.warn("Max retries reached, backup sync failed")
                // Non-fatal, just log and continue
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "persistid_backup_sync"
        const val MAX_RETRIES = 3
    }
}
