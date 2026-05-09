package com.example.forgeplan.core.model

data class User(
    val id: Long,
    val name: String,
    val username: String,
    val email: String,
    val password: String,
    val photo: String?,
    val role: String,
    val is_active: Boolean,
    val date_birth: String?,
    val description: String?,
    val created_at: String?,
    val updated_at: String?
)