# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn androidx.room.paging.**

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.work.ListenableWorker
-keep class androidx.work.** { *; }

# App model / Room rows (Gson-free, but reflection via Room converters)
-keep class com.secondmemory.app.domain.** { *; }
-keep class com.secondmemory.app.data.** { *; }

# FileProvider + Compose runtime
-keep class androidx.core.content.FileProvider { *; }
-keep class androidx.compose.runtime.** { *; }

-dontwarn org.bouncycastle.**
-dontwarn javax.naming.**
