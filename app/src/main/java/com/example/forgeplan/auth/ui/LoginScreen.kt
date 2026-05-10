package com.example.forgeplan.auth.ui

import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.forgeplan.R

@Composable
fun LoginScreen(
    selectedRole: String,
    onLoginSuccess: () -> Unit
) {
    var selectedLanguage by remember { mutableStateOf("EN") }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val darkBlue = Color(0xFF17184A)
    val beige = Color(0xFFEED7AD)
    val lightGrey = Color(0xFFF7F7F7)

    val emailRequired = if (selectedLanguage == "PT") "O email é obrigatório." else "Email is required."
    val invalidEmail = if (selectedLanguage == "PT") "Introduz um email válido." else "Enter a valid email."
    val passwordRequired = if (selectedLanguage == "PT") "A password é obrigatória." else "Password is required."
    val passwordShort = if (selectedLanguage == "PT") "A password deve ter pelo menos 6 caracteres." else "Password must have at least 6 characters."

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(lightGrey)
            .padding(32.dp)
    ) {
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
            Image(
                painter = painterResource(id = R.drawable.forgeplan_full_logo),
                contentDescription = "ForgePlan logo",
                modifier = Modifier
                    .fillMaxWidth(0.55f)
            )

            Spacer(modifier = Modifier.height(56.dp))

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Email",
                    color = Color.Black
                )

                TextField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = null
                    },
                    placeholder = { Text("your.email@example.com") },
                    isError = emailError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (emailError != null) {
                    Text(
                        text = emailError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = if (selectedLanguage == "PT") "Palavra-passe" else "Password",
                    color = Color.Black
                )

                TextField(
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
                    modifier = Modifier.fillMaxWidth()
                )

                if (passwordError != null) {
                    Text(
                        text = passwordError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(56.dp))

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
                        println("LOGIN VALIDADO: $email | ROLE: $selectedRole")
                        onLoginSuccess()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = darkBlue,
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