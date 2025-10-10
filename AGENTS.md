# PersistID - Development Guide

## Architecture Overview

PersistID is built with modern Android best practices using Kotlin coroutines for async operations.

### Core Components

```
┌──────────────────────────────────┐
│         Public API               │
│  PersistId (interface)           │
│  PersistIdConfig (data class)    │
│  PersistIdCallback (interface)   │
│  BackupStrategy (enum)           │
│  LogLevel (enum)                 │
└────────────┬─────────────────────┘
             │
┌────────────▼─────────────────────┐
│  Internal Implementation         │
│  PersistIdImpl (singleton)       │
│  - Lifecycle management          │
│  - Callback handling             │
│  - Background preloading         │
└────────────┬─────────────────────┘
             │
     ┌───────┴──────────┐
     │                  │
┌────▼────────┐  ┌──────▼──────────┐
│ Repository  │  │ BackupScheduler │
│             │  │ (WorkManager)   │
└──────┬──────┘  └─────────────────┘
       │
   ┌───┴────────────┐
   │                │
┌──▼────────┐  ┌───▼──────┐
│ Storage   │  │  Backup  │
│ Provider  │  │  Manager │
└───────────┘  └────┬─────┘
   │                │
┌──▼────────┐  ┌───▼──────────┐
│ DataStore │  │ BlockStore   │
│           │  │ NoOpBackup   │
└───────────┘  └──────────────┘
```

## Key Design Patterns

### 1. Builder Pattern
`PersistIdConfig.Builder()` - Fluent configuration

### 2. Strategy Pattern
`BackupManager` - Pluggable backup strategies

### 3. Repository Pattern
`IdentifierRepository` - Data access abstraction

### 4. Singleton Pattern
`PersistIdImpl` - Thread-safe instance management

## Performance Optimizations

### Asynchronous Operations
- All I/O operations utilize `Dispatchers.IO`
- Computation-intensive tasks use `Dispatchers.Default`
- Storage and backup operations execute in parallel using `async/await`

### Caching Strategy
- `StateFlow` maintains in-memory cache
- Background preloading during initialization
- Near-zero latency for cached reads

### Concurrency Control
- `Mutex` ensures thread-safe operations
- `SupervisorJob` provides failure isolation
- Timeout handling for external services (5s for BlockStore)

## File Structure

```
persistid/
├── src/main/kotlin/com/shibaprasadsahu/persistid/
│   ├── PersistId.kt                    # Public API interface
│   ├── PersistIdConfig.kt              # Configuration builder
│   ├── PersistIdCallback.kt            # Lifecycle callback interface
│   ├── BackupStrategy.kt               # Backup strategies enum
│   ├── LogLevel.kt                     # Log levels enum
│   └── internal/                       # Internal implementation (hidden from users)
│       ├── PersistIdImpl.kt            # Main implementation with lifecycle
│       ├── repository/
│       │   ├── IdentifierRepository.kt
│       │   └── IdentifierRepositoryImpl.kt
│       ├── storage/
│       │   ├── StorageProvider.kt
│       │   └── DataStoreStorage.kt
│       ├── backup/
│       │   ├── BackupManager.kt        # Factory for backup strategies
│       │   ├── BlockStoreBackup.kt     # Google BlockStore implementation
│       │   └── NoOpBackup.kt           # No-op implementation
│       ├── generator/
│       │   └── IdentifierGenerator.kt  # UUID generation + platform ID
│       ├── sync/
│       │   ├── BackupScheduler.kt      # WorkManager scheduling
│       │   └── BackupSyncWorker.kt     # Background sync worker
│       └── utils/
│           └── Logger.kt               # Structured logging
```

## API Design Principles

### 1. Minimal Public Surface
Public API is limited to essential components:
- `PersistId` interface
- `PersistIdConfig` with Builder pattern
- `PersistIdCallback` interface
- `BackupStrategy` enum (BLOCK_STORE, NONE)
- `LogLevel` enum

All implementation details are marked `internal`.

### 2. Asynchronous-First Design
All operations leverage Kotlin coroutines:
- Suspending functions for data access operations
- Flow API for reactive state updates
- Lifecycle-aware callbacks for UI components

### 3. Safe Defaults
- Backup strategy: BLOCK_STORE (local storage with cloud synchronization)
- Background synchronization: Enabled
- Logging level: NONE (production-safe default)

## Testing Strategy

### Modern Test Stack
- **Robolectric** - Android framework simulation
- **MockK** - Kotlin-friendly mocking
- **Google Truth** - Fluent assertions (`assertThat`)
- **Coroutine Test** - Suspending function testing
- **Turbine** - Flow testing

### Test Coverage
1. **PersistIdTest** - Public API integration tests
2. **DataStoreStorageTest** - Storage layer tests
3. **IdentifierRepositoryTest** - Repository logic tests
4. **IdentifierGeneratorTest** - ID generation tests

### Testing Practices
- Given-When-Then structure
- Helper functions to reduce boilerplate
- Proper cleanup with lifecycle simulation
- No Thread.sleep() or blocking code
- Test isolation with singleton reset

## Dependencies

### Core (Required)
- `kotlinx-coroutines-android` - Async operations
- `androidx.datastore-preferences` - Local storage
- `androidx.lifecycle-runtime-ktx` - Lifecycle awareness
- `androidx.work-runtime-ktx` - Background sync

### Backup (Required for BLOCK_STORE)
- `play-services-auth-blockstore` - Google BlockStore API

All dependencies managed via `libs.versions.toml` for easy updates.

## ProGuard Configuration

### Keep Rules
- Public API classes
- Internal for functionality
- DataStore, Coroutines, BlockStore

### Optimization
- Remove logging in release
- Obfuscate internal classes
- Keep debugging attributes

## Migration Guide

### From 0.x.x (Sync API)
```kotlin
// Old
val id = PersistId.getInstance().getIdentifier()

// New
lifecycleScope.launch {
    val id = PersistId.getInstance().getIdentifier()
}
```

### From SharedPreferences
Data is automatically migrated to DataStore on first access.

## Contributing

### Prerequisites
- Android Studio Koala+
- Kotlin 2.2.20+
- Min SDK 21, Target SDK 36

### Development Setup
1. Clone repo
2. Open in Android Studio
3. Sync Gradle
4. Run sample app

### Code Style
- Follow Kotlin conventions
- Use `internal` for implementation
- Document public APIs
- Add tests for new features

### Pull Request Process
1. Create feature branch
2. Write tests
3. Update docs
4. Submit PR with description

## Performance Benchmarks

### Cold Start (First Access)
- Identifier generation and storage: ~5-10ms
- Backup operation: ~50-100ms (executed in parallel)

### Warm Access (Cached)
- Retrieval from cache: <1ms (in-memory read)
- Flow emission: 0ms (hot stream)

### Concurrency
- Thread-safe operations using Mutex
- Non-blocking main thread execution

## Security Considerations

### Data Protection
- DataStore encrypted by OS (API 23+)
- UUID format (not device-specific)
- No PII collected

### Backup Security
- BlockStore: Google-managed encryption
- AutoBackup: OS-level encryption
- Keys never leave device

## Known Limitations

1. **BlockStore** requires Google Play Services (API 28+)
2. **Cloud backup** requires user to have Google backup enabled
3. **Identifier changes** after clear or regenerate

## Design Decisions

### Why BlockStore Over Auto Backup?
- **Simplified architecture**: Single strategy covers most use cases
- **Reliability**: Functions independently of app's `allowBackup` setting
- **Cross-device synchronization**: Provides cloud-based sync beyond local backup
- **Availability**: Combines local storage with optional cloud synchronization

### Why No Additional Encryption?
- **DataStore**: Encrypted by Android OS (API 23+)
- **BlockStore**: Google-managed encryption
- **Cross-device restoration**: Additional encryption would prevent restore on new devices
- **Privacy**: UUIDs do not contain personally identifiable information

## Support

- Issues: [GitHub Issues](https://github.com/shibaprasadsahu/persistid/issues)
- Email: shibaprasadsahu943@gmail.com

---

**Last Updated:** 2025-10-07
**Library Version:** 0.0.1
