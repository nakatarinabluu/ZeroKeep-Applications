package com.vaultguard.app.domain.use_case

import com.vaultguard.app.domain.repository.SecretRepository
import javax.inject.Inject

class DeleteSecretUseCase @Inject constructor(private val repository: SecretRepository) {
    suspend operator fun invoke(id: String, ownerHash: String): Result<Unit> {
        return repository.deleteSecret(id, ownerHash)
    }
}
