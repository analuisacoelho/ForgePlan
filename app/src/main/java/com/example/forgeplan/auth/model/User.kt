package com.example.forgeplan.auth.model

data class User(
    val id: String,
    val name: String,
    val username: String,
    val email: String,
    val role: String, // "admin", "manager", "user"
    val isActive: Boolean,
    val photo: String? = null
)