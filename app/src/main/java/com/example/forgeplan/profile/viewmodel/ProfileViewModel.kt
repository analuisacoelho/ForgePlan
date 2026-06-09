package com.example.forgeplan.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.forgeplan.core.network.SupabaseApi
import com.example.forgeplan.core.repository.UserRepository
import com.example.forgeplan.core.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import okhttp3.ResponseBody

class ProfileViewModel : ViewModel() {

    private val userRepository = UserRepository()

    // --- Edit Profile State ---
    private val _editSuccess = MutableStateFlow(false)
    val editSuccess: StateFlow<Boolean> = _editSuccess

    private val _editError = MutableStateFlow<String?>(null)
    val editError: StateFlow<String?> = _editError

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // --- Change Password State ---
    private val _passwordSuccess = MutableStateFlow(false)
    val passwordSuccess: StateFlow<Boolean> = _passwordSuccess

    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError: StateFlow<String?> = _passwordError

    // --- Photo Upload State ---
    private val _photoUrl = MutableStateFlow(SessionManager.currentUser?.photo)
    val photoUrl: StateFlow<String?> = _photoUrl

    fun resetEditState() {
        _editSuccess.value = false
        _editError.value = null
    }

    fun resetPasswordState() {
        _passwordSuccess.value = false
        _passwordError.value = null
    }

    fun updateProfile(name: String, username: String, email: String) {
        val user = SessionManager.currentUser ?: return
        if (name.isBlank() || username.isBlank() || email.isBlank()) {
            _editError.value = "Preenche todos os campos"
            return
        }

        _isLoading.value = true
        val updated = user.copy(
            name = name.trim(),
            username = username.trim(),
            email = email.trim(),
            photo = _photoUrl.value
        )

        userRepository.updateUser(
            user = updated,
            onSuccess = {
                SessionManager.currentUser = updated
                _editSuccess.value = true
                _isLoading.value = false
            },
            onError = { err ->
                _editError.value = err
                _isLoading.value = false
            }
        )
    }

    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        val user = SessionManager.currentUser ?: return

        if (currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
            _passwordError.value = "Preenche todos os campos"
            return
        }
        if (newPassword != confirmPassword) {
            _passwordError.value = "As passwords não coincidem"
            return
        }
        if (newPassword.length < 6) {
            _passwordError.value = "A password deve ter pelo menos 6 caracteres"
            return
        }

        // Verificar password atual
        val storedHash = user.password ?: run {
            _passwordError.value = "Erro ao verificar password atual"
            return
        }
        val result = BCrypt.verifyer().verify(currentPassword.toCharArray(), storedHash)
        if (!result.verified) {
            _passwordError.value = "Password atual incorreta"
            return
        }

        _isLoading.value = true
        val newHash = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray())
        val updated = user.copy(password = newHash)

        userRepository.updateUser(
            user = updated,
            onSuccess = {
                SessionManager.currentUser = updated
                _passwordSuccess.value = true
                _isLoading.value = false
            },
            onError = { err ->
                _passwordError.value = err
                _isLoading.value = false
            }
        )
    }

    fun uploadPhoto(context: Context, uri: Uri) {
        val user = SessionManager.currentUser ?: return
        _isLoading.value = true

        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
        val extension = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val filePath = "avatars/${user.id}.$extension"

        val bytes = contentResolver.openInputStream(uri)?.readBytes() ?: run {
            _editError.value = "Erro ao ler a imagem"
            _isLoading.value = false
            return
        }

        val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())

        SupabaseApi.storageService.uploadFile(
            bucket = "avatars",
            filePath = filePath,
            contentType = mimeType,
            file = requestBody
        ).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val publicUrl = SupabaseApi.publicStorageUrl("avatars", filePath)
                    _photoUrl.value = publicUrl
                    // Guardar URL na BD
                    val updated = user.copy(photo = publicUrl)
                    userRepository.updateUser(
                        user = updated,
                        onSuccess = {
                            SessionManager.currentUser = updated
                            _isLoading.value = false
                        },
                        onError = { err ->
                            _editError.value = err
                            _isLoading.value = false
                        }
                    )
                } else {
                    _editError.value = "Erro ao carregar foto: ${response.code()}"
                    _isLoading.value = false
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                _editError.value = t.message ?: "Erro desconhecido"
                _isLoading.value = false
            }
        })
    }
}