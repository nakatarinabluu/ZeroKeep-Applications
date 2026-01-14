package com.vaultguard.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SaveSecretRequest(
    @SerializedName("id") val id: String,
    @SerializedName("owner_hash") val ownerHash: String,
    @SerializedName("title_hash") val titleHash: String,
    @SerializedName("encrypted_blob") val encryptedBlob: String,
    @SerializedName("iv") val iv: String
)

data class SecretResponse(
    @SerializedName("id") val id: String,
    @SerializedName("title_hash") val titleHash: String,
    @SerializedName("encrypted_blob") val encryptedBlob: String,
    @SerializedName("iv") val iv: String
)
