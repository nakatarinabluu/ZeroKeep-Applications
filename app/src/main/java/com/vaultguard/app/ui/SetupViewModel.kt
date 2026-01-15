package com.vaultguard.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultguard.app.domain.repository.SecretRepository
import com.vaultguard.app.security.MnemonicUtils
import com.vaultguard.app.security.SecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.crypto.SecretKey
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val repository: SecretRepository,
    private val securityManager: SecurityManager
) : ViewModel() {

    private val _setupState = MutableStateFlow<SetupState>(SetupState.Idle)
    val setupState: StateFlow<SetupState> = _setupState

    fun finalizeSetup(mnemonic: List<String>, password: String, isRestore: Boolean) {
        viewModelScope.launch {
            _setupState.value = SetupState.Loading
            try {
                // 1. Derive Critical Key
                val derivedKey = MnemonicUtils.deriveKey(mnemonic, password)
                
                // 2. Strict Verification (If Restoring)
                if (isRestore) {
                    if (!verifyKeyAgainstServer(derivedKey)) {
                        _setupState.value = SetupState.Error("Incorrect Password or Recovery Phrase. Unable to unlock vault.")
                        return@launch
                    }
                }
                
                // 3. Save Key Securely
                securityManager.saveMasterKey(derivedKey)
                _setupState.value = SetupState.Success(password)
                
            } catch (e: Exception) {
                _setupState.value = SetupState.Error("Setup Failed: ${e.message}")
            }
        }
    }

    /**
     * Tries to fetch and decrypt 1 item from the server to prove the key is correct.
     */
    private suspend fun verifyKeyAgainstServer(key: SecretKey): Boolean {
        try {
            // HARDCODED USER for now (Matching SecretViewModel)
            val username = "default_user"
            val ownerHash = sha256(username)

            val result = repository.fetchSecrets(ownerHash)
            return result.fold(
                onSuccess = { secrets ->
                    if (secrets.isEmpty()) return true // No data = valid key (nothing to check against)
                    
                    // Try decrypting the first one
                    val sample = secrets.first()
                    try {
                        val iv = hexStringToByteArray(sample.iv)
                        val encrypted = hexStringToByteArray(sample.encryptedBlob)
                        
                        // If this throws or returns garbage, decryption failed.
                        // However, GCM usually throws AEADBadTagException if key is wrong.
                        securityManager.decrypt(iv, encrypted, key)
                        return true
                    } catch (e: Exception) {
                        return false // Decryption failed
                    }
                },
                onFailure = { 
                    // Network error? Assume valid to let user in, OR block?
                    // Safe default: Block if we cant verify? Or Warn?
                    // User said "Cant Login", so strict mode implies we must verify.
                    // But if offline, this bricks the app. 
                    // Let's assume network is required for Restore.
                    return false 
                }
            )
        } catch (e: Exception) {
            return false
        }
    }

    fun resetState() {
        _setupState.value = SetupState.Idle
    }
    
    // --- Utils (Duplicated from SecretViewModel - should actly be in a shared util) ---
    private fun sha256(input: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}

sealed class SetupState {
    object Idle : SetupState()
    object Loading : SetupState()
    data class Success(val password: String) : SetupState()
    data class Error(val message: String) : SetupState()
}
