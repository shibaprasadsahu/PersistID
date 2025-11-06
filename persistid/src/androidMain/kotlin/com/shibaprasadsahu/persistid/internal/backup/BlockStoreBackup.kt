package com.shibaprasadsahu.persistid.internal.backup

import android.content.Context
import android.os.Build
import com.google.android.gms.auth.blockstore.Blockstore
import com.google.android.gms.auth.blockstore.DeleteBytesRequest
import com.google.android.gms.auth.blockstore.RetrieveBytesRequest
import com.google.android.gms.auth.blockstore.StoreBytesData
import com.shibaprasadsahu.persistid.internal.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Internal BlockStore backup provider - fully async with coroutines
 * Uses modern Kotlin idioms and proper error handling
 */
internal class BlockStoreBackup(
    context: Context,
    private val logger: Logger
) : BackupManager {

    private val client by lazy {
        Blockstore.getClient(context)
    }

    override suspend fun backup(identifier: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            logger.verbose("BlockStore not supported on API < 28")
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val data = StoreBytesData.Builder()
                    .setBytes(identifier.toByteArray(Charsets.UTF_8))
                    .setKey(KEY_IDENTIFIER)
                    .setShouldBackupToCloud(true) // Enable cloud backup when user has Google backup enabled
                    .build()

                withTimeoutOrNull(TIMEOUT_MS) {
                    client.storeBytes(data).await()
                }
                logger.info("Backed up to BlockStore (local + cloud if enabled)")
            } catch (e: Exception) {
                logger.error("BlockStore backup failed", e)
                // Non-fatal, continue without backup
            }
        }
    }

    override suspend fun restore(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            logger.verbose("BlockStore not supported on API < 28")
            return null
        }

        return withContext(Dispatchers.IO) {
            try {
                val request = RetrieveBytesRequest.Builder()
                    .setKeys(listOf(KEY_IDENTIFIER))
                    .build()

                val response = withTimeoutOrNull(TIMEOUT_MS) {
                    client.retrieveBytes(request).await()
                }

                // Extract bytes from blockstore data map
                response?.blockstoreDataMap?.get(KEY_IDENTIFIER)?.bytes?.let { bytes ->
                    String(bytes, Charsets.UTF_8).also {
                        logger.info("Restored from BlockStore")
                    }
                } ?: run {
                    logger.verbose("No data in BlockStore")
                    null
                }
            } catch (e: Exception) {
                logger.error("BlockStore restore failed", e)
                null
            }
        }
    }

    override suspend fun clearBackup() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val request = DeleteBytesRequest.Builder()
                    .setKeys(listOf(KEY_IDENTIFIER))
                    .build()

                withTimeoutOrNull(TIMEOUT_MS) {
                    client.deleteBytes(request).await()
                }
                logger.debug("Cleared BlockStore")
            } catch (e: Exception) {
                logger.error("BlockStore clear failed", e)
                // Non-fatal
            }
        }
    }

    private companion object {
        private const val KEY_IDENTIFIER = "persistid_identifier"
        private const val TIMEOUT_MS = 5000L
    }
}