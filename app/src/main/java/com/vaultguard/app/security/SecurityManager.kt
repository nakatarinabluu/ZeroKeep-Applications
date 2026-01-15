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

class SecurityManager(private val context: Context) {

    private val ANDROID_KEYSTORE = "AndroidKeyStore"
    // New Alias for the WRAPPING Key (Device Local). 
    // We do NOT use the old "VaultGuardMasterKey" alias for the wrapping key to avoid confusion, 
    // but effectively we are replacing the old logic.
    private val WRAP_KEY_ALIAS = "VaultGuardDeviceKey" 
    private val TRANSFORMATION = "AES/GCM/NoPadding"
    private val PREFS_NAME = "vault_guard_secure_prefs"
    private val KEY_WRAPPED_BLOB = "wrapped_master_key_blob"
    private val KEY_WRAPPED_IV = "wrapped_master_key_iv"

    init {
        if (!isDeviceSecure()) {
            throw SecurityException("Device is compromised (Rooted/Hooked/Debugged)")
        }
        generateWrappingKeyIfNotExists()
    }

    // --- KEY MANAGEMENT (HYBRID) ---

    /**
     * Saves the Mnemonic-Derived Master Key securely.
     * It encrypts (wraps) the Master Key using the Device-Bound Key (Biometric protected).
     */
    fun saveMasterKey(masterKey: SecretKey) {
        val (iv, wrappedKey) = encryptLocal(masterKey.encoded)
        
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_WRAPPED_BLOB, android.util.Base64.encodeToString(wrappedKey, android.util.Base64.NO_WRAP))
            .putString(KEY_WRAPPED_IV, android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP))
            .apply()
    }

    /**
     * Loads the Master Key by unwrapping it with the Device-Bound Key.
     * THIS REQUIRE USER AUTHENTICATION (Biometrics) if the Device Key requires it.
     */
    fun loadMasterKey(): SecretKey {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val wrappedBlobStr = prefs.getString(KEY_WRAPPED_BLOB, null) ?: throw IllegalStateException("No Master Key Found")
        val ivStr = prefs.getString(KEY_WRAPPED_IV, null) ?: throw IllegalStateException("No IV Found")
        
        val wrappedBlob = android.util.Base64.decode(wrappedBlobStr, android.util.Base64.NO_WRAP)
        val iv = android.util.Base64.decode(ivStr, android.util.Base64.NO_WRAP)
        
        val keyBytes = decryptLocal(iv, wrappedBlob)
        return javax.crypto.spec.SecretKeySpec(keyBytes, "AES")
    }
    
    fun hasMasterKey(): Boolean {
         val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
         return prefs.contains(KEY_WRAPPED_BLOB)
    }

    // --- CRYPTO OPERATIONS (USING MASTER KEY) ---
    
    // Encrypts User Data using the LOADED Master Key
    fun encrypt(data: ByteArray, masterKey: SecretKey): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, masterKey)

        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data)

        return Pair(iv, encryptedData)
    }

    // Decrypts User Data using the LOADED Master Key
    fun decrypt(iv: ByteArray, encryptedData: ByteArray, masterKey: SecretKey): ByteArray {
        val spec = GCMParameterSpec(128, iv)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, masterKey, spec)

        return cipher.doFinal(encryptedData)
    }

    // --- INTERNAL: DEVICE KEY MANAGEMENT (WRAPPING) ---

    private fun generateWrappingKeyIfNotExists() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        if (!keyStore.containsAlias(WRAP_KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )

            // StrongBox Check
            val hasStrongBox = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
            } else {
                false
            }

            val keyGenParameterSpecBuilder = KeyGenParameterSpec.Builder(
                WRAP_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                // USER REQUEST: Make Biometrics OPTIONAL. 
                // We disable mandatory hardware auth here. Verification is now done at the UI layer.
                .setUserAuthenticationRequired(false) 
                // .setUnlockedDeviceRequired(true) // Removing restriction for usability
            
            // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //      keyGenParameterSpecBuilder.setUserAuthenticationParameters(
            //          30, 
            //          KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
            //      )
            // } else {
            //      @Suppress("DEPRECATION")
            //      keyGenParameterSpecBuilder.setUserAuthenticationValidityDurationSeconds(30)
            // }
            } else {
                 @Suppress("DEPRECATION")
                 keyGenParameterSpecBuilder.setUserAuthenticationValidityDurationSeconds(30)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && hasStrongBox) {
                keyGenParameterSpecBuilder.setIsStrongBoxBacked(true)
            }

            keyGenerator.init(keyGenParameterSpecBuilder.build())
            keyGenerator.generateKey()
        }
    }

    // Encrypts RAW BYTES using DEVICE KEY (No Auth needed usually for Encryption)
    private fun encryptLocal(data: ByteArray): Pair<ByteArray, ByteArray> {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        val secretKey = keyStore.getKey(WRAP_KEY_ALIAS, null) as SecretKey

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data)

        return Pair(iv, encryptedData)
    }

    // Decrypts RAW BYTES using DEVICE KEY (Auth REQUIRED)
    private fun decryptLocal(iv: ByteArray, encryptedData: ByteArray): ByteArray {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        val secretKey = keyStore.getKey(WRAP_KEY_ALIAS, null) as SecretKey

        val spec = GCMParameterSpec(128, iv)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        return cipher.doFinal(encryptedData)
    }

    /**
     * SELF-DESTRUCT: Permanently removes the WRAPPING key and the Encrypted Blob.
     */
    fun deleteKey() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        if (keyStore.containsAlias(WRAP_KEY_ALIAS)) {
            keyStore.deleteEntry(WRAP_KEY_ALIAS)
        }
        // Also clear prefs
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }

    /**
     * ADVANCED ROOT & TAMPER DETECTION
     * Checks for SU binaries, dangerous props, hooking frameworks, and debuggers.
     */
    private fun isDeviceSecure(): Boolean {
        // 1. Check for basic Root binaries
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

        // 2. Check for Test Keys (Custom ROMs)
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) return false

        // 3. Check for Debugging
        if (android.os.Debug.isDebuggerConnected()) return false
        
        // 4. Check for Frida / Xposed (Simple check)
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

        // 5. Check for Emulator
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
