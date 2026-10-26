# ErrorProne's compile-time annotation metadata references a JDK compiler enum.
# It is not executable Android test code and the compiler API is absent on Android.
-dontwarn javax.lang.model.element.Modifier

# AndroidJUnitRunner discovers these entry points through reflection/dex scanning.
# Preserve tests and annotations in the test APK; the app remains fully optimized.
-keep class com.byga.smartoverlay.**Test { *; }
-keepattributes *Annotation*
