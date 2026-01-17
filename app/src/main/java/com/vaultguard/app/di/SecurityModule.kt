package com.vaultguard.app.di

import android.content.Context
import com.vaultguard.app.security.SecurityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): android.content.SharedPreferences {
        val masterKey = androidx.security.crypto.MasterKey.Builder(context)
            .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
            .build()

        return try {
            createEncryptedPrefs(context, masterKey)
        } catch (e: Exception) {
            // CRASH FIX: If KeyStore is corrupted (e.g. reinstall), delete prefs and recreate
            context.deleteSharedPreferences("vault_guard_encrypted_prefs")
            createEncryptedPrefs(context, masterKey)
        }
    }

    private fun createEncryptedPrefs(context: Context, masterKey: androidx.security.crypto.MasterKey): android.content.SharedPreferences {
        return androidx.security.crypto.EncryptedSharedPreferences.create(
            context,
            "vault_guard_encrypted_prefs",
            masterKey,
            androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    @Provides
    @Singleton
    fun provideSecurityManager(@ApplicationContext context: Context, prefs: android.content.SharedPreferences): SecurityManager {
        return SecurityManager(context, prefs)
    }
}
