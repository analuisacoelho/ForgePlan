package com.example.forgeplan.tasks.viewmodel

import androidx.lifecycle.ViewModel
import com.example.forgeplan.core.model.User
import com.example.forgeplan.core.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UserViewModel : ViewModel() {

    private val repository = UserRepository()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadUsers() {
        _isLoading.value = true
        _error.value = null

        repository.getUsers(
            onSuccess = { userList ->
                _users.value = userList.filter { it.is_active }
                _isLoading.value = false
            },
            onError = { message ->
                _error.value = message
                _isLoading.value = false
            }
        )
    }

    // Utilizador com sessão activa (definido após o login pelo LoginViewModel)
    var currentUser: com.example.forgeplan.auth.model.User? = null

    /**
     * Iniciais do utilizador para mostrar no TopBar.
     * Devolve "UN" (Unknown) se ainda não há sessão.
     */
    val currentUserInitials: String
        get() {
            val name = currentUser?.name ?: return "UN"
            val parts = name.trim().split(" ")
            return if (parts.size >= 2) {
                "${parts.first().first()}${parts.last().first()}".uppercase()
            } else {
                name.take(2).uppercase()
            }
        }
}