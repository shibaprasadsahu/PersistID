package com.shibaprasadsahu.persistid.internal.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.shibaprasadsahu.persistid.internal.utils.Logger
import java.util.concurrent.TimeUnit

/**
 * Android implementation of backup scheduler using WorkManager
 * Handles both immediate first-time backup and periodic updates
 */
internal actual class BackupScheduler actual constructor(
    platformContext: Any,
    private val logger: Logger
) {
    private val context: Context = platformContext as Context

    private val workManager by lazy {
        WorkManager.getInstance(context)
    }

    /**
     * Schedule immediate backup on first app launch with retry
     */
    actual fun scheduleImmediateBackup() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // BlockStore needs network
            .build()

        val immediateWork = OneTimeWorkRequestBuilder<BackupSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                INITIAL_BACKOFF_SECONDS,
                TimeUnit.SECONDS
            )
            .addTag(TAG_IMMEDIATE)
            .build()

        // Use KEEP to avoid duplicate immediate backups
        workManager.enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            immediateWork
        )

        logger.verbose("Scheduled immediate backup")
    }

    /**
     * Schedule periodic 24-hour backup sync
     */
    actual fun schedulePeriodicBackup() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true) // Don't drain battery
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<BackupSyncWorker>(
            PERIODIC_INTERVAL_HOURS,
            TimeUnit.HOURS,
            FLEX_INTERVAL_HOURS,
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                PERIODIC_BACKOFF_SECONDS,
                TimeUnit.SECONDS
            )
            .addTag(TAG_PERIODIC)
            .build()

        // KEEP existing work to preserve the schedule
        workManager.enqueueUniquePeriodicWork(
            BackupSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWork
        )

        logger.verbose("Scheduled periodic backup (24h)")
    }

    /**
     * Cancel all scheduled backups
     */
    fun cancelAllBackups() {
        workManager.cancelUniqueWork(IMMEDIATE_WORK_NAME)
        workManager.cancelUniqueWork(BackupSyncWorker.WORK_NAME)
        logger.debug("Cancelled scheduled backups")
    }

    private companion object {
        const val IMMEDIATE_WORK_NAME = "persistid_immediate_backup"
        const val TAG_IMMEDIATE = "persistid_immediate"
        const val TAG_PERIODIC = "persistid_periodic"

        // Immediate backup retry
        const val INITIAL_BACKOFF_SECONDS = 30L

        // Periodic backup settings
        const val PERIODIC_INTERVAL_HOURS = 24L
        const val FLEX_INTERVAL_HOURS = 6L // Can run within 6-hour window
        const val PERIODIC_BACKOFF_SECONDS = 60L
    }
}
