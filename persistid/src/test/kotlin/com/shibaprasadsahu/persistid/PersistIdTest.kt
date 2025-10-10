package com.shibaprasadsahu.persistid

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Integration tests for PersistId public API
 * Uses Robolectric for Android framework dependencies
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], manifest = Config.NONE)
@OptIn(ExperimentalPersistIdApi::class)
class PersistIdTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        cleanupTestEnvironment()
    }

    @After
    fun tearDown() {
        cleanupTestEnvironment()
    }

    @Test
    fun `initialize creates instance successfully`() {
        // Given
        val config = createTestConfig()

        // When
        PersistId.initialize(context, config)

        // Then
        assertThat(PersistId.isInitialized()).isTrue()
    }

    @Test
    fun `getInstance returns non-null instance after initialization`() {
        // Given
        initializePersistId()

        // When
        val instance = PersistId.getInstance()

        // Then
        assertThat(instance).isNotNull()
    }

    @Test
    fun `getIdentifier returns valid non-empty ID`() = runTest {
        // Given
        val instance = initializePersistId()

        // When
        val id = instance.getIdentifier()

        // Then
        assertThat(id).isNotEmpty()
    }

    @Test
    fun `hasIdentifier returns true after getting identifier`() = runTest {
        // Given
        val instance = initializePersistId()
        instance.getIdentifier()

        // When
        val hasId = instance.hasIdentifier()

        // Then
        assertThat(hasId).isTrue()
    }

    @Test
    fun `regenerate creates new non-empty identifier`() = runTest {
        // Given
        val instance = initializePersistId()
        val originalId = instance.getIdentifier()

        // When
        val newId = instance.regenerate()

        // Then
        assertThat(newId).isNotEmpty()
        assertThat(newId).isNotEqualTo(originalId)
    }

    @Test
    fun `getIdentifier returns consistent ID across multiple calls`() = runTest {
        // Given
        val instance = initializePersistId()

        // When
        val id1 = instance.getIdentifier()
        val id2 = instance.getIdentifier()
        val id3 = instance.getIdentifier()

        // Then
        assertThat(id1).isEqualTo(id2)
        assertThat(id2).isEqualTo(id3)
    }

    @Test
    fun `config builder uses correct default values`() {
        // When
        val config = PersistIdConfig.Builder().build()

        // Then
        assertThat(config.backupStrategy).isEqualTo(BackupStrategy.BLOCK_STORE)
        assertThat(config.logLevel).isEqualTo(LogLevel.NONE)
        assertThat(config.enableBackgroundSync).isTrue()
    }

    @Test
    fun `config builder applies custom values correctly`() {
        // When
        val config = PersistIdConfig.Builder()
            .setBackupStrategy(BackupStrategy.BLOCK_STORE)
            .setLogLevel(LogLevel.DEBUG)
            .setBackgroundSync(false)
            .build()

        // Then
        assertThat(config.backupStrategy).isEqualTo(BackupStrategy.BLOCK_STORE)
        assertThat(config.logLevel).isEqualTo(LogLevel.DEBUG)
        assertThat(config.enableBackgroundSync).isFalse()
    }

    @Test
    fun `multiple initialization calls are idempotent`() {
        // Given
        val config = createTestConfig()

        // When
        PersistId.initialize(context, config)
        PersistId.initialize(context, config)
        PersistId.initialize(context, config)

        // Then
        assertThat(PersistId.isInitialized()).isTrue()
    }

    @Test
    fun `getIdentifierFlow emits current identifier`() = runTest {
        // Given
        val instance = initializePersistId()
        val expectedId = instance.getIdentifier()

        // When
        val flowId = instance.getIdentifierFlow().first()

        // Then
        assertThat(flowId).isEqualTo(expectedId)
    }

    // Helper functions to reduce boilerplate

    private fun createTestConfig(): PersistIdConfig {
        return PersistIdConfig.Builder()
            .setBackupStrategy(BackupStrategy.NONE)
            .setLogLevel(LogLevel.NONE)
            .setBackgroundSync(false)
            .build()
    }

    private fun initializePersistId(): PersistId {
        PersistId.initialize(context, createTestConfig())
        return PersistId.getInstance()
    }

    private fun cleanupTestEnvironment() {
        // Clean up DataStore files
        val dataStoreDir = File(context.filesDir, "datastore")
        dataStoreDir.deleteRecursively()

        // Clean up backup files
        val backupFile = File(context.filesDir, "persistid_backup.txt")
        backupFile.delete()

        // Cancel any WorkManager tasks
        try {
            androidx.work.WorkManager.getInstance(context).cancelAllWork()
        } catch (_: Exception) {
            // WorkManager may not be initialized
        }

        // Reset singleton using reflection (necessary for test isolation)
        resetSingleton()
    }

    private fun resetSingleton() {
        try {
            val implClass = Class.forName("com.shibaprasadsahu.persistid.internal.PersistIdImpl")
            val companionClass = Class.forName($$"com.shibaprasadsahu.persistid.internal.PersistIdImpl$Companion")

            val companionField = implClass.getDeclaredField("Companion")
            companionField.isAccessible = true
            val companion = companionField.get(null)

            if (companion != null) {
                synchronized(companion) {
                    val instanceField = companionClass.getDeclaredField("instance")
                    instanceField.isAccessible = true
                    instanceField.set(companion, null)
                }
            }
        } catch (_: Exception) {
            // Ignore reflection failures
        }
    }
}
