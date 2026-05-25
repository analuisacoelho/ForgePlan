package com.example.forgeplan.auth.repository

import com.example.forgeplan.auth.model.User

class AuthRepository {

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val user = User(
                id = "1",
                name = "Tiago Araújo",
                username = "tiago",
                email = email,
                role = "admin",
                isActive = true
            )
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}