package com.shibaprasadsahu.persistid.internal.generator

import com.shibaprasadsahu.persistid.internal.utils.Logger
import platform.UIKit.UIDevice
import platform.Foundation.NSUUID

/**
 * iOS implementation of identifier generator
 * Uses IDFV (identifierForVendor) or UUID fallback
 */
internal actual class IdentifierGenerator actual constructor(
    private val logger: Logger
) {

    /**
     * Generate platform-specific persistent ID
     * Uses IDFV (identifierForVendor) on iOS
     * Returns the RAW platform ID without modification
     */
    actual fun generate(): String {
        // Try IDFV (identifierForVendor) - persists until all vendor apps are uninstalled
        val idfv = try {
            UIDevice.currentDevice.identifierForVendor?.UUIDString
        } catch (e: Exception) {
            logger.error("Failed to get IDFV", e)
            null
        }

        if (idfv != null && idfv.isNotBlank()) {
            logger.debug("Using IDFV")
            return idfv
        }

        // Fallback to UUID (no persistence)
        logger.warn("Using UUID fallback (no platform ID)")
        return NSUUID().UUIDString
    }

    actual fun validate(identifier: String): Boolean {
        // Accept any non-empty string as valid
        return identifier.isNotBlank()
    }
}
