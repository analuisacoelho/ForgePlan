package com.example.forgeplan.admin.ui

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.admin.viewmodel.AdminViewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.core.ui.components.ForgePrimaryLargeButton
import com.example.forgeplan.core.ui.components.ForgeSecondaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEditUserScreen(
    userId: Long,
    onUserUpdated: () -> Unit = {},
    onBackClick: () -> Unit = {},
    viewModel: AdminViewModel = viewModel()
) {
    val users by viewModel.users.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val error by viewModel.error.collectAsState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("user") }
    var roleDropdownExpanded by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }

    var showResetDialog by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var resetPasswordError by remember { mutableStateOf<String?>(null) }

    val roles = listOf("admin", "manager", "user")
    val errorRed = Color(0xFFB3261E)

    LaunchedEffect(Unit) { viewModel.loadUsers() }

    LaunchedEffect(users) {
        val user = users.firstOrNull { it.id == userId }
        if (user != null) {
            name = user.name
            username = user.username
            email = user.email
            selectedRole = user.role
        }
    }

    if (successMessage != null) {
        onUserUpdated()
        viewModel.clearMessages()
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = {
                showResetDialog = false
                newPassword = ""
                confirmPassword = ""
                resetPasswordError = null
            },
            title = {
                Text(text = appText(en = "Reset Password", pt = "Repor Password"))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = appText(
                            en = "Set a new password for this user.",
                            pt = "Define uma nova password para este utilizador."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it; resetPasswordError = null },
                        label = { Text(appText(en = "New Password", pt = "Nova Password")) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showNewPassword = !showNewPassword }) {
                                Icon(
                                    imageVector = if (showNewPassword) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.outline,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; resetPasswordError = null },
                        label = { Text(appText(en = "Confirm Password", pt = "Confirmar Password")) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                Icon(
                                    imageVector = if (showConfirmPassword) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    resetPasswordError?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    when {
                        newPassword.isBlank() || confirmPassword.isBlank() ->
                            resetPasswordError = appText(en = "Fill in all fields.", pt = "Preenche todos os campos.")
                        newPassword.length < 6 ->
                            resetPasswordError = appText(en = "Password must be at least 6 characters.", pt = "A password deve ter pelo menos 6 caracteres.")
                        newPassword != confirmPassword ->
                            resetPasswordError = appText(en = "Passwords do not match.", pt = "As passwords não coincidem.")
                        else -> {
                            users.firstOrNull { it.id == userId }?.let { user ->
                                viewModel.resetUserPassword(user, newPassword)
                            }
                            showResetDialog = false
                            newPassword = ""
                            confirmPassword = ""
                            resetPasswordError = null
                        }
                    }
                }) {
                    Text(appText(en = "Confirm", pt = "Confirmar"))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    newPassword = ""
                    confirmPassword = ""
                    resetPasswordError = null
                }) {
                    Text(appText(en = "Cancel", pt = "Cancelar"))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ForgePlanTopBar(
            title = appText(en = "Edit User", pt = "Editar Utilizador")
        )

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 30.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    FormField(
                        label = appText(en = "Name", pt = "Nome"),
                        value = name,
                        onValueChange = { name = it; nameError = null },
                        placeholder = "",
                        error = nameError
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    FormField(
                        label = "Username",
                        value = username,
                        onValueChange = { username = it; usernameError = null },
                        placeholder = "",
                        error = usernameError
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    FormField(
                        label = "Email",
                        value = email,
                        onValueChange = { email = it; emailError = null },
                        placeholder = "",
                        error = emailError
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    RoleDropdown(
                        selectedRole = selectedRole,
                        expanded = roleDropdownExpanded,
                        roles = roles,
                        onExpandedChange = { roleDropdownExpanded = it },
                        onRoleSelected = { selectedRole = it; roleDropdownExpanded = false }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    error?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    ForgePrimaryLargeButton(
                        text = appText(en = "Save Changes", pt = "Guardar Alteracoes"),
                        onClick = {
                            validateUserFields(name, username, email,
                                setNameError = { nameError = it },
                                setUsernameError = { usernameError = it },
                                setEmailError = { emailError = it },
                                onValid = {
                                    users.firstOrNull { it.id == userId }?.let { user ->
                                        viewModel.updateUser(user.copy(
                                            name = name,
                                            username = username,
                                            email = email,
                                            role = selectedRole
                                        ))
                                    }
                                }
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(width = 1.dp, color = errorRed)
                    ) {
                        Text(
                            text = appText(en = "Reset Password", pt = "Repor Password"),
                            style = MaterialTheme.typography.labelLarge,
                            color = errorRed
                        )
                    }
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
                    placeholder = "",
                    error = nameError
                )
                Spacer(modifier = Modifier.height(16.dp))
                FormField(
                    label = "Username",
                    value = username,
                    onValueChange = { username = it; usernameError = null },
                    placeholder = "",
                    error = usernameError
                )
                Spacer(modifier = Modifier.height(16.dp))
                FormField(
                    label = "Email",
                    value = email,
                    onValueChange = { email = it; emailError = null },
                    placeholder = "",
                    error = emailError
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
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                ForgePrimaryLargeButton(
                    text = appText(en = "Save Changes", pt = "Guardar Alteracoes"),
                    onClick = {
                        validateUserFields(name, username, email,
                            setNameError = { nameError = it },
                            setUsernameError = { usernameError = it },
                            setEmailError = { emailError = it },
                            onValid = {
                                users.firstOrNull { it.id == userId }?.let { user ->
                                    viewModel.updateUser(user.copy(
                                        name = name,
                                        username = username,
                                        email = email,
                                        role = selectedRole
                                    ))
                                }
                            }
                        )
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(width = 1.dp, color = errorRed)
                ) {
                    Text(
                        text = appText(en = "Reset Password", pt = "Repor Password"),
                        style = MaterialTheme.typography.labelLarge,
                        color = errorRed
                    )
                }
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