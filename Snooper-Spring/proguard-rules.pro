# --------------------------------------------------
# AndroidSnooper Proguard Rules
# --------------------------------------------------

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Keep okhttp / okio
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep Spring Android
-dontwarn org.springframework.**
-keep class org.springframework.** { *; }

# Keep snooper API
-keep class com.prateekj.snooper.** { *; }

# Keep interceptor classes
-keep class com.prateekj.snooper.okhttp.** { *; }

# Keep model classes
-keep class com.prateekj.snooper.networksnooper.model.** { *; }

# Keep annotations
-keepattributes *Annotation*

# Keep debug info
-keepattributes SourceFile,LineNumberTable

# Reduce warning spam
-ignorewarnings
