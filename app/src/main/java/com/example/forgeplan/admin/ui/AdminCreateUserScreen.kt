package com.example.forgeplan.admin.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.admin.viewmodel.AdminViewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.core.ui.components.ForgePrimaryLargeButton
import com.example.forgeplan.core.ui.components.ForgeSecondaryButton
import com.example.forgeplan.admin.ui.validateUserFields

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCreateUserScreen(
    onUserCreated: () -> Unit = {},
    onBackClick: () -> Unit = {},
    viewModel: AdminViewModel = viewModel()
) {
    val successMessage by viewModel.successMessage.collectAsState()
    val error by viewModel.error.collectAsState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("user") }
    var roleDropdownExpanded by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val roles = listOf("admin", "manager", "user")

    // Navega para trás após criação com sucesso
    if (successMessage != null) {
        onUserCreated()
        viewModel.clearMessages()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ForgePlanTopBar(
            title = appText(en = "New User", pt = "Novo Utilizador")
        )

        // Landscape: 2 colunas - Portrait: 1 coluna
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 30.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Coluna esquerda: Name, Username, Email
                Column(modifier = Modifier.weight(1f)) {
                    FormField(
                        label = appText(en = "Name", pt = "Nome"),
                        value = name,
                        onValueChange = { name = it; nameError = null },
                        placeholder = "John Doe",
                        error = nameError
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    FormField(
                        label = "Username",
                        value = username,
                        onValueChange = { username = it; usernameError = null },
                        placeholder = "johndoe",
                        error = usernameError
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    FormField(
                        label = "Email",
                        value = email,
                        onValueChange = { email = it; emailError = null },
                        placeholder = "john@example.com",
                        error = emailError,
                        keyboardType = KeyboardType.Email
                    )
                }

                // Coluna direita: Password, Role, botões
                Column(modifier = Modifier.weight(1f)) {
                    FormField(
                        label = appText(en = "Password", pt = "Password"),
                        value = password,
                        onValueChange = { password = it; passwordError = null },
                        placeholder = "........",
                        error = passwordError,
                        keyboardType = KeyboardType.Password,
                        isPassword = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    RoleDropdown(
                        selectedRole = selectedRole,
                        expanded = roleDropdownExpanded,
                        roles = roles,
                        onExpandedChange = { roleDropdownExpanded = it },
                        onRoleSelected = { selectedRole = it; roleDropdownExpanded = false }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    error?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp))
                    }
                    ForgePrimaryLargeButton(
                        text = appText(en = "Create User", pt = "Criar Utilizador"),
                        onClick = {
                            // Valida password separadamente - só existe no create
                            if (password.length < 6) {
                                passwordError = "Min 6 characters"
                            } else {
                                validateUserFields(name, username, email,
                                    setNameError = { nameError = it },
                                    setUsernameError = { usernameError = it },
                                    setEmailError = { emailError = it },
                                    onValid = { viewModel.createUser(name, username, email, password, selectedRole) }
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ForgeSecondaryButton(
                        text = appText(en = "Cancel", pt = "Cancelar"),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onBackClick
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                FormField(
                    label = appText(en = "Name", pt = "Nome"),
                    value = name,
                    onValueChange = { name = it; nameError = null },
                    placeholder = "John Doe",
                    error = nameError
                )
                Spacer(modifier = Modifier.height(16.dp))
                FormField(
                    label = "Username",
                    value = username,
                    onValueChange = { username = it; usernameError = null },
                    placeholder = "johndoe",
                    error = usernameError
                )
                Spacer(modifier = Modifier.height(16.dp))
                FormField(
                    label = "Email",
                    value = email,
                    onValueChange = { email = it; emailError = null },
                    placeholder = "john@example.com",
                    error = emailError,
                    keyboardType = KeyboardType.Email
                )
                Spacer(modifier = Modifier.height(16.dp))
                FormField(
                    label = appText(en = "Password", pt = "Password"),
                    value = password,
                    onValueChange = { password = it; passwordError = null },
                    placeholder = "........",
                    error = passwordError,
                    keyboardType = KeyboardType.Password,
                    isPassword = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                RoleDropdown(
                    selectedRole = selectedRole,
                    expanded = roleDropdownExpanded,
                    roles = roles,
                    onExpandedChange = { roleDropdownExpanded = it },
                    onRoleSelected = { selectedRole = it; roleDropdownExpanded = false }
                )
                Spacer(modifier = Modifier.height(32.dp))
                error?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp))
                }
                ForgePrimaryLargeButton(
                    text = appText(en = "Create", pt = "Criar"),
                    onClick = {
                        if (password.length < 6) {
                            passwordError = "Min 6 characters"
                        } else {
                            validateUserFields(name, username, email,
                                setNameError = { nameError = it },
                                setUsernameError = { usernameError = it },
                                setEmailError = { emailError = it },
                                onValid = { viewModel.createUser(name, username, email, password, selectedRole) }
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                ForgeSecondaryButton(
                    text = appText(en = "Cancel", pt = "Cancelar"),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onBackClick
                )
            }
        }
    }
}