package com.shibaprasadsahu.persistid

import android.os.Build
import com.shibaprasadsahu.persistid.internal.generator.IdentifierGenerator
import com.shibaprasadsahu.persistid.internal.utils.Logger
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class IdentifierGeneratorTest {

    private lateinit var logger: Logger

    @Before
    fun setup() {
        logger = mockk(relaxed = true)
    }

    @Test
    fun `generate returns valid ANDROID_ID on API 26+`() {
        // Given
        val validAndroidId = "a1b2c3d4e5f6g7h8"
        val generator = IdentifierGenerator(validAndroidId, logger)

        // When
        val result = generator.generate()

        // Then
        assertEquals(validAndroidId, result)
        verify { logger.debug("Using ANDROID_ID") }
    }

    @Test
    fun `generate returns UUID when ANDROID_ID is invalid`() {
        // Given
        val invalidAndroidId = "unknown"
        val generator = IdentifierGenerator(invalidAndroidId, logger)

        // When
        val result = generator.generate()

        // Then
        assertNotNull(result)
        assertTrue(result.contains("-")) // UUID format
        verify { logger.warn("Using UUID fallback (no platform ID)") }
    }

    @Test
    fun `generate returns UUID when ANDROID_ID is null`() {
        // Given
        val generator = IdentifierGenerator(null, logger)

        // When
        val result = generator.generate()

        // Then
        assertNotNull(result)
        assertTrue(result.contains("-")) // UUID format
        verify { logger.warn("Using UUID fallback (no platform ID)") }
    }

    @Test
    fun `generate returns UUID when ANDROID_ID is blank`() {
        // Given
        val generator = IdentifierGenerator("", logger)

        // When
        val result = generator.generate()

        // Then
        assertNotNull(result)
        assertTrue(result.contains("-")) // UUID format
    }

    @Test
    fun `generate returns UUID when ANDROID_ID is too short`() {
        // Given
        val shortAndroidId = "abc123" // Less than 16 chars
        val generator = IdentifierGenerator(shortAndroidId, logger)

        // When
        val result = generator.generate()

        // Then
        assertNotNull(result)
        assertTrue(result.contains("-")) // UUID format
    }

    @Test
    fun `validate returns true for non-empty string`() {
        // Given
        val generator = IdentifierGenerator("test", logger)

        // When
        val result = generator.validate("a1b2c3d4e5f6g7h8")

        // Then
        assertTrue(result)
    }

    @Test
    fun `validate returns false for blank string`() {
        // Given
        val generator = IdentifierGenerator("test", logger)

        // When
        val result = generator.validate("   ")

        // Then
        assertFalse(result)
    }

    @Test
    fun `validate returns false for empty string`() {
        // Given
        val generator = IdentifierGenerator("test", logger)

        // When
        val result = generator.validate("")

        // Then
        assertFalse(result)
    }

    @Test
    fun `generated UUID is different each time`() {
        // Given
        val generator = IdentifierGenerator(null, logger)

        // When
        val id1 = generator.generate()
        val id2 = generator.generate()

        // Then
        assertNotNull(id1)
        assertNotNull(id2)
        assertTrue(id1 != id2) // Different UUIDs
    }
}
