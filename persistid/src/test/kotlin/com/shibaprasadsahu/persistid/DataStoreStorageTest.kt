package com.shibaprasadsahu.persistid

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.shibaprasadsahu.persistid.internal.storage.DataStoreStorage
import com.shibaprasadsahu.persistid.internal.utils.Logger
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for DataStore storage implementation
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DataStoreStorageTest {

    private lateinit var context: Context
    private lateinit var storage: DataStoreStorage
    private lateinit var logger: Logger

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        logger = mockk(relaxed = true)
        storage = DataStoreStorage(context, logger)
    }

    @Test
    fun `saveIdentifier and getIdentifier returns saved value`() = runTest {
        // Given
        val testId = "test-identifier-123"

        // When
        storage.saveIdentifier(testId)
        val result = storage.getIdentifier()

        // Then
        assertThat(result).isEqualTo(testId)
    }

    @Test
    fun `getIdentifier returns null when no identifier saved`() = runTest {
        // When
        val result = storage.getIdentifier()

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `hasIdentifier returns false when no identifier saved`() = runTest {
        // When
        val result = storage.hasIdentifier()

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `hasIdentifier returns true after saving identifier`() = runTest {
        // Given
        storage.saveIdentifier("test-id")

        // When
        val result = storage.hasIdentifier()

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `clearIdentifier removes saved identifier`() = runTest {
        // Given
        storage.saveIdentifier("test-id")
        assertThat(storage.hasIdentifier()).isTrue()

        // When
        storage.clearIdentifier()

        // Then
        assertThat(storage.hasIdentifier()).isFalse()
        assertThat(storage.getIdentifier()).isNull()
    }

    @Test
    fun `observeIdentifier emits saved value`() = runTest {
        // Given
        val testId = "test-identifier-456"
        storage.saveIdentifier(testId)

        // When
        val result = storage.observeIdentifier().first()

        // Then
        assertThat(result).isEqualTo(testId)
    }

    @Test
    fun `observeIdentifier emits null when no identifier`() = runTest {
        // When
        val result = storage.observeIdentifier().first()

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `saveBackupTimestamp and getLastBackupTimestamp returns saved value`() = runTest {
        // Given
        val timestamp = System.currentTimeMillis()

        // When
        storage.saveBackupTimestamp(timestamp)
        val result = storage.getLastBackupTimestamp()

        // Then
        assertThat(result).isEqualTo(timestamp)
    }

    @Test
    fun `getLastBackupTimestamp returns null when no timestamp saved`() = runTest {
        // When
        val result = storage.getLastBackupTimestamp()

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `multiple saves overwrite previous value`() = runTest {
        // Given
        storage.saveIdentifier("first-id")
        storage.saveIdentifier("second-id")

        // When
        val result = storage.getIdentifier()

        // Then
        assertThat(result).isEqualTo("second-id")
    }

    @Test
    fun `identifier persists across multiple reads`() = runTest {
        // Given
        val testId = "persistent-id"
        storage.saveIdentifier(testId)

        // When
        val read1 = storage.getIdentifier()
        val read2 = storage.getIdentifier()
        val read3 = storage.getIdentifier()

        // Then
        assertThat(read1).isEqualTo(testId)
        assertThat(read2).isEqualTo(testId)
        assertThat(read3).isEqualTo(testId)
    }
}
