package com.shibaprasadsahu.persistid.internal.generator

import android.annotation.SuppressLint
import android.os.Build
import com.shibaprasadsahu.persistid.internal.utils.Logger
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Internal identifier generator with platform-specific fallbacks
 * Priority: Build.SERIAL (API 21-25) → ANDROID_ID (API 26+) → UUID
 */
internal class IdentifierGenerator(
    private val androidId: String?,
    private val logger: Logger
) {

    /**
     * Generate platform-specific persistent ID
     * Uses Build.SERIAL on API 21-25, ANDROID_ID on API 26+
     * Returns the RAW platform ID without modification
     */
    @OptIn(ExperimentalUuidApi::class)
    fun generate(): String {
        // Try Build.SERIAL on old devices (API 21-25) - highest persistence
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            getBuildSerial()?.let { serial ->
                logger.debug("Using Build.SERIAL")
                return serial // Return RAW serial
            }
        }

        // Try ANDROID_ID (API 26+) - app-scoped but persists uninstall
        androidId?.let { id ->
            if (isValidAndroidId(id)) {
                logger.debug("Using ANDROID_ID")
                return id // Return RAW ANDROID_ID
            }
        }

        // Fallback to Kotlin UUID (no persistence)
        logger.warn("Using UUID fallback (no platform ID)")
        return Uuid.random().toString()
    }

    /**
     * Get Build.SERIAL safely on API 21-25
     */
    @SuppressLint("HardwareIds")
    private fun getBuildSerial(): String? {
        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                val serial = Build.SERIAL
                if (serial.isNotBlank() &&
                    serial != "unknown" &&
                    serial != "0" &&
                    serial.length > 3) {
                    serial
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            logger.error("Failed to get Build.SERIAL", e)
            null
        }
    }

    /**
     * Validate ANDROID_ID - filter out invalid values
     */
    private fun isValidAndroidId(androidId: String): Boolean {
        return androidId.isNotBlank() &&
            androidId != "unknown" &&
            androidId.length >= 16
    }

    fun validate(identifier: String): Boolean {
        // Accept any non-empty string as valid
        return identifier.isNotBlank()
    }
}