package com.vaultguard.app.di

import android.content.Context
import com.vaultguard.app.data.remote.VaultApi
import com.vaultguard.app.network.HmacInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.logging.HttpLoggingInterceptor
import com.vaultguard.app.BuildConfig


/**
 * Dependency Injection Module for Network Layer.
 *
 * Provides:
 * - [HmacInterceptor]: For HMAC-SHA256 request signing.
 * - [OkHttpClient]: With pinned user-agents and short timeouts.
 * - [Retrofit]: Configured for secure communication.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val NATIVE_LIB = "vaultguard"
    private const val USER_AGENT_CHROME = "ZeroKeep-Android/1.0"
    private const val TIMEOUT_SECONDS = 30L

    init {
        // Ensure native library is loaded for secret retrieval
        System.loadLibrary(NATIVE_LIB)
    }

    // --- NATIVE INTERFACE ---
    external fun getApiKey(): String
    external fun getHmacSecret(): String
    external fun getBaseUrl(): String

    @Provides
    @Singleton
    fun provideHmacInterceptor(@ApplicationContext context: Context): HmacInterceptor {
        val prefs = context.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE)
        val savedId = prefs.getString("device_id", null)
        
        val deviceId = if (savedId != null) {
            savedId
        } else {
            val newId = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("device_id", newId).apply()
            newId
        }

        // Decrypt secrets securely via JNI at runtime
        val apiKey = getApiKey()
        val hmacSecret = getHmacSecret()
        
        return HmacInterceptor(apiKey, hmacSecret, deviceId)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        hmacInterceptor: HmacInterceptor
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                // Masquerade as standard browser for traffic obfuscation
                val request = original.newBuilder()
                    .header("User-Agent", USER_AGENT_CHROME)
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(hmacInterceptor)
            .addNetworkInterceptor(loggingInterceptor) // Log FINAL request (with headers)
            // CERTIFICATE PINNING (Anti-MitM)
            // Pins the public key of the server to prevent Man-in-the-Middle attacks.
            // 1. Primary: *.vercel.app Leaf Certificate (Provided by User)
            // 2. Backup: GTS Root R1 (Google Trust Services) - Ensures connectivity if Leaf rotates.
            .certificatePinner(
                okhttp3.CertificatePinner.Builder()
                    .add("zerokeep.vercel.app", "sha256/DZM9Oxxp3uxt5JJnpUtfT6flVVHLDXP55RI/BtoaY1E=")
                    .add("zerokeep.vercel.app", "sha256/hxqRlPTu184Imu34eHV543wKhnaL2YEFT9gin9GH7pE=")
                    .build()
            )
            // Strict timeouts to mitigate Slowloris / hanging connections
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
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
