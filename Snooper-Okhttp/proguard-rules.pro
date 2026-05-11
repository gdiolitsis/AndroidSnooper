# --------------------------------------------------
# AndroidSnooper Proguard Rules
# --------------------------------------------------

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Keep okhttp / okio
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep snooper public API
-keep class com.prateekj.snooper.** { *; }

# Keep model classes
-keep class com.prateekj.snooper.networksnooper.model.** { *; }

# Keep interceptor classes
-keep class com.prateekj.snooper.okhttp.** { *; }

# Keep annotations
-keepattributes *Annotation*

# Keep source/debug info
-keepattributes SourceFile,LineNumberTable

# Prevent warning spam
-ignorewarnings
