package com.vaultguard.app.security


import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.system.exitProcess

/**
 * Core Security Module for VaultGuard.
 *
 * This class handles:
 * 1. Device Integrity Checks (Root, Frida, Hooking, Debugging).
 * 2. Application Integrity Checks (Signature Verification).
 * 3. Key Management (Master Key Wrapping using AndroidKeyStore).
 * 4. Cryptographic Operations (AES-GCM Encryption/Decryption).
 *
 * Checks are performed using a "Defense in Depth" strategy, combining
 * Native (C++) checks for stealth and Java checks for breadth.
 */
class SecurityManager(private val context: Context, private val prefs: SharedPreferences) {

    private companion object {
        const val TAG = "SecurityManager"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val WRAP_KEY_ALIAS = "VaultGuardDeviceKey"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_WRAPPED_BLOB = "wrapped_master_key_blob"
        const val KEY_WRAPPED_IV = "wrapped_master_key_iv"
        
        // Critical: Native library name
        const val NATIVE_LIB = "vaultguard"
    }

    // --- NATIVE INTERFACE ---
    /**
     * Retrieves the expected SHA-256 signature hash of the APK from native code.
     * This string is obfuscated in the C++ layer.
     */
    external fun getAppSignature(): String

    /**
     * Performs a native memory scan (reading /proc/self/maps) to detect
     * hooking frameworks like Frida, GumJS, or LTrace.
     */
    external fun checkFrida(): Boolean

    init {
        try {
            System.loadLibrary(NATIVE_LIB)
        } catch (e: UnsatisfiedLinkError) {
            // If native lib fails to load, the app is likely tampered or broken.
            Log.e(TAG, "FATAL: Failed to load native security library.")
            exitApp()
        }
        
        if (!isDeviceSecure()) {
            throw SecurityException("Device integrity check failed. Environment is compromised.")
        }
        
        verifyAppSignature()
        generateWrappingKeyIfNotExists()
    }

    /**
     * Verifies the APK's signing certificate against the expected hash stored in Native C++.
     * Mismatches trigger immediate app termination in Release builds.
     */
    private fun verifyAppSignature() {
        try {
            val currentHash = getComputedSignatureHash()
            val expectedSignature = getAppSignature() // From JNI

import com.vaultguard.app.BuildConfig

// ... inside verifyAppSignature
            if (currentHash != expectedSignature) {
                if (!BuildConfig.DEBUG) {
                    Log.e(TAG, "FATAL: Signature Mismatch! App may be tampered.")
                    deleteKey() // Nuke data
                    exitApp()
                } else {
                    Log.w(TAG, "Signature Mismatch detected but ignored in DEBUG mode.")
                }
            }
        } catch (e: Exception) {
             val isDebuggable = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
             if (!isDebuggable) throw SecurityException("Signature verification failed", e)
        }
    }

    /**
     * Computes SHA-256 hash of the current APK signature.
     */
    private fun getComputedSignatureHash(): String {
        val pm = context.packageManager
        val packageName = context.packageName
        
        // Handle different API levels for signature retrieval
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            android.content.pm.PackageManager.GET_SIGNATURES
        }
        
        val packageInfo = pm.getPackageInfo(packageName, flags)
        
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo.apkContentsSigners
        } else {
            packageInfo.signatures
        }
        
        if (signatures == null || signatures.isEmpty()) {
            throw SecurityException("No signing certificates found.")
        }

        val md = MessageDigest.getInstance("SHA-256")
        val signatureBytes = signatures[0].toByteArray()
        val digest = md.digest(signatureBytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Aggregates multiple security checks:
     * 1. Native Frida Scan
     * 2. Root Access (SU Binaries)
     * 3. Custom ROM Indicators (Test Keys)
     * 4. Debugger Connection
     * 5. Java-based Hooking Checks (Stacktrace analysis)
     * 6. Emulator Detection
     */
    private fun isDeviceSecure(): Boolean {
        // 1. NATIVE FRIDA PROTECTION (First Line of Defense - Fastest)
        if (checkFrida()) {
             Log.e(TAG, "Frida/Hooking Detected via Native Scan!")
             return false
        }

        // 2. Debugging Check
        if (android.os.Debug.isDebuggerConnected()) return false

        // 3. Root Detection (Common Paths)
        val suPaths = arrayOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
            "/system/bin/failsafe/su", "/data/local/su"
        )
        if (suPaths.any { java.io.File(it).exists() }) return false

        // 4. Test Keys (Custom ROMs)
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) return false

        // 5. Java-based Hooking Detection (Stack Inspection)
        try {
            throw Exception("StackCheck")
        } catch (e: Exception) {
            for (element in e.stackTrace) {
                if (element.className.contains("de.robv.android.xposed") || 
                    element.className.contains("com.saurik.substrate") ||
                    element.className.contains("frida")) {
                    return false
                }
            }
        }

        // 6. Emulator Detection (Heuristic)
        val isEmulator = (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk")
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

    /**
     * Encrypts (Wraps) the provided Master SecretKey using the hardware-backed keystore key.
     * The encrypted blob and IV are stored in SharedPreferences.
     */
    fun saveMasterKey(secretKey: SecretKey) {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            val wrappingKey = keyStore.getKey(WRAP_KEY_ALIAS, null) as SecretKey

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.WRAP_MODE, wrappingKey)
            
            val wrappedKeyBytes = cipher.wrap(secretKey)
            val iv = cipher.iv

            prefs.edit()
                .putString(KEY_WRAPPED_BLOB, Base64.encodeToString(wrappedKeyBytes, Base64.NO_WRAP))
                .putString(KEY_WRAPPED_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply()
        } catch (e: Exception) {
            throw SecurityException("Failed to save master key", e)
        }
    }

    /**
     * Loads and Decrypts (Unwraps) the Master SecretKey from storage.
     * Throws SecurityException if the key cannot be recovered.
     */
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

    /**
     * Irreversibly destroys the Master Key data.
     * This renders all stored secrets unreadable.
     */
    fun deleteKey() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            // Optional: We could delete WRAP_KEY_ALIAS too, but clearing the blob is sufficient
            // and allows the device key to be reused for a new wallet.
            // For a paranoid wipe, we delete everything.
            if (keyStore.containsAlias(WRAP_KEY_ALIAS)) {
                keyStore.deleteEntry(WRAP_KEY_ALIAS)
            }
            prefs.edit().remove(KEY_WRAPPED_BLOB).remove(KEY_WRAPPED_IV).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting keys", e)
        }
    }

    private fun exitApp() {
        exitProcess(0)
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
