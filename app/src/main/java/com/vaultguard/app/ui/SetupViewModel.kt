package com.vaultguard.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultguard.app.domain.repository.SecretRepository
import com.vaultguard.app.security.KdfGenerator
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
    private val securityManager: SecurityManager,
    private val kdfGenerator: KdfGenerator,
    private val prefs: android.content.SharedPreferences
) : ViewModel() {

    private val _setupState = MutableStateFlow<SetupState>(SetupState.Idle)
    val setupState: StateFlow<SetupState> = _setupState

    fun completeSetup(password: String, mnemonic: List<String>, isRestore: Boolean) {
        viewModelScope.launch {
            _setupState.value = SetupState.Loading
            try {
                // 1. Derive Critical Key (Argon2id)
                val mnemonicString = mnemonic.joinToString(" ")
                
                // Deterministic Salt Generation (Essential for Restore)
                // We hash (Mnemonic + "mnemonic" + Password) to create a consistent salt
                // This replaces the PBKDF2 "mnemonic" + password salt logic but adapts it for Argon2id fixed length
                val saltSource = "mnemonic" + password
                val deterministicSalt = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(saltSource.toByteArray(Charsets.UTF_8))
                    .take(16).toByteArray() // Use first 16 bytes as salt

                val kdfResult = kdfGenerator.deriveKey(mnemonicString, deterministicSalt)
                val keyBytes = kdfResult.first

                // val salt = kdfResult.second // Unused in this logic as we use deterministic salt
                val derivedKey = javax.crypto.spec.SecretKeySpec(keyBytes, "AES")
                
                // 2. Strict Verification (If Restoring)
                val vaultId = sha256(mnemonicString) // Compute early
                if (isRestore) {
                    if (!verifyKeyAgainstServer(derivedKey, vaultId)) {
                        _setupState.value = SetupState.Error("Incorrect Password or Recovery Phrase. Unable to unlock vault.")
                        return@launch
                    }
                }
                
                // 3. Save Key Securely
                securityManager.saveMasterKey(derivedKey)

                // 4. Save Vault Identity (Multi-Account Support)
                // We derive a unique Vault ID from the mnemonic.
                // This allows different phrases to have different storages (No collision).
                val vaultId = sha256(mnemonicString)
                prefs.edit()
                    .putBoolean("biometrics_enabled", false)
                    .putString("vault_id", vaultId)
                    .apply()
                
                _setupState.value = SetupState.Success(password)
                
            } catch (e: Exception) {
                _setupState.value = SetupState.Error("Setup Failed: ${e.message}")
            }
        }
    }

    /**
     * Tries to fetch and decrypt 1 item from the server to prove the key is correct.
     */
    private suspend fun verifyKeyAgainstServer(key: SecretKey, vaultId: String): Boolean {
        try {
            // Use the Derived Vault ID
            val ownerHash = vaultId // It is already hashed in call site? Or raw? 
            // Wait, call site does `sha256(mnemonicString)`. So it is the hash.
            // Let's verify sha256 function. Yes. the mnemonic string potentially or pass it in. 
            // Since we don't have it here easily without passing, we should assume verifyKey is called in context.
            // Actually, verifyKeysAgainstServer is called with key. We need the Vault ID too.
            // Let's modify the signature or logic?
            // Simpler: Just rely on the fact that if we are restoring, we know the mnemonic.
            // BUT wait, this function is private and only used in one place where we HAVE the mnemonic.
            
             // Temporary Fix: See Note below. 
             // To avoid changing signature significantly in this small edit: 
             // Logic moved to call site? No, let's keep it here but use the passed mnemonic? 
             // AHH, I can't access mnemonic here easily. 
             // I will change the signature of verifyKeyAgainstServer to accept vaultId.
             return false // Placeholder to trigger re-read/edit in "multi_replace" approach
        } catch (e: Exception) {

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
