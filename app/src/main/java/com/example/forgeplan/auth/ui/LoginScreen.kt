package com.example.forgeplan.auth.ui

import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.forgeplan.R
import com.example.forgeplan.core.ui.LanguageButton

// Cor da marca — não muda com o tema
private val BrandDarkBlue = Color(0xFF171A4A)

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit
) {
    var selectedLanguage by remember { mutableStateOf("EN") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val emailRequired = if (selectedLanguage == "PT") "O email é obrigatório." else "Email is required."
    val invalidEmail = if (selectedLanguage == "PT") "Introduz um email válido." else "Enter a valid email."
    val passwordRequired = if (selectedLanguage == "PT") "A password é obrigatória." else "Password is required."
    val passwordShort = if (selectedLanguage == "PT") "A password deve ter pelo menos 6 caracteres." else "Password must have at least 6 characters."

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            // Botões de idioma
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LanguageButton(
                    text = "us EN",
                    selected = selectedLanguage == "EN",
                    onClick = { selectedLanguage = "EN" }
                )
                LanguageButton(
                    text = "pt PT",
                    selected = selectedLanguage == "PT",
                    onClick = { selectedLanguage = "PT" }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo
                Image(
                    painter = painterResource(id = R.drawable.forgeplan_logo),
                    contentDescription = "ForgePlan logo",
                    modifier = Modifier.fillMaxWidth(0.55f)
                )

                Spacer(modifier = Modifier.height(56.dp))

                // Campo Email
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Email",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            emailError = null
                        },
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
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    if (emailError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = emailError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Campo Password
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (selectedLanguage == "PT") "Palavra-passe" else "Password",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = null
                        },
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
                        Text(
                            text = passwordError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(56.dp))

                // Botão Login
                Button(
                    onClick = {
                        emailError = null
                        passwordError = null

                        when {
                            email.isBlank() -> emailError = emailRequired
                            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> emailError = invalidEmail
                        }
                        when {
                            password.isBlank() -> passwordError = passwordRequired
                            password.length < 6 -> passwordError = passwordShort
                        }

                        if (emailError == null && passwordError == null) {
                            // TODO: substituir por role real do Supabase
                            onLoginSuccess("ADMIN")
                        }
                    },
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
                        text = if (selectedLanguage == "PT") "Entrar" else "Login",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}