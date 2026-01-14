package com.vaultguard.app.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class NetworkClient(private val context: Context) {

    // PRE-CALCULATED SHA-256 HASH OF VERCEL DEPLOYMENT CERTIFICATE
    // REPLACE THIS WITH YOUR REAL PUBLIC KEY HASH
    private val VERCEL_CERT_PIN = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=" // Placeholder

    fun createPinnedHttpClient(hmacInterceptor: HmacInterceptor): OkHttpClient {
        val certificatePinner = CertificatePinner.Builder()
            .add("your-vercel-app.vercel.app", VERCEL_CERT_PIN)
            .build()

        return OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
            .addInterceptor(hmacInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun isVpnActive(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }
}
