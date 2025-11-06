# Kotlin Multiplatform (KMP) Implementation for PersistID

## 🎯 Implementation Status

This document describes the KMP implementation following **Path 3: Hybrid (Recommended)** approach.

### ✅ Completed

1. **Gradle Configuration**
   - Added Kotlin Multiplatform plugin
   - Configured Android and iOS targets (iosX64, iosArm64, iosSimulatorArm64)
   - Added KVault dependency for cross-platform secure storage
   - Updated version to 0.2-alpha01

2. **Source Set Reorganization**
   - Created `commonMain/` for shared code
   - Created `androidMain/` for Android-specific implementations
   - Created `iosMain/` for iOS-specific implementations

3. **Platform-Specific Implementations (expect/actual)**

   #### IdentifierGenerator
   - **Common (expect)**: Platform-agnostic interface
   - **Android (actual)**: Uses ANDROID_ID (API 26+) or Build.SERIAL (API 21-25)
   - **iOS (actual)**: Uses IDFV (identifierForVendor)

   #### Storage
   - **Common**: StorageProvider interface + `createStorage()` expect function
   - **Android**: DataStoreStorage (AndroidX DataStore)
   - **iOS**: KeychainStorage (KVault wrapper for iOS Keychain)

   #### BackupManager
   - **Common**: BackupManager interface + `createBackupManager()` expect function
   - **Android**: BlockStoreBackup (Google BlockStore) + NoOpBackup
   - **iOS**: ICloudBackup (NSUbiquitousKeyValueStore) + NoOpBackup

   #### BackupScheduler
   - **Common (expect)**: Platform-agnostic interface
   - **Android (actual)**: WorkManager-based 24h periodic sync
   - **iOS (actual)**: No-op (Keychain + iCloud handle persistence automatically)

## 📁 File Organization

```
persistid/src/
├── commonMain/kotlin/
│   └── com/shibaprasadsahu/persistid/
│       ├── PersistId.kt                    # Public API interface
│       ├── PersistIdConfig.kt              # Configuration
│       ├── PersistIdCallback.kt            # Callbacks
│       ├── BackupStrategy.kt               # Backup strategies
│       ├── LogLevel.kt                     # Log levels
│       └── internal/
│           ├── generator/
│           │   └── IdentifierGenerator.kt  # expect class
│           ├── storage/
│           │   └── StorageProvider.kt      # interface + expect fun
│           ├── backup/
│           │   ├── BackupManager.kt        # interface + expect fun
│           │   └── NoOpBackup.kt           # Common no-op implementation
│           ├── sync/
│           │   └── BackupScheduler.kt      # expect class
│           ├── repository/
│           │   ├── IdentifierRepository.kt
│           │   └── IdentifierRepositoryImpl.kt
│           └── utils/
│               └── Logger.kt
│
├── androidMain/kotlin/
│   └── com/shibaprasadsahu/persistid/
│       └── internal/
│           ├── PersistIdImpl.kt            # Android implementation
│           ├── generator/
│           │   └── IdentifierGenerator.kt  # actual class (ANDROID_ID)
│           ├── storage/
│           │   ├── StorageProvider.kt      # actual fun (DataStore factory)
│           │   └── DataStoreStorage.kt     # DataStore implementation
│           ├── backup/
│           │   ├── BackupManager.kt        # actual fun (BlockStore factory)
│           │   └── BlockStoreBackup.kt     # Google BlockStore
│           └── sync/
│               ├── BackupScheduler.kt      # actual class (WorkManager)
│               └── BackupSyncWorker.kt     # WorkManager worker
│
└── iosMain/kotlin/
    └── com/shibaprasadsahu/persistid/
        └── internal/
            ├── PersistIdImpl.kt            # iOS implementation (TO BE CREATED)
            ├── generator/
            │   └── IdentifierGenerator.kt  # actual class (IDFV)
            ├── storage/
            │   ├── StorageProvider.kt      # actual fun (Keychain factory)
            │   └── KeychainStorage.kt      # KVault/Keychain implementation
            ├── backup/
            │   ├── BackupManager.kt        # actual fun (iCloud factory)
            │   └── ICloudBackup.kt         # iCloud KeyValue Store
            └── sync/
                └── BackupScheduler.kt      # actual class (no-op)
```

## 🔧 How It Works

### Android
1. **Identifier**: ANDROID_ID (API 26+) or Build.SERIAL (API 21-25)
2. **Local Storage**: AndroidX DataStore Preferences
3. **Cloud Backup**: Google BlockStore (API 28+)
4. **Background Sync**: WorkManager (24h periodic + immediate on first launch)
5. **Lifecycle**: AndroidX Lifecycle components

### iOS
1. **Identifier**: IDFV (identifierForVendor from UIDevice)
2. **Local Storage**: iOS Keychain via KVault (survives app uninstall)
3. **Cloud Backup**: iCloud KeyValue Store (up to 1MB, syncs automatically)
4. **Background Sync**: Not needed (Keychain + iCloud handle persistence)
5. **Lifecycle**: Simplified (no Android-style lifecycle needed)

## 📝 Remaining Work

### Critical (Must Complete Before Testing)

1. **PersistIdImpl for iOS**
   - File: `iosMain/kotlin/.../internal/PersistIdImpl.kt`
   - Simplified version without Android Lifecycle
   - Use basic coroutines for async operations
   - Skip lifecycle-aware callbacks (iOS pattern is different)

2. **Update PersistId.kt in commonMain**
   - Remove Android-specific Lifecycle dependencies
   - Keep interface platform-agnostic
   - Move lifecycle-specific methods to Android-only extension

3. **Fix IdentifierRepositoryImpl**
   - Remove `DataStoreStorage` import (should use `StorageProvider`)
   - Update initialization to be platform-agnostic
   - Remove Android-specific Build class references

4. **Update Logger.kt**
   - Make it platform-agnostic (currently uses Android Log)
   - Use expect/actual for platform-specific logging
   - Android: android.util.Log
   - iOS: NSLog

5. **Fix AndroidMain Files**
   - Update `PersistIdImpl.initialize()` to use `createStorage()` and `createBackupManager()`
   - Pass context as `platformContext: Any` parameter
   - Update IdentifierGenerator initialization

### Nice to Have

1. **iOS Sample App**
   - Create Xcode project demonstrating iOS usage
   - Show Swift interop with the framework

2. **Update README.md**
   - Add iOS installation instructions
   - Add iOS usage examples
   - Document platform differences

3. **Testing**
   - Add commonTest for shared logic
   - Add iosTest for iOS-specific code

## 🚀 Usage

### Android (Unchanged)
```kotlin
// Application.onCreate()
val config = PersistIdConfig.Builder()
    .setBackupStrategy(BackupStrategy.BLOCK_STORE)
    .setBackgroundSync(true)
    .setLogLevel(LogLevel.INFO)
    .build()

PersistId.initialize(this, config)

// Activity
PersistId.getInstance().observe(this, object : PersistIdCallback {
    override fun onReady(identifier: String) {
        Log.d("MainActivity", "ID: $identifier")
    }
})
```

### iOS (New)
```swift
// Swift
import PersistId

// AppDelegate or SwiftUI App
let config = PersistIdConfig.Builder()
    .setBackupStrategy(backupStrategy: BackupStrategy.blockStore)
    .setBackgroundSync(enableBackgroundSync: false) // Not needed on iOS
    .setLogLevel(level: LogLevel.info)
    .build()

PersistId.companion.initialize(context: NSNull(), config: config)

// ViewController
Task {
    let identifier = try await PersistId.companion.getInstance().getIdentifier()
    print("Device ID: \(identifier)")
}
```

## 🔍 Key Design Decisions

1. **KVault for iOS Storage**
   - Provides Keychain access (secure, persistent)
   - Survives app uninstall (if user has iCloud backup)
   - Battle-tested library

2. **iCloud KeyValue Store for iOS Backup**
   - Direct equivalent to Android's BlockStore
   - Automatic sync across devices
   - No explicit scheduling needed

3. **No Background Sync on iOS**
   - Keychain persists locally
   - iCloud syncs automatically when available
   - No need for periodic WorkManager-style jobs

4. **Simplified iOS Lifecycle**
   - iOS doesn't use AndroidX Lifecycle pattern
   - Use standard async/await or Combine
   - No automatic callback registration

## ⚠️ Known Limitations

1. **iOS Background Sync**
   - Not implemented (by design, not needed)
   - iCloud handles sync automatically

2. **iOS Lifecycle Callbacks**
   - Different from Android pattern
   - iOS developers should use standard iOS patterns (Combine, async/await)

3. **Platform Context**
   - Android: Requires `Context` for DataStore, BlockStore, WorkManager
   - iOS: Doesn't need context, pass `NSNull()` or Unit

## 🧪 Testing Instructions

### Build the Project
```bash
cd /home/user/PersistID
./gradlew :persistid:build
```

### Build iOS Framework
```bash
./gradlew :persistid:linkDebugFrameworkIosArm64
./gradlew :persistid:linkDebugFrameworkIosSimulatorArm64
```

### Run Android Tests
```bash
./gradlew :persistid:testDebugUnitTest
```

### Integration Testing
1. Test Android app with existing functionality
2. Create minimal iOS Xcode project
3. Import generated framework
4. Test basic identifier retrieval

## 📊 Migration Impact

### Breaking Changes
- Version bump to 0.2-alpha01
- Library description updated to mention iOS support
- No API changes for Android users

### Compatibility
- ✅ Existing Android code continues to work
- ✅ No migration needed for current users
- ✅ iOS is new addition, not a replacement

## 📚 References

- [Kotlin Multiplatform Docs](https://kotlinlang.org/docs/multiplatform.html)
- [KVault Library](https://github.com/Liftric/KVault)
- [iOS Keychain Services](https://developer.apple.com/documentation/security/keychain_services)
- [iCloud KeyValue Storage](https://developer.apple.com/documentation/foundation/nsubiquitouskeyvaluestore)
- [IDFV Documentation](https://developer.apple.com/documentation/uikit/uidevice/identifierforvendor)

---

**Implementation Date**: 2025-11-06
**Implementation Approach**: Path 3 (Hybrid - Recommended)
**Target Platforms**: Android (API 21+), iOS (iOS 13+)
