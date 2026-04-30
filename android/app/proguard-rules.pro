# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep Gson serialization
-keep class com.languagepartner.app.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
