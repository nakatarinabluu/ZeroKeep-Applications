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

    private const val BASE_URL = "https://zerokeep.vercel.app/"

    init {
        System.loadLibrary("vaultguard")
    }
    
    // NATIVE METHODS (Secrets hidden in C++)
    external fun getApiKey(): String
    external fun getHmacSecret(): String

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
        val certificatePinner = okhttp3.CertificatePinner.Builder()
            // Vercel SHA-256 Fingerprint (Provided by User)
            .add("zerokeep.vercel.app", "sha256/DZM9Oxxp3uxt5JJnpUtfT6flVVHLDXP55RI/BtoaY1E=")
            // Recommended: Add a backup pin for the Intermediate CA (GTS Root R1 or Let's Encrypt) to prevent breakage on rotation
            .build()

        return OkHttpClient.Builder()
            .addInterceptor(hmacInterceptor)
            .certificatePinner(certificatePinner)
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
            .baseUrl(BASE_URL)
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
