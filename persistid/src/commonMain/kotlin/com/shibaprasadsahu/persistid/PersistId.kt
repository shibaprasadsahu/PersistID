package com.shibaprasadsahu.persistid

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.shibaprasadsahu.persistid.internal.PersistIdImpl
import kotlinx.coroutines.flow.Flow

/**
 * Public interface for PersistId SDK
 * This is the ONLY API users can access
 *
 * Modern async-first API using Kotlin coroutines and lifecycle awareness
 *
 * **⚠️ EXPERIMENTAL**: This API is experimental and subject to change in future releases.
 * Use at your own risk in production environments.
 *
 * @since 0.1.0-alpha01
 */
@RequiresOptIn(
    message = "This API is experimental and may change in future releases. Use with caution.",
    level = RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class ExperimentalPersistIdApi

@ExperimentalPersistIdApi
interface PersistId {

    /**
     * Get persistent unique identifier asynchronously
     *
     * Example:
     * ```kotlin
     * lifecycleScope.launch {
     *     val deviceId = PersistId.getInstance().getIdentifier()
     *     Log.d("MyApp", "Device ID: $deviceId")
     * }
     * ```
     *
     * @return Unique device identifier
     */
    suspend fun getIdentifier(): String

    /**
     * Get persistent unique identifier as Flow
     * Useful for reactive UI updates
     *
     * Example:
     * ```kotlin
     * @Composable
     * fun MyScreen() {
     *     val deviceId by PersistId.getInstance()
     *         .getIdentifierFlow()
     *         .collectAsState(initial = "Loading...")
     *     Text("ID: $deviceId")
     * }
     * ```
     *
     * @return Flow emitting the unique identifier
     */
    fun getIdentifierFlow(): Flow<String>

    /**
     * Check if identifier exists
     * @return true if identifier is already generated and stored
     */
    suspend fun hasIdentifier(): Boolean

    /**
     * Clear all stored identifiers and backups
     * Note: Call regenerate() after clearing to generate a new ID immediately
     */
    suspend fun clearIdentifier()

    /**
     * Regenerate a new identifier immediately
     * This will generate a new platform-specific ID and save it to all layers
     * @return The newly generated identifier
     */
    suspend fun regenerate(): String

    /**
     * Add lifecycle-aware callback that automatically removes itself when lifecycle is destroyed
     * Perfect for Activities, Fragments, and Compose
     *
     * Example:
     * ```kotlin
     * class MainActivity : AppCompatActivity() {
     *     override fun onCreate(savedInstanceState: Bundle?) {
     *         super.onCreate(savedInstanceState)
     *
     *         PersistId.getInstance().observe(this, object : PersistIdCallback {
     *             override fun onReady(identifier: String) {
     *                 // Called every time onStart() is called
     *                 Log.d("MainActivity", "Device ID: $identifier")
     *             }
     *         })
     *         // No cleanup needed - auto-removed on destroy!
     *     }
     * }
     * ```
     *
     * @param lifecycleOwner Lifecycle owner (Activity, Fragment, etc.)
     * @param callback Callback to receive identifier updates
     */
    fun observe(lifecycleOwner: LifecycleOwner, callback: PersistIdCallback)

    /**
     * Add lifecycle-aware callback with minimum state requirement
     * Callback only receives events when lifecycle is at least in the specified state
     *
     * @param lifecycleOwner Lifecycle owner (Activity, Fragment, etc.)
     * @param minState Minimum lifecycle state (e.g., STARTED, RESUMED)
     * @param callback Callback to receive identifier updates
     */
    fun observe(
        lifecycleOwner: LifecycleOwner,
        minState: Lifecycle.State = Lifecycle.State.STARTED,
        callback: PersistIdCallback
    )

    companion object {
        /**
         * Initialize PersistId SDK
         * Call this in Application.onCreate() - returns instantly, loads in background
         * Use callbacks or Flow to get notified when ready
         *
         * Example:
         * ```kotlin
         * class MyApplication : Application() {
         *     override fun onCreate() {
         *         super.onCreate()
         *
         *         val config = PersistIdConfig.Builder()
         *             .setBackupStrategy(BackupStrategy.BLOCK_STORE)
         *             .setBackgroundSync(true)
         *             .setLogLevel(LogLevel.INFO)
         *             .build()
         *
         *         PersistId.initialize(this, config)
         *     }
         * }
         * ```
         *
         * @param context Application context
         * @param config Configuration for the SDK
         */
        @JvmStatic
        fun initialize(context: Context, config: PersistIdConfig) {
            PersistIdImpl.initialize(context, config)
        }

        /**
         * Get singleton instance of PersistId
         * Safe to call - waits for initialization to complete if needed
         *
         * @return PersistId instance
         */
        @JvmStatic
        fun getInstance(): PersistId {
            return PersistIdImpl.getInstance()
        }

        /**
         * Check if PersistId has been initialized
         * @return true if initialized (initialization may still be in progress)
         */
        @JvmStatic
        fun isInitialized(): Boolean {
            return PersistIdImpl.isInitialized()
        }
    }
}