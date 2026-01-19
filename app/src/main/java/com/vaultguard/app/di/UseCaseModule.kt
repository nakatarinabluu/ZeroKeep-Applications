package com.vaultguard.app.di

import com.vaultguard.app.domain.repository.SecretRepository
import com.vaultguard.app.domain.use_case.DeleteSecretUseCase
import com.vaultguard.app.domain.use_case.GetSecretsUseCase
import com.vaultguard.app.domain.use_case.SaveSecretUseCase
import com.vaultguard.app.security.SecurityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideGetSecretsUseCase(
            repository: SecretRepository,
            securityManager: SecurityManager
    ): GetSecretsUseCase {
        return GetSecretsUseCase(repository, securityManager)
    }

    @Provides
    @Singleton
    fun provideSaveSecretUseCase(
            repository: SecretRepository,
            securityManager: SecurityManager
    ): SaveSecretUseCase {
        return SaveSecretUseCase(repository, securityManager)
    }

    @Provides
    @Singleton
    fun provideDeleteSecretUseCase(repository: SecretRepository): DeleteSecretUseCase {
        return DeleteSecretUseCase(repository)
    }
}
