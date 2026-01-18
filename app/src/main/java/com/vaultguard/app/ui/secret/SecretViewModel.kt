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
            // Dynamic Vault Identity
            val ownerHash = prefs.getString("vault_id", null) ?: run {
                // Fallback for legacy "default_user" or just fail?
                // For migration: Check if "vault_id" missing, maybe use sha256("default_user")?
                // Better: sha256("default_user") so existing installs don't break immediately.
                sha256("default_user") 
            }
            
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
                    // Auto-Migration: If current vault is empty, check legacy "default_user" vault
                    if (responseList.isEmpty()) {
                        val legacyHash = sha256("default_user")
                        // Only check if we are NOT already in the legacy vault
                        if (ownerHash != legacyHash) {
                             val legacyResult = repository.fetchSecrets(legacyHash)
                             legacyResult.onSuccess { legacyList ->
                                 if (legacyList.isNotEmpty()) {
                                     android.util.Log.i("SecretViewModel", "Found ${legacyList.size} legacy items. Attempting migration...")
                                     migrateLegacyData(legacyList, masterKey, ownerHash)
                                     return@onSuccess // migrateLegacyData will reload
                                 }
                             }
                        }
                    }

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
                            .also {
                                android.util.Log.d("SecretViewModel", "Decryption success for: ${it.title} (${it.id})")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("SecretViewModel", "Decryption failed for ${secret.id}", e)
                            null
                        }
                    }
                    _secrets.value = decryptedList
                }
                result.onFailure { e ->
                    android.util.Log.e("SecretViewModel", "Fetch failed", e)
                    // TODO: Expose error to UI
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
                
                // FIX: ownerHash must be consistent for the App User
                val ownerHash = prefs.getString("vault_id", null) ?: sha256("default_user")
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
                val ownerHash = prefs.getString("vault_id", "default_vault")!!
                
                val result = repository.deleteSecret(id, ownerHash) // Requires ownerHash
                
                if (result.isSuccess) {
                    loadSecrets() // Refresh list
                } else {
                     android.util.Log.e("SecretViewModel", "Delete failed", result.exceptionOrNull())
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
                // 1. Wipe Cloud Data First
                val result = repository.wipeVault(token)
                if (result.isFailure) {
                    android.util.Log.e("SecretViewModel", "Server Wipe Failed", result.exceptionOrNull())
                    // Continue to local wipe anyway? Yes, user wants destruction.
                }

                // 2. Delete KeyStore Entry
                securityManager.deleteKey()
                
                // 3. Clear Preferences (Token, Biometrics)
                setBiometricEnabled(false)
                
                // 4. Clear In-Memory Data
                _secrets.value = emptyList()

                // Note: Clearing App Data via Code is tricky on Android.
            } catch (e: Exception) {
                android.util.Log.e("SecretViewModel", "Wipe Error", e)
                // Ensure we try to delete key last resort
                securityManager.deleteKey()
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

    private fun migrateLegacyData(legacyList: List<com.vaultguard.app.domain.model.Secret>, masterKey: javax.crypto.SecretKey, newOwnerHash: String) {
        viewModelScope.launch {
            var migratedCount = 0
            legacyList.forEach { secret ->
                try {
                    val iv = hexStringToByteArray(secret.iv)
                    val encrypted = hexStringToByteArray(secret.encryptedBlob)
                    
                    // 1. Try Decrypt
                    val decryptedBytes = securityManager.decrypt(iv, encrypted, masterKey)
                    
                    // 2. If successful, Re-Encrypt for New Vault
                    // We can just re-save the SAME payload (iv/blob) if we trust it, 
                    // BUT the server might enforce owner ownership on specific IDs?
                    // Usually safer to create NEW ID or just re-save with new OwnerHash.
                    // Let's re-save with SAME ID but NEW OwnerHash.
                    
                    // BUT wait, Repository.saveSecret takes ID.
                    // The server handles UPSERT.
                    // We should probably keep the ID to avoid duplicates if migration runs partial?
                    // Or keep it simple.
                    
                    val result = repository.saveSecret(secret.id, newOwnerHash, secret.titleHash, secret.encryptedBlob, secret.iv)
                    if (result.isSuccess) migratedCount++
                    
                } catch (e: Exception) {
                    // Decryption failed = Key doesn't match = Not this user's data. Ignore.
                }
            }
            if (migratedCount > 0) {
                android.util.Log.i("SecretViewModel", "Migrated $migratedCount secrets successfully.")
                loadSecrets() // Reload to show migrated data
            }
        }
    }
}

data class SecretUiModel(
    val id: String,
    val title: String,
    val username: String,
    val password: String
)
