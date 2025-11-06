package com.shibaprasadsahu.persistid.internal.generator

import com.shibaprasadsahu.persistid.internal.utils.Logger

/**
 * Platform-specific identifier generator
 * - Android: ANDROID_ID (API 26+) or Build.SERIAL (API 21-25) or UUID
 * - iOS: IDFV (identifierForVendor) or UUID
 */
internal expect class IdentifierGenerator(logger: Logger) {
    /**
     * Generate platform-specific persistent ID
     * Returns the RAW platform ID without modification
     */
    fun generate(): String

    /**
     * Validate identifier
     */
    fun validate(identifier: String): Boolean
}
