package com.example.forgeplan.core.repository

import com.example.forgeplan.ForgePlanApplication
import com.example.forgeplan.core.database.DatabaseProvider
import com.example.forgeplan.core.database.entity.UserEntity
import com.example.forgeplan.core.model.User
import com.example.forgeplan.core.model.UserPayload
import com.example.forgeplan.core.network.NetworkUtils
import com.example.forgeplan.core.network.SupabaseApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserRepository {

    private val context get() = ForgePlanApplication.instance
    private val db get() = DatabaseProvider.getDatabase(context)

    fun getUsers(
        onSuccess: (List<User>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (NetworkUtils.isOnline(context)) {
            SupabaseApi.service.getUsers()
                .enqueue(object : Callback<List<User>> {
                    override fun onResponse(call: Call<List<User>>, response: Response<List<User>>) {
                        if (response.isSuccessful) {
                            val users = response.body() ?: emptyList()
                            CoroutineScope(Dispatchers.IO).launch {
                                db.userDao().insertAll(users.map { it.toEntity() })
                            }
                            onSuccess(users)
                        } else {
                            loadLocalUsers(onSuccess)
                        }
                    }
                    override fun onFailure(call: Call<List<User>>, t: Throwable) {
                        loadLocalUsers(onSuccess)
                    }
                })
        } else {
            loadLocalUsers(onSuccess)
        }
    }

    private fun loadLocalUsers(onSuccess: (List<User>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val local = db.userDao().getAllUsers().map { it.toModel() }
            withContext(Dispatchers.Main) { onSuccess(local) }
        }
    }

    suspend fun getUserNameById(userId: Long): String = withContext(Dispatchers.IO) {
        db.userDao().getUserById(userId)?.name ?: "Utilizador #$userId"
    }

    private fun User.toEntity() = UserEntity(
        id = id,
        name = name,
        username = username,
        email = email,
        password = password ?: "",
        photo = photo,
        role = role,
        is_active = is_active,
        date_birth = null,
        description = null,
        created_at = null,
        updated_at = null
    )

    private fun UserEntity.toModel() = User(
        id = id,
        name = name,
        username = username,
        email = email,
        is_active = is_active,
        role = role,
        photo = photo,
        password = null
    )

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