package com.vaultguard.app.domain.use_case

import com.vaultguard.app.fake.FakeSecretRepository
import com.vaultguard.app.security.SecurityManager
import javax.crypto.KeyGenerator
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`

class SaveSecretUseCaseTest {

    private lateinit var fakeRepository: FakeSecretRepository
    private lateinit var mockSecurityManager: SecurityManager
    private lateinit var saveSecretUseCase: SaveSecretUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeSecretRepository()
        mockSecurityManager = Mockito.mock(SecurityManager::class.java)
        saveSecretUseCase = SaveSecretUseCase(fakeRepository, mockSecurityManager)
    }

    @Test
    fun `SaveSecret encrypts and saves to repository`() =
            kotlinx.coroutines.test.runTest {
                // Arrange
                val ownerHash = "user123"
                val masterKey = KeyGenerator.getInstance("AES").generateKey()

                `when`(mockSecurityManager.loadMasterKey()).thenReturn(masterKey)

                // Mock Encrypt: Return dummy Pair(IV, Bytes)
                val dummyIv = ByteArray(12) { 0x01 }
                val dummyEncrypted = ByteArray(32) { 0x02 }
                `when`(mockSecurityManager.encrypt(Mockito.any(), Mockito.any()))
                        .thenReturn(Pair(dummyIv, dummyEncrypted))

                // Act
                val result = saveSecretUseCase("Google", "bob", "pass", ownerHash)

                // Assert
                assertTrue(result.isSuccess)

                // Verify it's in the repo
                val secrets = fakeRepository.fetchSecrets(ownerHash).getOrNull()!!
                assertEquals(1, secrets.size)
                // Check if IV string matches our dummy (010101...)
                assertTrue(secrets[0].iv.startsWith("0101"))
            }
}
