package com.shibaprasadsahu.persistid.internal.storage

import android.content.Context
import com.shibaprasadsahu.persistid.ExperimentalPersistIdApi
import com.shibaprasadsahu.persistid.PersistIdConfig
import com.shibaprasadsahu.persistid.internal.utils.Logger

/**
 * Android implementation of storage factory
 * Uses DataStore for local persistence
 */
@OptIn(ExperimentalPersistIdApi::class)
internal actual fun createStorage(
    platformContext: Any,
    config: PersistIdConfig,
    logger: Logger
): StorageProvider {
    require(platformContext is Context) { "Platform context must be Android Context" }
    return DataStoreStorage(platformContext, logger)
}
