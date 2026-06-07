package com.example.forgeplan.admin.viewmodel

import androidx.lifecycle.ViewModel
import com.example.forgeplan.auth.repository.AuthRepository
import com.example.forgeplan.core.model.ActivityLog
import com.example.forgeplan.core.model.User
import com.example.forgeplan.core.network.SupabaseApi
import com.example.forgeplan.core.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminViewModel : ViewModel() {

    private val userRepository = UserRepository()
    private val authRepository = AuthRepository()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _activityLogs = MutableStateFlow<List<ActivityLog>>(emptyList())
    val activityLogs: StateFlow<List<ActivityLog>> = _activityLogs

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    fun loadUsers() {
        _isLoading.value = true
        _error.value = null
        userRepository.getUsers(
            onSuccess = { userList ->
                _users.value = userList
                _isLoading.value = false
            },
            onError = { message ->
                _error.value = message
                _isLoading.value = false
            }
        )
    }

    // Carrega o histórico de atividades ordenado por data
    fun loadActivityLogs() {
        _isLoading.value = true
        _error.value = null
        SupabaseApi.service.getActivityLogs()
            .enqueue(object : Callback<List<ActivityLog>> {
                override fun onResponse(call: Call<List<ActivityLog>>, response: Response<List<ActivityLog>>) {
                    _activityLogs.value = response.body() ?: emptyList()
                    _isLoading.value = false
                }
                override fun onFailure(call: Call<List<ActivityLog>>, t: Throwable) {
                    _error.value = t.message
                    _isLoading.value = false
                }
            })
    }

    fun toggleUserActive(user: User) {
        val updated = user.copy(is_active = !user.is_active)
        userRepository.updateUser(
            user = updated,
            onSuccess = {
                _successMessage.value = if (updated.is_active) "Conta ativada." else "Conta desativada."
                loadUsers()
            },
            onError = { message -> _error.value = message }
        )
    }

    fun createUser(name: String, username: String, email: String, password: String, role: String) {
        _isLoading.value = true
        val hashedPassword = authRepository.hashPassword(password)
        userRepository.createUser(
            name = name,
            username = username,
            email = email,
            password = hashedPassword,
            role = role,
            onSuccess = {
                _successMessage.value = "Utilizador criado."
                loadUsers()
            },
            onError = { message ->
                _error.value = message
                _isLoading.value = false
            }
        )
    }

    fun updateUser(user: User) {
        userRepository.updateUser(
            user = user,
            onSuccess = {
                _successMessage.value = "Utilizador atualizado."
                loadUsers()
            },
            onError = { message -> _error.value = message }
        )
    }

    fun clearMessages() {
        _error.value = null
        _successMessage.value = null
    }
}