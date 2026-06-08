package com.example.forgeplan.admin.viewmodel

import androidx.lifecycle.ViewModel
import com.example.forgeplan.auth.repository.AuthRepository
import com.example.forgeplan.core.model.ActivityLog
import com.example.forgeplan.core.model.User
import com.example.forgeplan.core.network.SupabaseApi
import com.example.forgeplan.core.network.SupabaseService
import com.example.forgeplan.core.repository.UserRepository
import com.example.forgeplan.core.session.SessionManager
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
                val action = if (updated.is_active) "USER_ACTIVATED" else "USER_DEACTIVATED"
                logActivity(action, "user", user.id, "User: ${user.name}")
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
                logActivity("USER_CREATED", "user", 0L, "Admin: ${SessionManager.currentUser?.name} criou o utilizador '$name' com role '$role'")
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
                logActivity("USER_UPDATED", "user", user.id, "Admin: ${SessionManager.currentUser?.name} editou o utilizador '${user.name}'")
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

    // Regista uma ação no histórico de atividades
    private fun logActivity(action: String, entityType: String, entityId: Long, details: String = "") {
        val payload = SupabaseService.ActivityLogPayload(
            user_id = SessionManager.userId,
            action = action,
            entity_type = entityType,
            entity_id = entityId,
            details = details
        )
        SupabaseApi.service.createActivityLog(payload)
            .enqueue(object : retrofit2.Callback<Unit> {
                override fun onResponse(call: retrofit2.Call<Unit>, response: retrofit2.Response<Unit>) {}
                override fun onFailure(call: retrofit2.Call<Unit>, t: Throwable) {}
            })
    }
}