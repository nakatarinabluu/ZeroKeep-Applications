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
