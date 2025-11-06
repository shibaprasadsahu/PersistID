package com.shibaprasadsahu.persistid.internal.storage

import com.shibaprasadsahu.persistid.ExperimentalPersistIdApi
import com.shibaprasadsahu.persistid.PersistIdConfig
import com.shibaprasadsahu.persistid.internal.utils.Logger

/**
 * iOS implementation of storage factory
 * Uses KVault (Keychain) for secure, persistent storage
 */
@OptIn(ExperimentalPersistIdApi::class)
internal actual fun createStorage(
    platformContext: Any,
    config: PersistIdConfig,
    logger: Logger
): StorageProvider {
    // On iOS, we don't need context - Keychain is system-wide
    return KeychainStorage(logger)
}
