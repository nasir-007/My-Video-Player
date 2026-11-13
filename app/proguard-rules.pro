# KinoX ProGuard / R8 Rules
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Kotlin Serialization for Navigation Compose @Serializable routes
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep,includedescriptorclasses class com.example.ui.navigation.**$$serializer { *; }
-keepclassmembers class com.example.ui.navigation.** {
    *** Companion;
}

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
