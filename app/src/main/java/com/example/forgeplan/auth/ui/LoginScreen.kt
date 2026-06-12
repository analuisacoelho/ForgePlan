package com.example.forgeplan.auth.ui

import android.content.res.Configuration
import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.R
import com.example.forgeplan.auth.viewmodel.LoginUiState
import com.example.forgeplan.auth.viewmodel.LoginViewModel
import com.example.forgeplan.core.language.appText

private val BrandDarkBlue = Color(0xFF171A4A)

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val emailRequired   = appText(en = "Email is required.",                       pt = "O email é obrigatório.")
    val invalidEmail    = appText(en = "Enter a valid email.",                      pt = "Introduz um email válido.")
    val passwordRequired= appText(en = "Password is required.",                    pt = "A password é obrigatória.")
    val passwordShort   = appText(en = "Password must have at least 6 characters.", pt = "A password deve ter pelo menos 6 caracteres.")

    val loginViewModel: LoginViewModel = viewModel()
    val uiState by loginViewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            val user = (uiState as LoginUiState.Success).user
            onLoginSuccess(user.role.uppercase())
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 32.dp,
                    end = 32.dp,
                    top = if (isLandscape) 18.dp else 32.dp,
                    bottom = if (isLandscape) 18.dp else 32.dp
                )
        ) {
            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    Box(
                        modifier = Modifier.weight(0.9f),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.forgeplan_logo),
                            contentDescription = "ForgePlan logo",
                            modifier = Modifier.fillMaxWidth(0.82f)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1.1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        LoginFormFields(
                            email = email,
                            password = password,
                            emailError = emailError,
                            passwordError = passwordError,
                            uiState = uiState,
                            onEmailChange = { email = it; emailError = null },
                            onPasswordChange = { password = it; passwordError = null },
                            onLoginClick = {
                                emailError = null; passwordError = null
                                if (email.isBlank()) emailError = emailRequired
                                else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) emailError = invalidEmail
                                if (password.isBlank()) passwordError = passwordRequired
                                else if (password.length < 6) passwordError = passwordShort
                                if (emailError == null && passwordError == null) loginViewModel.login(email, password)
                            }
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 8.dp, end = 8.dp, top = 32.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.forgeplan_logo),
                        contentDescription = "ForgePlan logo",
                        modifier = Modifier.fillMaxWidth(0.55f)
                    )

                    Spacer(modifier = Modifier.height(56.dp))

                    LoginFormFields(
                        email = email,
                        password = password,
                        emailError = emailError,
                        passwordError = passwordError,
                        uiState = uiState,
                        onEmailChange = { email = it; emailError = null },
                        onPasswordChange = { password = it; passwordError = null },
                        onLoginClick = {
                            emailError = null; passwordError = null
                            if (email.isBlank()) emailError = emailRequired
                            else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) emailError = invalidEmail
                            if (password.isBlank()) passwordError = passwordRequired
                            else if (password.length < 6) passwordError = passwordShort
                            if (emailError == null && passwordError == null) loginViewModel.login(email, password)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LoginFormFields(
    email: String,
    password: String,
    emailError: String?,
    passwordError: String?,
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Email",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = { Text("your.email@example.com") },
            isError = emailError != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = MaterialTheme.colorScheme.secondary,
                focusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
        if (emailError != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = emailError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = appText(en = "Password", pt = "Palavra-passe"),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = { Text("........") },
            isError = passwordError != null,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = MaterialTheme.colorScheme.secondary,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )
        if (passwordError != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = passwordError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(56.dp))

        Button(
            onClick = onLoginClick,
            enabled = uiState !is LoginUiState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandDarkBlue,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = appText(en = "Login", pt = "Entrar"),
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (uiState is LoginUiState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = uiState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}