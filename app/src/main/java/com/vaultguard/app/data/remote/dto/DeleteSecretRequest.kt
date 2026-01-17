package com.vaultguard.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DeleteSecretRequest(
    @SerializedName("id") val id: String,
    @SerializedName("owner_hash") val ownerHash: String
)
