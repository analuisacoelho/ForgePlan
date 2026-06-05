package com.example.forgeplan.core.model

import com.google.gson.annotations.SerializedName

data class User(
    val id: Long,
    val name: String,
    val username: String,
    val email: String,

    // Nullable porque nem sempre pedimos a password ao Supabase
    val password: String? = null,
    val photo: String? = null,
    val role: String,

    // SerializedName garante que o Gson mapeia corretamente "is_active" da BD
    @SerializedName("is_active") val is_active: Boolean = true,
    val date_birth: String? = null,
    val description: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)