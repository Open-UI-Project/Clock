# ProGuard rules for org.openui.clock

# Keep Compose models & Room entities
-keep class org.openui.clock.data.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }

# Kotlin Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }

# Keep Activity and Android components
-keep public class org.openui.clock.MainActivity
-keep public class org.openui.clock.alarm.AlarmReceiver
-keep public class org.openui.clock.alarm.AlarmService

# Suppress warnings
-dontwarn org.openui.clock.**
