# --------------------------------------------------
# AndroidSnooper Core Proguard Rules
# --------------------------------------------------

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Keep Snooper public API
-keep class com.prateekj.snooper.** { *; }

# Keep network models
-keep class com.prateekj.snooper.networksnooper.model.** { *; }

# Keep interceptors
-keep class com.prateekj.snooper.okhttp.** { *; }

# Keep Spring support
-keep class org.springframework.http.client.** { *; }

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**

# Spring Android
-dontwarn org.springframework.**

# Mockk / tests
-dontwarn io.mockk.**

# Keep annotations
-keepattributes *Annotation*

# Keep debug information
-keepattributes SourceFile,LineNumberTable

# Avoid aggressive stripping
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Prevent warning spam
-ignorewarnings
