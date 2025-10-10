package com.shibaprasadsahu.persistid.sample

import android.app.Application
import com.shibaprasadsahu.persistid.BackupStrategy
import com.shibaprasadsahu.persistid.ExperimentalPersistIdApi
import com.shibaprasadsahu.persistid.LogLevel
import com.shibaprasadsahu.persistid.PersistId
import com.shibaprasadsahu.persistid.PersistIdConfig

class MyApplication : Application() {

    @OptIn(ExperimentalPersistIdApi::class)
    override fun onCreate() {
        super.onCreate()

        // Initialize PersistId - loads in background
        val config = PersistIdConfig.Builder()
            .setBackupStrategy(BackupStrategy.BLOCK_STORE) // Local + Cloud backup
            .setBackgroundSync(true) // Auto-backup every 24h
            .setLogLevel(LogLevel.DEBUG) // Debug mode with structured logging
            .build()

        PersistId.initialize(this, config)
    }

}