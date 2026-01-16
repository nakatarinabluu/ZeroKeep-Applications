package com.vaultguard.app.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

import android.content.SharedPreferences

class SecurityManager(private val context: Context, private val prefs: SharedPreferences) {

    private val ANDROID_KEYSTORE = "AndroidKeyStore"
    // New Alias for the WRAPPING Key (Device Local). 
    // We do NOT use the old "VaultGuardMasterKey" alias for the wrapping key to avoid confusion, 
    // but effectively we are replacing the old logic.
    private val WRAP_KEY_ALIAS = "VaultGuardDeviceKey" 
    private val TRANSFORMATION = "AES/GCM/NoPadding"
    // private val PREFS_NAME = "vault_guard_secure_prefs" // REMOVED - Using Injected Prefs
    private val KEY_WRAPPED_BLOB = "wrapped_master_key_blob"
    private val KEY_WRAPPED_IV = "wrapped_master_key_iv"

    // SHA-256 of the Release Keystore (Place your REAL Release Key Hash here)
    // For now, this is the standard Android Debug Key SHA-256 to allow local testing.
    // NATIVE METHODS (Secrets hidden in C++)
    external fun getAppSignature(): String
    external fun checkFrida(): Boolean

    // private val EXPECTED_SIGNATURE = "..." // REMOVED - Moved to Native C++

    init {
        // Load library (if not already loaded by NetworkModule, but good practice to ensure)
        System.loadLibrary("vaultguard")
        
        if (!isDeviceSecure()) {
            throw SecurityException("Device is compromised (Rooted/Hooked/Debugged)")
        }
        verifyAppSignature()
        generateWrappingKeyIfNotExists()
    }
    
    // ...

    private fun verifyAppSignature() {
        try {
            // ... (existing signature retrieval code) ...
            val pm = context.packageManager
            val packageName = context.packageName
            val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                android.content.pm.PackageManager.GET_SIGNATURES
            }
            
            val packageInfo = pm.getPackageInfo(packageName, flags)
            
            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.signingInfo.apkContentsSigners
            } else {
                packageInfo.signatures
            }
            
            if (signatures == null || signatures.isEmpty()) {
                throw SecurityException("No signature found")
            }

            val md = java.security.MessageDigest.getInstance("SHA-256")
            val signatureBytes = signatures[0].toByteArray()
            val digest = md.digest(signatureBytes)
            val currentHash = digest.joinToString("") { "%02x".format(it) }

            // RETRIEVE EXPECTED HASH FROM NATIVE LAYER
            val expectedSignature = getAppSignature()

            val isDebuggable = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

            if (currentHash != expectedSignature) {
                if (!isDebuggable) {
                    // RELEASE BUILD: Strict Enforcement
                    android.util.Log.e("SecurityManager", "FATAL: Signature Mismatch! Expected: $expectedSignature, Found: $currentHash")
                    deleteKey()
                    kotlin.system.exitProcess(0)
                } else {
                    // DEBUG BUILD: Log warning but allow
                    android.util.Log.w("SecurityManager", "Signature Mismatch (Allowed in Debug). Current: $currentHash")
                }
            }
        } catch (e: Exception) {
             val isDebuggable = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
             if (!isDebuggable) throw SecurityException("Signature verification failed", e)
        }
    }

    // ...

    private fun isDeviceSecure(): Boolean {
        // 1. NATIVE FRIDA PROTECTION (First Line of Defense)
        if (checkFrida()) {
             android.util.Log.e("SecurityManager", "Frida/Hooking Detected via Native Scan!")
             return false
        }

        // 2. Check for basic Root binaries
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in paths) {
            if (java.io.File(path).exists()) return false
        }

        // 3. Check for Test Keys (Custom ROMs)
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) return false

        // 4. Check for Debugging
        if (android.os.Debug.isDebuggerConnected()) return false
        
        // 5. Check for Frida / Xposed (Java-based checks)
        try {
            throw Exception("Check")
        } catch (e: Exception) {
            val stackTrace = e.stackTrace
            for (element in stackTrace) {
                if (element.className.contains("de.robv.android.xposed") || 
                    element.className.contains("com.saurik.substrate") ||
                    element.className.contains("frida")) {
                    return false
                }
            }
        }

        // 6. Check for Emulator
        val isEmulator = (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")

        if (isEmulator) return false

        return true
    }
}
