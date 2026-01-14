package com.vaultguard.app.data.repository

import com.vaultguard.app.data.remote.VaultApi
import com.vaultguard.app.data.remote.dto.SaveSecretRequest
import com.vaultguard.app.data.remote.dto.SecretResponse
import com.vaultguard.app.domain.repository.SecretRepository
import java.io.IOException
import javax.inject.Inject

class SecretRepositoryImpl @Inject constructor(
    private val api: VaultApi
) : SecretRepository {

    override suspend fun saveSecret(
        id: String,
        ownerHash: String,
        titleHash: String,
        encryptedBlob: String,
        iv: String
    ): Result<Unit> {
        return try {
            val response = api.saveSecret(
                SaveSecretRequest(id, ownerHash, titleHash, encryptedBlob, iv)
            )
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("SecretRepo", "Save failed: ${response.code()} Body: $errorBody")
                Result.failure(IOException("Error: ${response.code()} $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchSecrets(ownerHash: String): Result<List<SecretResponse>> {
        return try {
            val response = api.fetchSecrets(ownerHash)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(IOException("Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteSecret(id: String): Result<Unit> {
        return try {
            val response = api.deleteSecret(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun wipeVault(token: String): Result<Unit> {
        return try {
            val response = api.wipeVault(token)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
