# PersistId Library - Consumer ProGuard Rules
# These rules are automatically applied to apps using this library

# ===========================
# Keep Public API (Required)
# ===========================
-keep public class com.shibaprasadsahu.persistid.PersistId {
    public *;
}
-keep public interface com.shibaprasadsahu.persistid.PersistIdCallback {
    *;
}
-keep public class com.shibaprasadsahu.persistid.PersistIdConfig {
    public *;
}
-keep public class com.shibaprasadsahu.persistid.PersistIdConfig$Builder {
    public *;
}
-keep public enum com.shibaprasadsahu.persistid.BackupStrategy {
    *;
}
-keep public enum com.shibaprasadsahu.persistid.LogLevel {
    *;
}

# ===========================
# DataStore (Required)
# ===========================
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# ===========================
# Coroutines (Required)
# ===========================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}

# ===========================
# BlockStore (Required)
# ===========================
-keep class com.google.android.gms.auth.blockstore.** { *; }
-dontwarn com.google.android.gms.auth.blockstore.**

# ===========================
# WorkManager (Required)
# ===========================
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(...);
}
-keep class com.shibaprasadsahu.persistid.internal.sync.BackupSyncWorker {
    public <init>(...);
}

# ===========================
# Kotlin Reflection (Optional - for better stack traces)
# ===========================
-keep class kotlin.Metadata { *; }