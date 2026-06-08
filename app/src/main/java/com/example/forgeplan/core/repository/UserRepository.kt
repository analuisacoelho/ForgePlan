package com.example.forgeplan.core.repository

import com.example.forgeplan.core.model.User
import com.example.forgeplan.core.model.UserPayload
import com.example.forgeplan.core.network.SupabaseApi
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserRepository {

    fun getUsers(
        onSuccess: (List<User>) -> Unit,
        onError: (String) -> Unit
    ) {
        SupabaseApi.service.getUsers()
            .enqueue(object : Callback<List<User>> {
                override fun onResponse(call: Call<List<User>>, response: Response<List<User>>) {
                    if (response.isSuccessful) {
                        onSuccess(response.body() ?: emptyList())
                    } else {
                        onError("Erro ao carregar utilizadores: ${response.code()}")
                    }
                }
                override fun onFailure(call: Call<List<User>>, t: Throwable) {
                    onError(t.message ?: "Erro desconhecido")
                }
            })
    }

    // Cria um novo utilizador na BD - a password já deve vir em hash bcrypt
    fun createUser(
        name: String,
        username: String,
        email: String,
        password: String,
        role: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val newUser = UserPayload(
            name = name,
            username = username,
            email = email,
            password = password,
            role = role
        )
        SupabaseApi.service.createUser(newUser)
            .enqueue(object : Callback<List<User>> {
                override fun onResponse(call: Call<List<User>>, response: Response<List<User>>) {
                    if (response.isSuccessful) {
                        onSuccess()
                    } else {
                        onError("Erro ao criar utilizador: ${response.code()}")
                    }
                }
                override fun onFailure(call: Call<List<User>>, t: Throwable) {
                    onError(t.message ?: "Erro desconhecido")
                }
            })
    }

    // Atualiza dados de um utilizador existente - usado para editar perfil e ativar/desativar conta
    fun updateUser(
        user: User,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        SupabaseApi.service.updateUser("eq.${user.id}", user)
            .enqueue(object : Callback<List<User>> {
                override fun onResponse(call: Call<List<User>>, response: Response<List<User>>) {
                    if (response.isSuccessful) {
                        onSuccess()
                    } else {
                        onError("Erro ao atualizar utilizador: ${response.code()}")
                    }
                }
                override fun onFailure(call: Call<List<User>>, t: Throwable) {
                    onError(t.message ?: "Erro desconhecido")
                }
            })
    }
}