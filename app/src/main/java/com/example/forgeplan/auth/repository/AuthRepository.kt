package com.example.forgeplan.auth.repository

import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.forgeplan.core.model.User
import com.example.forgeplan.core.network.SupabaseApi
import com.example.forgeplan.core.session.SessionManager
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume

class AuthRepository {

    suspend fun login(email: String, password: String): Result<User> =
        suspendCancellableCoroutine { continuation ->

            // Busca o utilizador pelo email — a password não é enviada na query
            SupabaseApi.service.getUserByEmail("eq.$email")
                .enqueue(object : Callback<List<User>> {

                    override fun onResponse(
                        call: Call<List<User>>,
                        response: Response<List<User>>
                    ) {
                        if (!response.isSuccessful) {
                            continuation.resume(
                                Result.failure(Exception("Erro de servidor: ${response.code()}"))
                            )
                            return
                        }

                        val users = response.body() ?: emptyList()
                        val user = users.firstOrNull()

                        when {
                            user == null -> {
                                // Mensagem genérica — não revelar se é o email ou password que está errado
                                continuation.resume(
                                    Result.failure(Exception("Email ou password incorretos"))
                                )
                            }

                            !verifyPassword(password, user.password) -> {
                                // BCrypt compara a password introduzida com o hash da BD
                                continuation.resume(
                                    Result.failure(Exception("Email ou password incorretos"))
                                )
                            }

                            !user.is_active -> {
                                // Conta existe e password correta, mas conta está desativada
                                continuation.resume(
                                    Result.failure(Exception("Conta desativada. Contacta o administrador."))
                                )
                            }

                            else -> {
                                // Login válido — guarda o utilizador na sessão global
                                SessionManager.currentUser = user
                                continuation.resume(Result.success(user))
                            }
                        }
                    }

                    override fun onFailure(call: Call<List<User>>, t: Throwable) {
                        continuation.resume(Result.failure(t))
                    }
                })
        }

    // Verifica a password introduzida contra o hash bcrypt guardado na BD
    private fun verifyPassword(plainPassword: String, hashedPassword: String?): Boolean {
        if (hashedPassword == null) return false
        return try {
            BCrypt.verifyer().verify(plainPassword.toCharArray(), hashedPassword).verified
        } catch (e: Exception) {
            // Hash inválido ou corrompido - recusa por segurança
            false
        }
    }

    // Usar este método sempre que criares um novo utilizador no CRUD Admin
    fun hashPassword(plainPassword: String): String {
        // cost=12 é o standard atual — equilibrio entre segurança e performance
        return BCrypt.withDefaults().hashToString(12, plainPassword.toCharArray())
    }
}