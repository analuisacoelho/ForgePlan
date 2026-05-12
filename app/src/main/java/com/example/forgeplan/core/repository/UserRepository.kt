package com.example.forgeplan.core.repository

import com.example.forgeplan.core.model.User
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

                override fun onResponse(
                    call: Call<List<User>>,
                    response: Response<List<User>>
                ) {

                    if (response.isSuccessful) {

                        onSuccess(response.body() ?: emptyList())

                    } else {

                        onError("Erro ao carregar utilizadores: ${response.code()}")
                    }
                }

                override fun onFailure(
                    call: Call<List<User>>,
                    t: Throwable
                ) {

                    onError(t.message ?: "Erro desconhecido")
                }
            })
    }
}