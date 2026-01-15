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
                            val decryptedString = String(decryptedBytes, java.nio.charset.StandardCharsets.UTF_8)
                            
                            // Parse "username|password" format
                            val parts = decryptedString.split("|", limit = 2)
                            val username = if (parts.size > 1) parts[0] else ""
                            val password = if (parts.size > 1) parts[1] else parts[0] // Fallback if no delimiter
                            
                            SecretUiModel(
                                id = secret.id,
                                title = "Secret ${secret.id.take(4)}", // ideally we hash/decrypt title or use metadata
                                username = username,
                                password = password
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
    
    // ... helper functions ...

    fun saveSecret(title: String, username: String, secret: String) {
        viewModelScope.launch {
            try {
                val masterKey = securityManager.loadMasterKey() // Requires Auth
                
                val id = UUID.randomUUID().toString()
                // Format: "username|password"
                val payload = "$username|$secret" 
                val (ivBytes, encryptedBytes) = securityManager.encrypt(payload.toByteArray(java.nio.charset.StandardCharsets.UTF_8), masterKey)
                
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
    // ... wipeVault ...
}

data class SecretUiModel(
    val id: String,
    val title: String,
    val username: String,
    val password: String
)
