# PersistID

[![CI](https://github.com/shibaprasadsahu/PersistID/actions/workflows/ci.yml/badge.svg)](https://github.com/shibaprasadsahu/PersistID/actions/workflows/ci.yml)
[![JitPack](https://jitpack.io/v/shibaprasadsahu/PersistID.svg)](https://jitpack.io/#shibaprasadsahu/PersistID)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A modern, lightweight Android library for generating and persisting unique device identifiers with cloud backup.

> **⚠️ EARLY ALPHA**: This library is in early alpha stage (0.1-alpha01). The API is experimental and may change in future releases. Use with caution and thorough testing before deploying to production.

## Features

- ⚡ **Asynchronous Operations** - Built with Kotlin coroutines for non-blocking execution and background preloading
- 🔄 **Reliable Cloud Backup** - BlockStore integration ensures identifier persistence across uninstalls and factory resets
- 🎯 **Lifecycle Integration** - Automatic lifecycle management with Android's lifecycle components
- 📦 **Lightweight** - Minimal dependencies with optimized footprint
- 🛡️ **Broad Compatibility** - Supports Android 5.0+ (API 21-36)
- 🔧 **Configurable Logging** - Structured log levels for debugging and production (VERBOSE, DEBUG, INFO, WARN, ERROR, NONE)
- ♻️ **Automatic Resource Management** - Lifecycle-aware callbacks with automatic cleanup
- 🌐 **Cross-Device Synchronization** - Restore identifiers across devices through Google account integration

## Installation

[![Latest Release](https://img.shields.io/github/v/release/shibaprasadsahu/PersistID?include_prereleases&label=release)](https://github.com/shibaprasadsahu/PersistID/releases)
[![JitPack](https://jitpack.io/v/shibaprasadsahu/PersistID.svg)](https://jitpack.io/#shibaprasadsahu/PersistID)

### Step 1: Add JitPack repository

Add JitPack to your `settings.gradle.kts`:
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### Step 2: Add dependency

Add PersistID to your app's `build.gradle.kts`:
```kotlin
dependencies {
    implementation("com.github.shibaprasadsahu:PersistID:0.1-alpha01")
}
```

> **Note**: Replace `0.1-alpha01` with the latest version from [releases](https://github.com/shibaprasadsahu/PersistID/releases).

**Note**: Experimental API requires opt-in:
```kotlin
@OptIn(ExperimentalPersistIdApi::class)
fun setupPersistId() {
    PersistId.initialize(context, config)
}
```

## Usage

### Initialize (in Application class)

Initialize PersistID in your Application class. The initialization is non-blocking and performs background loading.

```kotlin
class MyApplication : Application() {
    @OptIn(ExperimentalPersistIdApi::class)
    override fun onCreate() {
        super.onCreate()

        val config = PersistIdConfig.Builder()
            .setBackupStrategy(BackupStrategy.BLOCK_STORE) // Enables local and cloud backup (recommended)
            .setBackgroundSync(true) // Enables automatic 24-hour sync interval
            .setLogLevel(LogLevel.INFO) // Configures log verbosity
            .build()

        PersistId.initialize(this, config)
    }
}
```

### Get Identifier

**Option 1: Lifecycle-Aware Callback (Recommended for Activities/Fragments/Compose)**
```kotlin
@OptIn(ExperimentalPersistIdApi::class)
fun observeId() {
    PersistId.getInstance().observe(lifecycleOwner, object : PersistIdCallback {
        override fun onReady(identifier: String) {
            // Invoked on the main thread when identifier is available
            // Triggered on each lifecycle start event
            println("Device ID: $identifier")
        }

        override fun onError(error: Exception) {
            // Optional error handling
        }
    })
    // Automatic cleanup when lifecycle is destroyed
}
```

**Compose Example:**
```kotlin
@OptIn(ExperimentalPersistIdApi::class)
@Composable
fun MyScreen() {
    val lifecycleOwner = LocalLifecycleOwner.current
    var deviceId by remember { mutableStateOf("Loading...") }

    DisposableEffect(Unit) {
        PersistId.getInstance().observe(lifecycleOwner, object : PersistIdCallback {
            override fun onReady(id: String) {
                deviceId = id
            }
        })
        onDispose { } // Automatic cleanup
    }
}
```

**Option 2: Suspend Function**
```kotlin
@OptIn(ExperimentalPersistIdApi::class)
suspend fun getDeviceId() {
    val id = PersistId.getInstance().getIdentifier()
    println("Device ID: $id")
}
```

**Option 3: Flow (Reactive updates from DataStore)**
```kotlin
@OptIn(ExperimentalPersistIdApi::class)
fun observeFlow() {
    PersistId.getInstance()
        .getIdentifierFlow()
        .collect { id ->
            println("Device ID: $id")
            // Emitted when identifier changes in DataStore
        }
}
```

**Compose with Flow:**
```kotlin
@OptIn(ExperimentalPersistIdApi::class)
@Composable
fun MyScreen() {
    val deviceId by PersistId.getInstance()
        .getIdentifierFlow()
        .collectAsState(initial = "Loading...")

    Text("ID: $deviceId")
}
```

### Regenerate & Clear
```kotlin
@OptIn(ExperimentalPersistIdApi::class)
suspend fun manageId() {
    val persistId = PersistId.getInstance()

    // Check identifier existence
    val hasId = persistId.hasIdentifier()

    // Regenerate identifier (clears existing and creates new)
    val newId = persistId.regenerate()
    println("New ID: $newId")

    // Clear identifier (new identifier generated on next app launch)
    persistId.clearIdentifier()
}
```

### Check Initialization Status
```kotlin
@OptIn(ExperimentalPersistIdApi::class)
fun checkInit() {
    if (PersistId.isInitialized()) {
        PersistId.getInstance().observe(this, callback)
    }
}
```

## Backup Strategies

### `BLOCK_STORE` (Recommended - Default)
- **Local + Cloud**: Stores data locally on device AND syncs to Google cloud when backup is enabled
- **Always Available**: Local storage works offline, cloud sync happens in background
- **Survives Uninstall**: Restores identifier after app reinstallation (same signing key)
- **Survives Factory Reset**: Restores via Google account on new device
- **App Identification**: Uses package name + signing key for security
- **Requirements**: Android 9+ (API 28+) with Google Play Services

**⚠️ Important Notes:**
- **BlockStore data is cleared** when user explicitly clears app data via Settings
- **However, ANDROID_ID persists** through app data clearing (system-level identifier)
- Same identifier is regenerated for the same app (signing key) + user profile combination
- Even after clearing app data, the app will have the same ANDROID_ID on that device

### `NONE`
- **Local Only**: Identifier persists only in DataStore (app-private storage)
- **No Cloud Backup**: Does not survive app uninstall or factory reset
- **Clears with App Data**: Lost when user clears app data
- **Use Case**: When cloud backup is not desired or GDPR compliance requires local-only storage

## Background Sync

PersistID automatically syncs backups to BlockStore using WorkManager:

- **Immediate**: First backup happens on app launch with retry (exponential backoff: 30s, 60s, 120s)
- **Periodic**: Auto-syncs every 24 hours to keep cloud backups fresh
- **Smart**: Only runs when network available and battery not low
- **Cross-Device**: Enables ID persistence across devices via Google account
- **Configurable**: Disable via `.setBackgroundSync(false)` if needed

## How It Works

### Architecture Overview

PersistID uses a clean architecture with a clear separation between public API and internal implementation:

#### Public API (`PersistId` interface)
- **What users access**: The only API surface available to developers
- **Location**: `com.shibaprasadsahu.persistid.PersistId`
- **Purpose**: Provides all public methods for identifier management
- **Thread-safe**: All methods are safe to call from any thread

#### Internal Implementation (`PersistIdImpl` class)
- **Hidden from users**: Located in `internal` package - not accessible
- **Purpose**: Handles async operations, lifecycle management, and background loading
- **Features**:
  - Fully async with Kotlin coroutines
  - Lifecycle-aware callbacks with automatic cleanup
  - Background preloading for instant access
  - Main thread callback delivery

#### Internal Components (All in `internal` package)

**Storage Layer**:
- `StorageProvider` - Factory for creating storage instances
- `DataStoreStorage` - Encrypted DataStore for local persistence

**Backup Layer**:
- `BackupManager` - Manages backup strategy selection
- `BlockStoreBackup` - Google BlockStore cloud backup implementation
- `NoOpBackup` - No-op implementation when backup is disabled

**Sync Layer**:
- `BackupScheduler` - Schedules WorkManager jobs for background sync
- `BackupSyncWorker` - Worker that performs periodic backups

**Data Layer**:
- `IdentifierRepository` - Repository interface
- `IdentifierRepositoryImpl` - Implementation with multi-layer persistence
- `IdentifierGenerator` - Generates platform-specific identifiers

**Utilities**:
- `Logger` - Structured logging with configurable levels

### Access Patterns

**1. Lifecycle Callback** (Recommended for UI)
```kotlin
// Fires on every onStart, auto-cleanup on destroy
PersistId.getInstance().observe(lifecycleOwner) { identifier ->
    // Use identifier
}
```

**2. Suspend Function** (For background operations)
```kotlin
suspend fun doSomething() {
    val id = PersistId.getInstance().getIdentifier()
}
```

**3. Flow** (For reactive UI)
```kotlin
PersistId.getInstance()
    .getIdentifierFlow()
    .collect { id -> /* Update UI */ }
```

### Persistence Layers

1. **DataStore** - App-private local storage (primary source)
2. **BlockStore** - Local + Google cloud backup (survives uninstall/factory reset)
3. **Platform Fallback** - ANDROID_ID as last resort

**Restore Priority**: DataStore → BlockStore → Platform ID → Generate New

### Identifier Persistence Scenarios

| Scenario | DataStore | BlockStore | ANDROID_ID | Result |
|----------|-----------|------------|------------|--------|
| **Normal app restart** | ✅ Persists | ✅ Persists | ✅ Same | Same ID |
| **Clear app data** | ❌ Cleared | ❌ Cleared | ✅ Same | **Same ID** (ANDROID_ID) |
| **Uninstall + Reinstall** | ❌ Cleared | ✅ Restored | ✅ Same | Same ID (from BlockStore) |
| **Factory reset** | ❌ Cleared | ✅ Restored* | ❌ New | Same ID* (if Google backup enabled) |
| **New device** | ❌ None | ✅ Restored* | ❌ Different | Same ID* (if signed in to Google) |

*BlockStore restore requires Google backup enabled and user signed in with same Google account.

**Key Insight**: On Android 8.0+ (API 26+), ANDROID_ID is **scoped to app signing key + user profile**, meaning:
- Same identifier even after clearing app data
- Same identifier on the same device for the same user
- Different identifier on different devices (unless restored via BlockStore)
- Different identifier if app signing key changes

## Requirements

- Android 5.0+ (API 21)
- Kotlin coroutines support

## ProGuard

ProGuard rules are automatically applied via `consumer-rules.pro`. No manual configuration needed.

## Documentation

- [AGENTS.md](AGENTS.md) - Architecture and development guide
- [RELEASE.md](RELEASE.md) - How to create releases
- [WORKFLOWS.md](WORKFLOWS.md) - CI/CD workflows explained

## Contributing

Contributions are welcome! Please follow the development guidelines in [AGENTS.md](AGENTS.md).

This project uses GitHub Actions for continuous integration:
- **Lint Check** - Runs on every push to main
- **Unit Tests** - Runs 46 tests with Robolectric
- **Build** - Creates release AAR
- **Release** - Automated releases on git tags

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

Made with ❤️ by [Shiba Prasad Sahu](https://github.com/shibaprasadsahu)
