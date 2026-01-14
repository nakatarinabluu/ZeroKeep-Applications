package com.vaultguard.app.di

import com.vaultguard.app.data.repository.SecretRepositoryImpl
import com.vaultguard.app.domain.repository.SecretRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSecretRepository(
        secretRepositoryImpl: SecretRepositoryImpl
    ): SecretRepository
}
