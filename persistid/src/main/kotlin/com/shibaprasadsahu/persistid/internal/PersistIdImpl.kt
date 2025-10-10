package com.shibaprasadsahu.persistid.internal

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.shibaprasadsahu.persistid.ExperimentalPersistIdApi
import com.shibaprasadsahu.persistid.PersistId
import com.shibaprasadsahu.persistid.PersistIdCallback
import com.shibaprasadsahu.persistid.PersistIdConfig
import com.shibaprasadsahu.persistid.internal.backup.BackupManager
import com.shibaprasadsahu.persistid.internal.generator.IdentifierGenerator
import com.shibaprasadsahu.persistid.internal.repository.IdentifierRepositoryImpl
import com.shibaprasadsahu.persistid.internal.storage.StorageProvider
import com.shibaprasadsahu.persistid.internal.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Internal implementation of PersistId - fully async with lifecycle support
 * Users CANNOT access this class
 */
@OptIn(ExperimentalPersistIdApi::class)
internal class PersistIdImpl private constructor(
    private val repository: IdentifierRepositoryImpl
) : PersistId {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    // Lifecycle-aware callbacks with their observers
    private val lifecycleCallbacks = mutableMapOf<PersistIdCallback, LifecycleCallbackWrapper>()

    @Volatile
    private var cachedIdentifier: String? = null

    init {
        // Preload identifier in background for faster first access
        scope.launch {
            try {
                val id = repository.getIdentifier()
                cachedIdentifier = id

                // Notify all lifecycle-aware callbacks on main thread
                mainHandler.post {
                    notifyCallbacks { it.onReady(id) }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    notifyCallbacks { it.onError(e) }
                }
            }
        }
    }

    override suspend fun getIdentifier(): String {
        return repository.getIdentifier()
    }

    override fun getIdentifierFlow(): Flow<String> {
        return repository.getIdentifierFlow()
    }

    override suspend fun hasIdentifier(): Boolean {
        return repository.hasIdentifier()
    }

    override suspend fun clearIdentifier() {
        repository.clearIdentifier()
        cachedIdentifier = null
    }

    override suspend fun regenerate(): String {
        val newId = repository.regenerate()
        cachedIdentifier = newId

        // Notify all lifecycle-aware callbacks on main thread
        mainHandler.post {
            notifyCallbacks { it.onReady(newId) }
        }

        return newId
    }

    override fun observe(lifecycleOwner: LifecycleOwner, callback: PersistIdCallback) {
        observe(lifecycleOwner, Lifecycle.State.STARTED, callback)
    }

    override fun observe(
        lifecycleOwner: LifecycleOwner,
        minState: Lifecycle.State,
        callback: PersistIdCallback
    ) {
        synchronized(lifecycleCallbacks) {
            // Remove existing if already registered
            lifecycleCallbacks[callback]?.let { wrapper ->
                wrapper.lifecycleOwner.lifecycle.removeObserver(wrapper.observer)
            }

            val wrapper = LifecycleCallbackWrapper(
                lifecycleOwner = lifecycleOwner,
                minState = minState,
                onDestroy = {
                    synchronized(lifecycleCallbacks) {
                        lifecycleCallbacks.remove(callback)
                    }
                },
                onStart = {
                    // Fire callback every time onStart is called
                    cachedIdentifier?.let { id ->
                        mainHandler.post {
                            callback.onReady(id)
                        }
                    }
                }
            )

            lifecycleCallbacks[callback] = wrapper
            lifecycleOwner.lifecycle.addObserver(wrapper.observer)

            // If already have identifier and lifecycle is active, invoke immediately
            cachedIdentifier?.let { id ->
                if (lifecycleOwner.lifecycle.currentState.isAtLeast(minState)) {
                    mainHandler.post {
                        callback.onReady(id)
                    }
                }
            }
        }
    }

    private fun notifyCallbacks(action: (PersistIdCallback) -> Unit) {
        synchronized(lifecycleCallbacks) {
            lifecycleCallbacks.forEach { (callback, wrapper) ->
                if (wrapper.lifecycleOwner.lifecycle.currentState.isAtLeast(wrapper.minState)) {
                    action(callback)
                }
            }
        }
    }

    /**
     * Wrapper for lifecycle-aware callbacks
     */
    private class LifecycleCallbackWrapper(
        val lifecycleOwner: LifecycleOwner,
        val minState: Lifecycle.State,
        val onDestroy: () -> Unit,
        val onStart: () -> Unit
    ) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                // Fire callback every time lifecycle enters STARTED state
                if (owner.lifecycle.currentState.isAtLeast(minState)) {
                    onStart()
                }
            }

            override fun onDestroy(owner: LifecycleOwner) {
                owner.lifecycle.removeObserver(this)
                onDestroy()
            }
        }
    }

    /**
     * Force backup to all layers (used by BackupSyncWorker)
     */
    internal suspend fun forceBackup() {
        repository.forceBackup()
    }

    internal companion object {
        @Volatile
        private var instance: PersistIdImpl? = null

        /**
         * Initialize synchronously - creates instance immediately
         * Then loads ID in background from BlockStore/Platform
         */
        @SuppressLint("HardwareIds")
        fun initialize(context: Context, config: PersistIdConfig) {
            if (instance != null) return

            synchronized(this) {
                if (instance != null) return


                val logger = Logger(config.logLevel)
                val storage = StorageProvider.create(
                    context.applicationContext,
                    config,
                    logger
                )
                val backupManager = BackupManager.create(
                    context.applicationContext,
                    config,
                    logger
                )

                // Get ANDROID_ID synchronously
                val androidId = try {
                    android.provider.Settings.Secure.getString(
                        context.contentResolver,
                        android.provider.Settings.Secure.ANDROID_ID
                    )
                } catch (e: Exception) {
                    logger.error("Failed to get ANDROID_ID", e)
                    null
                }

                val generator = IdentifierGenerator(
                    androidId,
                    logger
                )

                val repository = IdentifierRepositoryImpl(
                    storage = storage,
                    backupManager = backupManager,
                    generator = generator,
                    logger = logger
                )

                // Create instance immediately
                instance = PersistIdImpl(repository)

                // Schedule background sync if enabled
                if (config.enableBackgroundSync) {
                    scheduleBackgroundSync(context.applicationContext, config)
                }
            }
        }

        private fun scheduleBackgroundSync(context: Context, config: PersistIdConfig) {
            try {
                val logger = Logger(config.logLevel)
                val scheduler = com.shibaprasadsahu.persistid.internal.sync.BackupScheduler(context, logger)

                // Immediate backup on first launch with retry
                scheduler.scheduleImmediateBackup()

                // Periodic 24-hour backup
                scheduler.schedulePeriodicBackup()
            } catch (e: Exception) {
                // Non-fatal, continue without background sync
            }
        }

        /**
         * Get instance - suspends until initialization completes
         */
        /**
         * Get instance - returns immediately (non-blocking)
         * Throws exception if not initialized
         */
        fun getInstance(): PersistIdImpl {
            return instance ?: throw IllegalStateException(
                "PersistId not initialized. Call PersistId.initialize() in Application.onCreate()"
            )
        }

        /**
         * Check if initialized
         */
        fun isInitialized(): Boolean = instance != null
    }
}