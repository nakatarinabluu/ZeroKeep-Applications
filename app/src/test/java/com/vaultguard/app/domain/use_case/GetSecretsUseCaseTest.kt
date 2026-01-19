package com.vaultguard.app.domain.use_case

import com.vaultguard.app.fake.FakeSecretRepository
import com.vaultguard.app.security.SecurityManager
import java.nio.charset.StandardCharsets
import javax.crypto.KeyGenerator
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`

class GetSecretsUseCaseTest {

    private lateinit var fakeRepository: FakeSecretRepository
    private lateinit var mockSecurityManager: SecurityManager
    private lateinit var getSecretsUseCase: GetSecretsUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeSecretRepository()
        mockSecurityManager = Mockito.mock(SecurityManager::class.java)
        getSecretsUseCase = GetSecretsUseCase(fakeRepository, mockSecurityManager)
    }

    @Test
    fun `GetSecrets returns decrypted list when repository succeeds`() =
            kotlinx.coroutines.test.runTest {
                // Arrange
                val ownerHash = "user123"
                val masterKey = KeyGenerator.getInstance("AES").generateKey()

                // Mock SecurityManager behavior
                `when`(mockSecurityManager.hasMasterKey()).thenReturn(true)
                `when`(mockSecurityManager.loadMasterKey()).thenReturn(masterKey)
                // Mock Decrypt: Just return "Title|User|Pass" bytes for ANY input
                `when`(mockSecurityManager.decrypt(Mockito.any(), Mockito.any(), Mockito.any()))
                        .thenReturn("Netflix|alice|password123".toByteArray(StandardCharsets.UTF_8))

                // Pre-fill Repository with 1 item
                fakeRepository.saveSecret("1", ownerHash, "hash", "encrypted", "iv")

                // Act
                val result = getSecretsUseCase(ownerHash)

                // Assert
                assertTrue(result.isSuccess)
                val secrets = result.getOrNull()!!
                assertEquals(1, secrets.size)
                assertEquals("Netflix", secrets[0].title)
                assertEquals("alice", secrets[0].username)
            }

    @Test
    fun `GetSecrets returns failure when vault is locked`() =
            kotlinx.coroutines.test.runTest {
                // Arrange
                `when`(mockSecurityManager.hasMasterKey()).thenReturn(false)

                // Act
                val result = getSecretsUseCase("user123")

                // Assert
                assertTrue(result.isFailure)
                assertEquals("Vault Locked or Key Missing", result.exceptionOrNull()?.message)
            }
}
