package com.example.forgeplan.core.session

import com.example.forgeplan.core.model.User

/**
 * Singleton que guarda o utilizador com sessão activa.
 * É preenchido no LoginScreen após autenticação bem-sucedida
 * e consultado em qualquer ecrã que precise do userId.
 */
object SessionManager {

    var currentUser: User? = null

    val userId: Long
        get() = currentUser?.id ?: -1L

    val userRole: String
        get() = currentUser?.role?.uppercase() ?: "USER"

    val userInitials: String
        get() {
            val name = currentUser?.name ?: return "UN"
            val parts = name.trim().split(" ")
            return if (parts.size >= 2) {
                "${parts.first().first()}${parts.last().first()}".uppercase()
            } else {
                name.take(2).uppercase()
            }
        }

    fun clear() {
        currentUser = null
    }
}