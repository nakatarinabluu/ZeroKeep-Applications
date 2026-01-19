package com.vaultguard.app.utils

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricHelper(private val activity: FragmentActivity) {

    fun canAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(activity)
        return when (biometricManager.canAuthenticate(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG
                )
        ) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    fun showBiometricPrompt(
            title: String = "Identity Verification",
            subtitle: String = "Confirm your identity to access Vault",
            onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
            onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback =
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        // "User cancelled" is common, treat as error or ignore? Treat as error msg.
                        onError(errString.toString())
                    }

                    override fun onAuthenticationSucceeded(
                            result: BiometricPrompt.AuthenticationResult
                    ) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess(result)
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        // Soft failure (wrong finger), prompt stays open usually.
                        // We could log this?
                    }
                }

        val promptInfo =
                BiometricPrompt.PromptInfo.Builder()
                        .setTitle(title)
                        .setSubtitle(subtitle)
                        .setNegativeButtonText("Use Password")
                        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                        .build()

        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        biometricPrompt.authenticate(promptInfo)
    }
}
