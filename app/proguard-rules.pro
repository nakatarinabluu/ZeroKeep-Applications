# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /usr/local/android-sdk-linux/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-dontwarn okio.**
-dontwarn javax.annotation.**

# Gson
-keep class com.vaultguard.app.data.remote.dto.** { *; }

# Hilt
-keep class com.vaultguard.app.VaultGuardApp { *; }
-keep class dagger.hilt.** { *; }
-keep interface dagger.hilt.** { *; }
-keep public class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper
-keep class javax.inject.** { *; }

# Compose
-keep class androidx.compose.** { *; }

# --- HARDENED SECURITY OBFUSCATION ---

# 1. Flatten package hierarchy (makes analysis harder)
-repackageclasses ''
-allowaccessmodification

# 2. Obfuscate Security Classes (but keep JNI methods identifiable by C++)
-keepclasseswithmembernames class * {
    native <methods>;
}

# 3. Allow Obfuscation of SecurityManager but keep entry points if needed
# We do NOT keep the class name "SecurityManager", it should be renamed to something random like "a.b.c"
# However, we must ensure it doesn't break DI.
# Hilt usually needs -keep for injected constructors, handled by Hilt rules above.

# 4. Aggressively rename fields in domain/data models (except JSON mapped ones)
-keepclassmembers class com.vaultguard.app.data.remote.dto.** {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 5. Remove Log calls in Release builds (Anti-Forensics)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}
