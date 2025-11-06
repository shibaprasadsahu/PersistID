package com.shibaprasadsahu.persistid

/**
 * Backup strategy for identifier persistence
 *
 * **⚠️ EXPERIMENTAL**: This API is experimental and subject to change.
 *
 * @since 0.1.0-alpha01
 */
@ExperimentalPersistIdApi
enum class BackupStrategy {
    /**
     * Use Android BlockStore API (Recommended)
     *
     * Features:
     * - Stores data locally on device (always available)
     * - Automatically syncs to Google cloud when backup is enabled
     * - Survives app uninstall and factory reset
     * - Cross-device restore when user sets up new device
     * - Works on Android 9+ with Google Play Services
     *
     * Note: If Google backup is disabled, data still persists locally on device
     * but won't restore to new devices
     */
    BLOCK_STORE,

    /**
     * No backup strategy
     * Identifier persists only in DataStore (local storage)
     * Does not survive app uninstall or factory reset
     */
    NONE
}