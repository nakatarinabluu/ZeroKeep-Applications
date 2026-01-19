package com.vaultguard.app.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultguard.app.security.SecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel
@Inject
constructor(
        private val securityManager: SecurityManager,
        private val kdfGenerator: com.vaultguard.app.security.KdfGenerator,
        @ApplicationContext private val context: Context,
        private val prefs: android.content.SharedPreferences
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private var attempts = 0
    private val MAX_ATTEMPTS = 7

    fun attemptUnlock(password: String) {
        android.util.Log.d("ZeroKeepOps", "🔓 Auth: Attempting Unlock...")
        val storedHash = prefs.getString("master_password_hash", null)
        val legacyPassword = prefs.getString("master_password", null)

        android.util.Log.d("ZeroKeepOps", "ℹ️ Auth: Stored Hash Exists: ${storedHash != null}")

        val duressPassword = prefs.getString("duress_password", null)

        var isSuccess = false

        if (storedHash != null) {
            // New Secure Flow
            android.util.Log.d("ZeroKeepOps", "🔓 Auth: Verifying Password with Argon2...")
            if (kdfGenerator.verifyPassword(password, storedHash)) {
                android.util.Log.d(
                        "ZeroKeepOps",
                        "✅ Auth: Password Verified! Decrypting Master Key..."
                )
                isSuccess = true
            } else {
                android.util.Log.d("ZeroKeepOps", "❌ Auth: Password Verification Failed.")
            }
        } else if (legacyPassword != null) {
            // Migration Flow (Temporary) - One-time auto-migration could happen here,
            // but for now we just allow unlock if they match, then we should re-save as hash?
            // Safer to just allow unlock and let them re-auth or re-setup.
            // Actually, let's just support it for now to avoid lockout during dev.
            if (password == legacyPassword) {
                isSuccess = true
                // Auto-migrate
                viewModelScope.launch {
                    val newHash = kdfGenerator.hashPassword(password)
                    prefs.edit()
                            .putString("master_password_hash", newHash)
                            .remove("master_password")
                            .apply()
                }
            }
        } else {
            // No password set?
            android.util.Log.e(
                    "AuthViewModel",
                    "CRITICAL: No stored hash or legacy password found, but Setup is complete."
            )
            // Force UI error to show "Incorrect password" (as fallback) or trigger reset
            _authState.value = AuthState.Error(1)
            return
        }

        if (isSuccess) {
            attempts = 0
            _authState.value = AuthState.Success
        } else if (duressPassword != null && password == duressPassword) {
            // DURESS MODE TRIGGERED: SILENT DESTRUCTION
            // 1. Delete Crypto Key (Data becomes permanently unreadable)
            securityManager.deleteKey()

            // 2. Do NOT clear prefs or crash.
            // We want the app to look NORMAL.
            // Navigate to Dashboard.

            // 3. Set Success (Unlock)
            // The Dashboard will try to load secrets, fail to decrypt (no key), and show empty
            // list.
            _authState.value = AuthState.Success
        } else {
            attempts++
            if (attempts >= MAX_ATTEMPTS) {
                securityManager.deleteKey()
                _authState.value = AuthState.Wiped("Self-Destruct Triggered: Master Key Deleted.")
            } else {
                android.util.Log.d(
                        "ZeroKeepOps",
                        "⚠️ Auth: Incorrect Password. Attempt $attempts/$MAX_ATTEMPTS"
                )
                _authState.value = AuthState.Error(attempts)
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun login(password: String) {
        attemptUnlock(password)
    }

    fun loginWithBiometrics() {
        // Biometric success implies identity verified.
        // We assume Master Key is accessible or we triggered BiometricPrompt with CryptoObject
        // (Advanced).
        // For 'Clean Architecture' starting point, we just unlock UI.
        // To really unlock data, we need the KeyStore unlock which BiometricPrompt handles if
        // configured with CryptoObject.
        // For this task, we assume simple Auth bypass (Auth to UI).
        attempts = 0
        _authState.value = AuthState.Success
    }

    private val _isBiometricEnabled =
            MutableStateFlow(prefs.getBoolean("biometrics_enabled", false))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled

    // Prevent auto-prompt loop on Logout
    var shouldAutoPromptBiometrics = true

    fun disableAutoPrompt() {
        shouldAutoPromptBiometrics = false
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val attemptsUsed: Int) : AuthState()
    data class Wiped(val reason: String) : AuthState()
}
