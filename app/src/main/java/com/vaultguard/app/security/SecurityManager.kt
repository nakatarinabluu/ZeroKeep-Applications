package com.vaultguard.app.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.content.SharedPreferences
import android.util.Base64

class SecurityManager(private val context: Context, private val prefs: SharedPreferences) {

    private val ANDROID_KEYSTORE = "AndroidKeyStore"
    private val WRAP_KEY_ALIAS = "VaultGuardDeviceKey" 
    private val TRANSFORMATION = "AES/GCM/NoPadding"
    private val KEY_WRAPPED_BLOB = "wrapped_master_key_blob"
    private val KEY_WRAPPED_IV = "wrapped_master_key_iv"

    // NATIVE METHODS
    external fun getAppSignature(): String
    external fun checkFrida(): Boolean

    init {
        // Load library
        System.loadLibrary("vaultguard")
        
        if (!isDeviceSecure()) {
            throw SecurityException("Device is compromised (Rooted/Hooked/Debugged)")
        }
        verifyAppSignature()
        generateWrappingKeyIfNotExists()
    }

    private fun verifyAppSignature() {
        try {
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

    // --- KEY MANAGEMENT ---

    private fun generateWrappingKeyIfNotExists() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            if (!keyStore.containsAlias(WRAP_KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                val spec = KeyGenParameterSpec.Builder(
                    WRAP_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            throw SecurityException("Failed to generate wrapping key", e)
        }
    }

    fun saveMasterKey(secretKey: SecretKey) {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            val wrappingKey = keyStore.getKey(WRAP_KEY_ALIAS, null) as SecretKey

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.WRAP_MODE, wrappingKey)
            
            val wrappedKeyBytes = cipher.wrap(secretKey)
            val iv = cipher.iv

            // Store Blob + IV
            prefs.edit()
                .putString(KEY_WRAPPED_BLOB, Base64.encodeToString(wrappedKeyBytes, Base64.NO_WRAP))
                .putString(KEY_WRAPPED_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply()
        } catch (e: Exception) {
            throw SecurityException("Failed to save master key", e)
        }
    }

    fun loadMasterKey(): SecretKey {
        try {
            val wrappedBlobStr = prefs.getString(KEY_WRAPPED_BLOB, null) ?: throw SecurityException("No Master Key Found")
            val ivStr = prefs.getString(KEY_WRAPPED_IV, null) ?: throw SecurityException("No Master Key IV Found")

            val wrappedKeyBytes = Base64.decode(wrappedBlobStr, Base64.NO_WRAP)
            val iv = Base64.decode(ivStr, Base64.NO_WRAP)

            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            val wrappingKey = keyStore.getKey(WRAP_KEY_ALIAS, null) as SecretKey

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.UNWRAP_MODE, wrappingKey, spec)

            return cipher.unwrap(wrappedKeyBytes, "AES", Cipher.SECRET_KEY) as SecretKey
        } catch (e: Exception) {
            throw SecurityException("Failed to load master key", e)
        }
    }

    fun hasMasterKey(): Boolean {
        return prefs.contains(KEY_WRAPPED_BLOB)
    }

    fun deleteKey() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            if (keyStore.containsAlias(WRAP_KEY_ALIAS)) {
                keyStore.deleteEntry(WRAP_KEY_ALIAS)
            }
            prefs.edit().remove(KEY_WRAPPED_BLOB).remove(KEY_WRAPPED_IV).apply()
        } catch (e: Exception) {
            // Ignore
        }
    }

    // --- CRYPTO UTILS ---

    fun encrypt(data: ByteArray, key: SecretKey): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal(data)
        return Pair(cipher.iv, encrypted)
    }

    fun decrypt(iv: ByteArray, encrypted: ByteArray, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher.doFinal(encrypted)
    }
}
