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
    private val KEY_ALIAS = "VaultGuardMasterKey"
    private val TRANSFORMATION = "AES/GCM/NoPadding"

    init {
        if (!isDeviceSecure()) {
            throw SecurityException("Device is compromised (Rooted/Hooked/Debugged)")
        }
        generateKeyIfNotExists()
    }

    /**
     * SELF-DESTRUCT: Permanently removes the master key from the Keystore.
     * This renders all stored data undecryptable.
     */
    fun deleteKey() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
        }
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

    private fun generateKeyIfNotExists() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )

            // StrongBox Check: Prefer Trusted Execution Environment (TEE) / StrongBox
            val hasStrongBox = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
            } else {
                false
            }

            val keyGenParameterSpecBuilder = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                // Require user authentication (Biometrics/PIN) within the last 30 seconds to use this key
                .setUserAuthenticationRequired(true)
                .setUnlockedDeviceRequired(true)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                 // Secure Hardware requirement
                 keyGenParameterSpecBuilder.setUserAuthenticationParameters(
                     30, 
                     KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
                 )
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

    fun encrypt(data: ByteArray): Pair<ByteArray, ByteArray> {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data)

        return Pair(iv, encryptedData)
    }

    fun decrypt(iv: ByteArray, encryptedData: ByteArray): ByteArray {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey

        val spec = GCMParameterSpec(128, iv)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        return cipher.doFinal(encryptedData)
    }
}
