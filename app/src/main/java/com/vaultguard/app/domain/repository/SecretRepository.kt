package com.vaultguard.app.domain.repository

import com.vaultguard.app.data.remote.dto.SecretResponse

interface SecretRepository {
    suspend fun saveSecret(id: String, ownerHash: String, titleHash: String, encryptedBlob: String, iv: String): Result<Unit>
    suspend fun fetchSecrets(ownerHash: String): Result<List<SecretResponse>>
    suspend fun deleteSecret(id: String): Result<Unit>
    suspend fun wipeVault(token: String): Result<Unit>
}
