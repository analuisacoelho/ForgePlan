package com.example.forgeplan.auth.repository

import com.example.forgeplan.core.model.User
import com.example.forgeplan.core.network.SupabaseApi
import com.example.forgeplan.core.session.SessionManager
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume

/**
 * O login vai ao Supabase buscar o utilizador pelo email + password,
 * guarda-o no SessionManager e devolve o role real.
 */
class AuthRepository {

    suspend fun login(email: String, password: String): Result<User> =
        suspendCancellableCoroutine { continuation ->
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
                            user == null ->
                                continuation.resume(
                                    Result.failure(Exception("Email ou password incorrectos"))
                                )

                            user.password != password ->
                                continuation.resume(
                                    Result.failure(Exception("Email ou password incorrectos"))
                                )

                            !user.is_active ->
                                continuation.resume(
                                    Result.failure(Exception("Conta desactivada. Contacta o administrador."))
                                )

                            else -> {
                                // Guarda o utilizador na sessão global
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
}