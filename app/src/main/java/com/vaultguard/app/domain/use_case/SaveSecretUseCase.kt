package com.vaultguard.app.domain.use_case

import com.vaultguard.app.domain.repository.SecretRepository
import com.vaultguard.app.security.SecurityManager
import com.vaultguard.app.security.SecurityUtils
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject

class SaveSecretUseCase
@Inject
constructor(
        private val repository: SecretRepository,
        private val securityManager: SecurityManager
) {
    suspend operator fun invoke(
            title: String,
            username: String,
            secret: String,
            ownerHash: String
    ): Result<Unit> {
        return try {
            val masterKey = securityManager.loadMasterKey()

            val id = UUID.randomUUID().toString()
            val payload = "$title|$username|$secret"
            val encryptResult =
                    securityManager.encrypt(payload.toByteArray(StandardCharsets.UTF_8), masterKey)

            val iv = encryptResult.first.joinToString("") { "%02x".format(it) }
            val encryptedBlob = encryptResult.second.joinToString("") { "%02x".format(it) }

            val titleHash = SecurityUtils.sha256(title)

            repository.saveSecret(id, ownerHash, titleHash, encryptedBlob, iv)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
