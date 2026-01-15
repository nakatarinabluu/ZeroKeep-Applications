package com.vaultguard.app.ui.secret

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultguard.app.domain.repository.SecretRepository
import com.vaultguard.app.security.SecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SecretViewModel @Inject constructor(
    private val repository: SecretRepository,
    private val securityManager: SecurityManager
) : ViewModel() {

    private val _saveState = MutableStateFlow<Result<Unit>?>(null)
    val saveState: StateFlow<Result<Unit>?> = _saveState

    private val _secrets = MutableStateFlow<List<SecretUiModel>>(emptyList())
    val secrets: StateFlow<List<SecretUiModel>> = _secrets

    fun loadSecrets() {
        viewModelScope.launch {
            // "default_user" matches AddSecretScreen. In real app, get from User Session.
            val username = "default_user" 
            val ownerHash = sha256(username)
            
            try {
                // LOAD MASTER KEY (Requires Auth)
                if (!securityManager.hasMasterKey()) {
                     // Should navigate to Setup? Or handle empty state.
                     _secrets.value = emptyList()
                     return@launch
                }
                
                val masterKey = securityManager.loadMasterKey()
                
                val result = repository.fetchSecrets(ownerHash)
                result.onSuccess { responseList ->
                    val decryptedList = responseList.mapNotNull { secret ->
                        try {
                            val iv = hexStringToByteArray(secret.iv)
                            val encrypted = hexStringToByteArray(secret.encryptedBlob)
                            
                            // Decrypt using loaded key
                            val decryptedBytes = securityManager.decrypt(iv, encrypted, masterKey)
                            val decryptedPassword = String(decryptedBytes, java.nio.charset.StandardCharsets.UTF_8)
                            
                            SecretUiModel(
                                id = secret.id,
                                title = "Secret ${secret.id.take(4)}", 
                                password = decryptedPassword
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("SecretViewModel", "Decryption failed for ${secret.id}", e)
                            null
                        }
                    }
                    _secrets.value = decryptedList
                }
            } catch (e: Exception) {
                 // Auth failed or Key missing
                 android.util.Log.e("SecretViewModel", "Failed to load secrets (Auth/Key)", e)
            }
        }
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
    
    private fun sha256(input: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun saveSecret(title: String, username: String, secret: String) {
        viewModelScope.launch {
            try {
                val masterKey = securityManager.loadMasterKey() // Requires Auth
                
                val id = UUID.randomUUID().toString()
                val (ivBytes, encryptedBytes) = securityManager.encrypt(secret.toByteArray(java.nio.charset.StandardCharsets.UTF_8), masterKey)
                
                val iv = ivBytes.joinToString("") { "%02x".format(it) }
                val encryptedBlob = encryptedBytes.joinToString("") { "%02x".format(it) }
                
                // FIX: ownerHash must be consistent for the App User, NOT the secret's username.
                // Since we don't have multi-user login yet, we use the same "default_user" as loadSecrets.
                val appOwnerUsername = "default_user" 
                val ownerHash = sha256(appOwnerUsername)
                val titleHash = sha256(title)

                val result = repository.saveSecret(id, ownerHash, titleHash, encryptedBlob, iv)
                _saveState.value = result
                // Refresh list
                if (result.isSuccess) loadSecrets()
            } catch (e: Exception) {
                android.util.Log.e("SecretViewModel", "Error saving secret", e)
                _saveState.value = Result.failure(e)
            }
        }
    }
    fun wipeVault(token: String) {
        viewModelScope.launch {
            try {
                val result = repository.wipeVault(token)
                // If successful, local data should also be cleared or app reset
                if (result.isSuccess) {
                    securityManager.deleteKey() // Self-Destruct Local Key
                    _secrets.value = emptyList() // Clear UI
                }
                // We might want to expose a separate state for wipe result
            } catch (e: Exception) {
                // Log error
            }
        }
    }
}

data class SecretUiModel(
    val id: String,
    val title: String,
    val password: String
)
