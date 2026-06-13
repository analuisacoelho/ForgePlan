package com.example.forgeplan.admin.viewmodel

import androidx.lifecycle.ViewModel
import com.example.forgeplan.auth.repository.AuthRepository
import com.example.forgeplan.core.language.AppLanguage
import com.example.forgeplan.core.model.ActivityLog
import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.model.User
import com.example.forgeplan.core.network.SupabaseApi
import com.example.forgeplan.core.repository.ActivityLogRepository
import com.example.forgeplan.core.repository.ProjectRepository
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
    private val projectRepository = ProjectRepository()
    private val logRepository = ActivityLogRepository()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects

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

    fun loadProjects() {
        _isLoading.value = true
        _error.value = null
        projectRepository.getProjects(
            onSuccess = { projectList ->
                _projects.value = projectList
                _isLoading.value = false
            },
            onError = { message ->
                _error.value = message
                _isLoading.value = false
            }
        )
    }

    fun loadActivityLogs() {
        _isLoading.value = true
        _error.value = null
        SupabaseApi.service.getActivityLogs()
            .enqueue(object : Callback<List<ActivityLog>> {
                override fun onResponse(
                    call: Call<List<ActivityLog>>,
                    response: Response<List<ActivityLog>>
                ) {
                    _activityLogs.value = response.body() ?: emptyList()
                    _isLoading.value = false
                }
                override fun onFailure(call: Call<List<ActivityLog>>, t: Throwable) {
                    _error.value = if (AppLanguage.isPortuguese())
                        "Erro ao carregar atividades"
                    else
                        "Error loading activity logs"
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
                logRepository.logActivity(
                    action = action,
                    entityType = "user",
                    entityId = user.id,
                    detailsEn = "Admin: ${SessionManager.currentUser?.name} ${if (updated.is_active) "activated" else "deactivated"} the account of '${user.name}'",
                    detailsPt = "Admin: ${SessionManager.currentUser?.name} ${if (updated.is_active) "ativou" else "desativou"} a conta de '${user.name}'"
                )
                _successMessage.value = if (AppLanguage.isPortuguese())
                    if (updated.is_active) "Conta ativada." else "Conta desativada."
                else
                    if (updated.is_active) "Account activated." else "Account deactivated."
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
                logRepository.logActivity(
                    action = "USER_CREATED",
                    entityType = "user",
                    entityId = 0L,
                    detailsEn = "Admin: ${SessionManager.currentUser?.name} created user '$name' with role '$role'",
                    detailsPt = "Admin: ${SessionManager.currentUser?.name} criou o utilizador '$name' com role '$role'"
                )
                _successMessage.value = if (AppLanguage.isPortuguese()) "Utilizador criado." else "User created."
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
                logRepository.logActivity(
                    action = "USER_UPDATED",
                    entityType = "user",
                    entityId = user.id,
                    detailsEn = "Admin: ${SessionManager.currentUser?.name} edited user '${user.name}'",
                    detailsPt = "Admin: ${SessionManager.currentUser?.name} editou o utilizador '${user.name}'"
                )
                _successMessage.value = if (AppLanguage.isPortuguese()) "Utilizador atualizado." else "User updated."
                loadUsers()
            },
            onError = { message -> _error.value = message }
        )
    }

    fun resetUserPassword(user: User, newPassword: String) {
        val hashedPassword = authRepository.hashPassword(newPassword)
        val updated = user.copy(password = hashedPassword)
        userRepository.updateUser(
            user = updated,
            onSuccess = {
                logRepository.logActivity(
                    action = "USER_PASSWORD_RESET",
                    entityType = "user",
                    entityId = user.id,
                    detailsEn = "Admin: ${SessionManager.currentUser?.name} reset the password of '${user.name}'",
                    detailsPt = "Admin: ${SessionManager.currentUser?.name} repôs a password de '${user.name}'"
                )
                _successMessage.value = if (AppLanguage.isPortuguese())
                    "Password reposta com sucesso."
                else
                    "Password reset successfully."
            },
            onError = { message -> _error.value = message }
        )
    }

    fun clearMessages() {
        _error.value = null
        _successMessage.value = null
    }
}