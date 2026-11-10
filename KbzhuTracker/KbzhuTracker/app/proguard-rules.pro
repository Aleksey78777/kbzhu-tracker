# --- kotlinx.serialization (DTO Open Food Facts и маршруты навигации) ---
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }

-keep,includedescriptorclasses class ru.kbzhu.tracker.**$$serializer { *; }
-keepclassmembers class ru.kbzhu.tracker.** { *** Companion; }
-keepclasseswithmembers class ru.kbzhu.tracker.** { kotlinx.serialization.KSerializer serializer(...); }

-keep class ru.kbzhu.tracker.data.remote.dto.** { *; }
-keep class ru.kbzhu.tracker.ui.navigation.** { *; }

# --- Retrofit / OkHttp ---
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
