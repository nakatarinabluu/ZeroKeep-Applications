package com.vaultguard.app.security

import android.content.Context
import android.os.Build
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader

object SecurityUtils {

    /**
     * Comprehensive check for device compromise.
     * Returns true if device is Rooted or is an Emulator.
     */
    fun isDeviceCompromised(context: Context): Boolean {
        // In DEBUG mode, we might want to allow emulators for development
        // But for this security audit, strict rules apply.
        // You can uncomment the check below to allow emulators in debug builds.
        // if (com.vaultguard.app.BuildConfig.DEBUG) return false 

        return isRooted() || isEmulator()
    }

    /**
     * Detects if the device is rooted using multiple heuristics.
     */
    private fun isRooted(): Boolean {
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true
        }

        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )

        for (path in paths) {
            if (File(path).exists()) return true
        }

        return canExecuteCommand("/system/xbin/which su") || canExecuteCommand("/system/bin/which su") || canExecuteCommand("which su")
    }

    private fun canExecuteCommand(command: String): Boolean {
        return try {
            Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val process = Runtime.getRuntime().exec(command)
            val inReader = BufferedReader(InputStreamReader(process.inputStream))
            inReader.readLine() != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Detects if the app is running on an emulator.
     */
    private fun isEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
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
    }
}
