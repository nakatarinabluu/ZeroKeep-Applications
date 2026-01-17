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

import android.content.SharedPreferences

@HiltViewModel
class SecretViewModel @Inject constructor(
    private val repository: SecretRepository,
    private val securityManager: SecurityManager,
    private val prefs: SharedPreferences
) : ViewModel() {

    private val _isBiometricsEnabled = MutableStateFlow(prefs.getBoolean("biometrics_enabled", false))
    val isBiometricsEnabled: StateFlow<Boolean> = _isBiometricsEnabled

    fun setBiometricEnabled(enabled: Boolean) {
        _isBiometricsEnabled.value = enabled
        prefs.edit().putBoolean("biometrics_enabled", enabled).apply()
    }


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
                            
                            // Parse "title|username|password" format (New) OR "username|password" (Old)
                            val parts = decryptedString.split('|', limit = 3)
                            
                            val title: String
                            val username: String
                            val password: String

                            // Check format by size
                            if (parts.size == 3) {
                                // New Format: title|username|password
                                title = parts[0]
                                username = parts[1]
                                password = parts[2]
                            } else if (parts.size == 2) {
                                // Old Format: username|password
                                title = "Secret ${secret.id.take(4)}" // Fallback Title
                                username = parts[0]
                                password = parts[1]
                            } else {
                                // Fallback/Error
                                title = "Unknown Secret"
                                username = ""
                                password = parts[0]
                            }
                            
                            SecretUiModel(
                                id = secret.id,
                                title = title,
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
                // Format: "title|username|password"
                val payload = "$title|$username|$secret" 
                val encryptResult = securityManager.encrypt(payload.toByteArray(java.nio.charset.StandardCharsets.UTF_8), masterKey)
                val ivBytes = encryptResult.first
                val encryptedBytes = encryptResult.second
                
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

    fun deleteSecret(id: String) {
        viewModelScope.launch {
            try {
                // Optimistic update or waiting? Let's wait.
                val appOwnerUsername = "default_user"
                val ownerHash = sha256(appOwnerUsername)
                
                val result = repository.deleteSecret(id, ownerHash) // Requires ownerHash
                
                if (result.isSuccess) {
                    loadSecrets() // Refresh list
                } else {
                     android.util.Log.e("SecretViewModel", "Delete failed")
                }
            } catch (e: Exception) {
                 android.util.Log.e("SecretViewModel", "Delete error", e)
            }
        }
    }

    fun clearSensitiveData() {
        _secrets.value = emptyList()
        _saveState.value = null
    }

    fun wipeVault(token: String) {
        // In real app, verify token or just wipe.
        // DANGER: Deletes Key + Encrypted Data
        viewModelScope.launch {
            try {
                // Delete KeyStore Entry
                securityManager.deleteKey()
                
                // Clear Preferences (Token, Biometrics)
                setBiometricEnabled(false)
                
                // Note: Clearing App Data via Code is tricky on Android (requires root or specific System APIs).
                // Best we can do is clear our DB/Prefs/Keys.
                
                // Kill process to ensure RAM dump
                // System.exit(0) handled by caller/signout?
            } catch (e: Exception) {
                // Log but ensure we try to delete key
            }
        }
    }

    // ... wipeVault ...
    // --- Helpers ---
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

data class SecretUiModel(
    val id: String,
    val title: String,
    val username: String,
    val password: String
)
