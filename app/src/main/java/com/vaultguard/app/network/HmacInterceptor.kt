package com.vaultguard.app.network

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.nio.charset.Charset
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.UUID

class HmacInterceptor(
    private val apiKey: String,
    private val hmacSecret: String,
    private val deviceId: String
) : Interceptor {

    companion object {
        private const val USER_AGENT = "ZeroKeep-Android/1.0"
        private const val HMAC_ALGORITHM = "HmacSHA256"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val timestamp = System.currentTimeMillis().toString()
        
        // 1. Get Request Body
        val bodyString = bodyToString(original)

        // 2. Calculate Signature
        // 3. Build New Request
        val userAgent = original.header("User-Agent") ?: USER_AGENT
        
        // Formula: HMAC-SHA256(APP_API_KEY + X-Timestamp + User-Agent + X-Device-ID + RequestBody)
        val payload = "$apiKey$timestamp$userAgent$deviceId$bodyString"
        val signature = calculateHmac(payload, hmacSecret)

        android.util.Log.d("SecurityManager", "HMAC Debug: TS=$timestamp, UA=$userAgent, Payload=$payload, Sig=$signature")

        val requestBuilder = original.newBuilder()
            .header("X-API-KEY", apiKey)
            .header("X-Timestamp", timestamp)
            .header("User-Agent", userAgent)
            .header("X-Device-ID", deviceId)
            .header("X-Signature", signature)
            .header("Content-Type", "application/json")
            .method(original.method, original.body)

        return chain.proceed(requestBuilder.build())
    }

    private fun bodyToString(request: Request): String {
        return try {
            val copy = request.newBuilder().build()
            val buffer = Buffer()
            copy.body?.writeTo(buffer)
            buffer.readString(Charset.forName("UTF-8"))
        } catch (e: Exception) {
            ""
        }
    }

    private fun calculateHmac(data: String, secret: String): String {
        val sha256_HMAC = Mac.getInstance(HMAC_ALGORITHM)
        val secret_key = SecretKeySpec(secret.toByteArray(Charset.forName("UTF-8")), HMAC_ALGORITHM)
        sha256_HMAC.init(secret_key)
        val bytes = sha256_HMAC.doFinal(data.toByteArray(Charset.forName("UTF-8")))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
