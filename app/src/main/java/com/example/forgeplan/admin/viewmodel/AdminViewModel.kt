package com.example.forgeplan.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.forgeplan.auth.repository.AuthRepository
import com.example.forgeplan.core.model.User
import com.example.forgeplan.core.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AdminViewModel : ViewModel() {

    private val userRepository = UserRepository()
    private val authRepository = AuthRepository()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    // Carrega todos os utilizadores - admin vê todos, ativos e inativos
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

    // Ativa ou desativa uma conta sem a apagar - histórico é preservado
    fun toggleUserActive(user: User) {
        val updated = user.copy(is_active = !user.is_active)
        userRepository.updateUser(
            user = updated,
            onSuccess = {
                _successMessage.value = if (updated.is_active) "Conta ativada." else "Conta desativada."
                loadUsers()
            },
            onError = { message ->
                _error.value = message
            }
        )
    }

    // Cria novo utilizador com password já em hash bcrypt
    fun createUser(
        name: String,
        username: String,
        email: String,
        password: String,
        role: String
    ) {
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

    // Atualiza dados de um utilizador existente
    fun updateUser(user: User) {
        userRepository.updateUser(
            user = user,
            onSuccess = {
                _successMessage.value = "Utilizador atualizado."
                loadUsers()
            },
            onError = { message ->
                _error.value = message
            }
        )
    }

    fun clearMessages() {
        _error.value = null
        _successMessage.value = null
    }
}