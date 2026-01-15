package com.vaultguard.app.di

import android.content.Context
import com.vaultguard.app.data.remote.VaultApi
import com.vaultguard.app.network.HmacInterceptor
import com.vaultguard.app.network.NetworkClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    init {
        System.loadLibrary("vaultguard")
    }

    // NATIVE METHODS (Secrets hidden in C++)
    external fun getApiKey(): String
    external fun getHmacSecret(): String
    external fun getBaseUrl(): String

    @Provides
    @Singleton
    fun provideHmacInterceptor(): HmacInterceptor {
        // Fetch secrets securely from Native Layer
        val apiKey = getApiKey()
        val hmacSecret = getHmacSecret()
        val deviceId = java.util.UUID.randomUUID().toString() // Ephemeral ID for this session instance
        return HmacInterceptor(apiKey, hmacSecret, deviceId)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        hmacInterceptor: HmacInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(hmacInterceptor)
            // Hardening: Short timeouts to prevent hanging connections
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(getBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideVaultApi(retrofit: Retrofit): VaultApi {
        return retrofit.create(VaultApi::class.java)
    }
}
