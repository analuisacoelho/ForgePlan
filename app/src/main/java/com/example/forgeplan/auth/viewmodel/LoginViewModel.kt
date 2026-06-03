package com.example.forgeplan.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.forgeplan.core.model.User
import com.example.forgeplan.auth.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val user: User) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    fun login(email: String, password: String) {
        if (email.isBlank()) {
            _uiState.value = LoginUiState.Error("O email não pode estar vazio")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = LoginUiState.Error("Email inválido")
            return
        }
        if (password.isBlank()) {
            _uiState.value = LoginUiState.Error("A password não pode estar vazia")
            return
        }
        if (password.length < 6) {
            _uiState.value = LoginUiState.Error("A password deve ter pelo menos 6 caracteres")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val result = repository.login(email, password)
            result.fold(
                onSuccess = { user ->
                    _uiState.value = LoginUiState.Success(user)
                },
                onFailure = { error ->
                    _uiState.value = LoginUiState.Error(error.message ?: "Erro desconhecido")
                }
            )
        }
    }
}