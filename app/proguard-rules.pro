# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# 高德地图 SDK
-keep class com.amap.api.** { *; }
-dontwarn com.amap.api.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Generic
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
