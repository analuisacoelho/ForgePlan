package com.example.forgeplan.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Long,
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