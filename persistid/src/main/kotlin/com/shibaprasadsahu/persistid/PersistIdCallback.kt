package com.shibaprasadsahu.persistid

/**
 * Callback interface for PersistId lifecycle events
 * Modern, clean API following Android best practices
 *
 * **⚠️ EXPERIMENTAL**: This API is experimental and subject to change.
 *
 * @since 0.1.0-alpha01
 */
@ExperimentalPersistIdApi
interface PersistIdCallback {
    /**
     * Called when PersistId is fully initialized and ready to use
     * This is called on the main thread
     *
     * @param identifier The device identifier (already loaded and cached)
     */
    fun onReady(identifier: String)

    /**
     * Called if initialization fails (rare)
     * This is called on the main thread
     *
     * @param error The error that occurred
     */
    fun onError(error: Exception) {
        // Optional - override if needed
    }
}
