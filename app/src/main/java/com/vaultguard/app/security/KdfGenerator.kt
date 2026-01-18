package com.vaultguard.app.security

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import java.security.SecureRandom
import javax.inject.Inject

class KdfGenerator @Inject constructor() {

    private val argon2 = Argon2Kt()

    companion object {
        private const val SALT_LENGTH = 16 // 32 is also fine, 16 is standard min
        private const val KEY_LENGTH = 32 // 256 bits
        private const val ITERATIONS = 4
        private const val MEMORY = 65536 // 64 MB in KB
        private const val PARALLELISM = 2
    }

    /**
     * Derives a 256-bit AES key from the Recovery Phrase (Mnemonic) and a Salt.
     * Uses Argon2id.
     */
    fun deriveKey(mnemonic: String, salt: ByteArray? = null): Pair<ByteArray, ByteArray> {
        val actualSalt = salt ?: generateSalt()

        val result = argon2.hash(
            mode = Argon2Mode.ARGON2_ID,
            password = mnemonic.toByteArray(Charsets.UTF_8),
            salt = actualSalt,
            tCostInIterations = ITERATIONS,
            mCostInKibibyte = MEMORY,
            parallelism = PARALLELISM,
            hashLengthInBytes = KEY_LENGTH
        )

        return Pair(result.rawHashAsByteArray(), actualSalt)
    }

    private fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return salt
    }

    /**
     * Hashes a password using Argon2id.
     * Returns the full encoded string (PHC format) containing salt, params, and hash.
     */
    fun hashPassword(password: String): String {
        val salt = generateSalt()
        val result = argon2.hash(
            mode = Argon2Mode.ARGON2_ID,
            password = password.toByteArray(Charsets.UTF_8),
            salt = salt,
            tCostInIterations = ITERATIONS,
            mCostInKibibyte = MEMORY,
            parallelism = PARALLELISM,
            hashLengthInBytes = KEY_LENGTH
        )
        return result.encodedOutputAsString()
    }

    /**
     * Verifies a password against a stored Argon2id hash.
     */
    fun verifyPassword(password: String, encodedHash: String): Boolean {
        return argon2.verify(
            mode = Argon2Mode.ARGON2_ID,
            encoded = encodedHash,
            password = password.toByteArray(Charsets.UTF_8)
        )
    }
}
