package com.example.forgeplan.core.model

data class UserPayload(
    val name: String,
    val username: String,
    val email: String,
    val password: String,
    val role: String,
    val is_active: Boolean = true
)