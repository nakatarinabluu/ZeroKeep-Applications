package com.vaultguard.app.data.remote

import com.vaultguard.app.data.remote.dto.SaveSecretRequest
import com.vaultguard.app.data.remote.dto.SecretResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface VaultApi {

    @POST("/api/v1/vault/save")
    suspend fun saveSecret(@Body request: SaveSecretRequest): Response<Unit>

    @GET("/api/v1/vault/fetch")
    suspend fun fetchSecrets(@Header("x-owner-hash") ownerHash: String): Response<List<SecretResponse>>

    @POST("/api/v1/vault/delete")
    suspend fun deleteSecret(@Body id: String): Response<Unit> // Need DTO for ID wrapper if API expects object

    @POST("/api/v1/vault/wipe")
    suspend fun wipeVault(@Header("x-wipe-token") wipeToken: String): Response<Unit>
}
