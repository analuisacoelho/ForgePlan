package com.example.forgeplan.profile

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.ui.components.ForgePrimaryButton
import com.example.forgeplan.core.ui.components.ForgeSecondaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var showCurrent by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsState()
    val passwordSuccess by viewModel.passwordSuccess.collectAsState()
    val passwordError by viewModel.passwordError.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(passwordSuccess) {
        if (passwordSuccess) {
            snackbarHostState.showSnackbar(
                appText(en = "Password changed successfully!", pt = "Password alterada com sucesso!")
            )
            viewModel.resetPasswordState()
            onBack()
        }
    }

    LaunchedEffect(passwordError) {
        passwordError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.resetPasswordState()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = appText(en = "Change Password", pt = "Alterar Password"),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = appText(en = "Back", pt = "Voltar"),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (isLandscape) 64.dp else 18.dp,
                    vertical = 24.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = appText(
                    en = "Enter your current password and choose a new one.",
                    pt = "Introduz a tua password atual e escolhe uma nova."
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isLandscape) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    PasswordTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = appText(en = "Current Password", pt = "Password Atual"),
                        visible = showCurrent,
                        onToggleVisibility = { showCurrent = !showCurrent },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Spacer(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    PasswordTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = appText(en = "New Password", pt = "Nova Password"),
                        visible = showNew,
                        onToggleVisibility = { showNew = !showNew },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    PasswordTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = appText(en = "Confirm Password", pt = "Confirmar Password"),
                        visible = showConfirm,
                        onToggleVisibility = { showConfirm = !showConfirm },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                PasswordTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = appText(en = "Current Password", pt = "Password Atual"),
                    visible = showCurrent,
                    onToggleVisibility = { showCurrent = !showCurrent },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                PasswordTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = appText(en = "New Password", pt = "Nova Password"),
                    visible = showNew,
                    onToggleVisibility = { showNew = !showNew },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                PasswordTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = appText(en = "Confirm Password", pt = "Confirmar Password"),
                    visible = showConfirm,
                    onToggleVisibility = { showConfirm = !showConfirm },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                ForgePrimaryButton(
                    text = appText(en = "Change Password", pt = "Alterar Password"),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        viewModel.changePassword(currentPassword, newPassword, confirmPassword)
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
                ForgeSecondaryButton(
                    text = appText(en = "Cancel", pt = "Cancelar"),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onBack
                )
            }
        }
    }
}

@Composable
private fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall
            )
        },
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (visible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.outline,
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedLabelColor = MaterialTheme.colorScheme.outline,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    )
}