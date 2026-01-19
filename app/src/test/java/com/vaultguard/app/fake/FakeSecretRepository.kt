package com.vaultguard.app.fake

import com.vaultguard.app.data.remote.dto.SecretResponse
import com.vaultguard.app.domain.repository.SecretRepository

class FakeSecretRepository : SecretRepository {

    private val secrets = mutableListOf<SecretResponse>()
    private val failureMode = false

    override suspend fun saveSecret(
            id: String,
            ownerHash: String,
            titleHash: String,
            encryptedBlob: String,
            iv: String
    ): Result<Unit> {
        if (failureMode) return Result.failure(Exception("Network Error"))

        val newSecret = SecretResponse(id, ownerHash, titleHash, encryptedBlob, iv)
        secrets.add(newSecret)
        return Result.success(Unit)
    }

    override suspend fun fetchSecrets(ownerHash: String): Result<List<SecretResponse>> {
        if (failureMode) return Result.failure(Exception("Network Error"))

        val filtered = secrets.filter { it.ownerHash == ownerHash }
        return Result.success(filtered)
    }

    override suspend fun deleteSecret(id: String, ownerHash: String): Result<Unit> {
        if (failureMode) return Result.failure(Exception("Network Error"))

        secrets.removeAll { it.id == id && it.ownerHash == ownerHash }
        return Result.success(Unit)
    }

    override suspend fun wipeVault(token: String): Result<Unit> {
        secrets.clear()
        return Result.success(Unit)
    }
}
