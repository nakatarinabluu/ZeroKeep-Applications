package com.vaultguard.app.domain.use_case

import com.vaultguard.app.domain.model.Secret
import com.vaultguard.app.domain.repository.SecretRepository
import com.vaultguard.app.security.SecurityManager
import com.vaultguard.app.security.SecurityUtils
import java.nio.charset.StandardCharsets
import javax.inject.Inject

class GetSecretsUseCase
@Inject
constructor(
        private val repository: SecretRepository,
        private val securityManager: SecurityManager
) {
    suspend operator fun invoke(ownerHash: String): Result<List<Secret>> {
        return try {
            if (!securityManager.hasMasterKey()) {
                return Result.failure(Exception("Vault Locked or Key Missing"))
            }

            val masterKey = securityManager.loadMasterKey()
            val result = repository.fetchSecrets(ownerHash)

            if (result.isFailure) {
                return Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }

            val responseList = result.getOrNull() ?: emptyList()

            // Note: Migration logic could be a separate UseCase or kept here if simple.
            // For now, we focus on fetching and decrypting the MAIN vault.
            // Migration is a complex side-effect best handled by a specialized
            // 'MigrateVaultUseCase' or kept in VM for now to avoid over-engineering.
            // I will implement the core decryption logic here.

            val decryptedList =
                    responseList.mapNotNull { secret ->
                        try {
                            val iv = SecurityUtils.hexStringToByteArray(secret.iv)
                            val encrypted = SecurityUtils.hexStringToByteArray(secret.encryptedBlob)

                            val decryptedBytes = securityManager.decrypt(iv, encrypted, masterKey)
                            val decryptedString = String(decryptedBytes, StandardCharsets.UTF_8)

                            val parts = decryptedString.split('|', limit = 3)

                            val title: String
                            val username: String
                            val password: String

                            if (parts.size == 3) {
                                title = parts[0]
                                username = parts[1]
                                password = parts[2]
                            } else if (parts.size == 2) {
                                title = "Secret ${secret.id.take(4)}"
                                username = parts[0]
                                password = parts[1]
                            } else {
                                title = "Unknown Secret"
                                username = ""
                                password = parts[0]
                            }

                            Secret(
                                    id = secret.id,
                                    title = title,
                                    username = username,
                                    password = password
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }

            Result.success(decryptedList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
