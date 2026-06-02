package com.example.forgeplan.team

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.example.forgeplan.core.network.SupabaseApi
import com.example.forgeplan.core.model.User
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TeamViewModel : ViewModel() {

    private val _users = mutableStateOf<List<User>>(emptyList())
    val users: State<List<User>> = _users

    private val _loading = mutableStateOf(true)
    val loading: State<Boolean> = _loading

    init {
        loadUsers()
    }

    private fun loadUsers() {
        SupabaseApi.service.getUsers()
            .enqueue(object : Callback<List<User>> {

                override fun onResponse(
                    call: Call<List<User>>,
                    response: Response<List<User>>
                ) {
                    _users.value = response.body() ?: emptyList()
                    _loading.value = false
                }

                override fun onFailure(call: Call<List<User>>, t: Throwable) {
                    _users.value = emptyList()
                    _loading.value = false
                }
            })
    }
}