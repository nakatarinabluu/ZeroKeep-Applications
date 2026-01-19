package com.vaultguard.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultguard.app.domain.repository.SecretRepository
import com.vaultguard.app.security.KdfGenerator
import com.vaultguard.app.security.SecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.crypto.SecretKey
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SetupViewModel
@Inject
constructor(
        private val repository: SecretRepository,
        private val securityManager: SecurityManager,
        private val kdfGenerator: KdfGenerator,
        private val prefs: android.content.SharedPreferences
) : ViewModel() {

    private val _setupState = MutableStateFlow<SetupState>(SetupState.Idle)
    val setupState: StateFlow<SetupState> = _setupState

    fun completeSetup(password: String, mnemonic: List<String>, isRestore: Boolean) {
        viewModelScope.launch {
            android.util.Log.d("ZeroKeepOps", "🔐 Setup: Starting. Restoring: $isRestore")
            _setupState.value = SetupState.Loading
            try {
                // 1. Derive Critical Key (Argon2id)
                android.util.Log.d("ZeroKeepOps", "🔐 Setup: Deriving Key from Mnemonic...")
                val mnemonicString = mnemonic.joinToString(" ")

                // Deterministic Salt Generation (Essential for Restore)
                // We hash (Mnemonic + "mnemonic" + Password) to create a consistent salt
                // This replaces the PBKDF2 "mnemonic" + password salt logic but adapts it for
                // Argon2id fixed length
                val saltSource = "mnemonic" + password
                val deterministicSalt =
                        java.security.MessageDigest.getInstance("SHA-256")
                                .digest(saltSource.toByteArray(Charsets.UTF_8))
                                .take(16)
                                .toByteArray() // Use first 16 bytes as salt

                val kdfResult = kdfGenerator.deriveKey(mnemonicString, deterministicSalt)
                val keyBytes = kdfResult.first

                // val salt = kdfResult.second // Unused in this logic as we use deterministic salt
                val derivedKey = javax.crypto.spec.SecretKeySpec(keyBytes, "AES")

                // 2. Strict Verification (If Restoring)
                val vaultId =
                        com.vaultguard.app.security.SecurityUtils.sha256(
                                mnemonicString
                        ) // Compute early
                if (isRestore) {
                    if (!verifyKeyAgainstServer(derivedKey, vaultId)) {
                        _setupState.value =
                                SetupState.Error(
                                        "Incorrect Password or Recovery Phrase. Unable to unlock vault."
                                )
                        return@launch
                    }
                }

                // 3. Save Key Securely
                securityManager.saveMasterKey(derivedKey)
                android.util.Log.d("ZeroKeepOps", "✅ Setup: Master Key saved securely to Keystore.")

                // 4. Save Vault Identity (Multi-Account Support) & Auth Hash
                val passwordHash = kdfGenerator.hashPassword(password)
                android.util.Log.d(
                        "ZeroKeepOps",
                        "📝 Setup: Persisting Vault ID and Preferences..."
                )
                prefs.edit()
                        .putBoolean("biometrics_enabled", false)
                        .putString("vault_id", vaultId)
                        .putString("master_password_hash", passwordHash)
                        .putBoolean("is_setup_complete", true)
                        .remove("master_password") // Cleanup legacy
                        .remove("duress_password")
                        .apply()

                _setupState.value = SetupState.Success(password)
                android.util.Log.d("SetupViewModel", "Setup Complete! Emitting Success state.")
            } catch (e: Exception) {
                _setupState.value = SetupState.Error("Setup Failed: ${e.message}")
            }
        }
    }

    /** Tries to fetch and decrypt 1 item from the server to prove the key is correct. */
    private suspend fun verifyKeyAgainstServer(key: SecretKey, vaultId: String): Boolean {
        try {
            val ownerHash = vaultId

            val result = repository.fetchSecrets(ownerHash)
            return result.fold(
                    onSuccess = { secrets ->
                        if (secrets.isEmpty())
                                return true // No data = valid key (nothing to check against)

                        // Try decrypting the first one
                        val sample = secrets.first()
                        try {
                            val iv =
                                    com.vaultguard.app.security.SecurityUtils.hexStringToByteArray(
                                            sample.iv
                                    )
                            val encrypted =
                                    com.vaultguard.app.security.SecurityUtils.hexStringToByteArray(
                                            sample.encryptedBlob
                                    )

                            // If this throws or returns garbage, decryption failed.
                            securityManager.decrypt(iv, encrypted, key)
                            return true
                        } catch (e: Exception) {
                            return false // Decryption failed
                        }
                    },
                    onFailure = {
                        // Network error or other issue
                        // strict mode implies we must verify.
                        return false
                    }
            )
        } catch (e: Exception) {
            return false
        }
    }

    // --- Helper Methods for UI ---
    fun createVault(password: String, mnemonic: List<String>) {
        completeSetup(password, mnemonic, isRestore = false)
    }

    fun restoreVault(password: String, mnemonic: List<String>) {
        completeSetup(password, mnemonic, isRestore = true)
    }

    fun resetState() {
        _setupState.value = SetupState.Idle
    }

    /**
     * Clears the 'is_setup_complete' flag to prevent being locked out if the user exits the app
     * during the reset/setup process.
     */
    fun prepareNewSetup() {
        prefs.edit().putBoolean("is_setup_complete", false).apply()
        resetState()
    }

    // --- Utils ---
    // Moved to SecurityUtils to prevent duplication.
}

sealed class SetupState {
    object Idle : SetupState()
    object Loading : SetupState()
    data class Success(val password: String) : SetupState()
    data class Error(val message: String) : SetupState()
}
