package com.example.forgeplan.profile

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.session.SessionManager
import com.example.forgeplan.core.ui.components.ForgeAvatar
import com.example.forgeplan.core.ui.components.ForgePrimaryButton
import com.example.forgeplan.core.ui.components.ForgeSecondaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val user = SessionManager.currentUser
    var name by remember { mutableStateOf(user?.name ?: "") }
    var username by remember { mutableStateOf(user?.username ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }

    val isLoading by viewModel.isLoading.collectAsState()
    val editSuccess by viewModel.editSuccess.collectAsState()
    val editError by viewModel.editError.collectAsState()
    val photoUrl by viewModel.photoUrl.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Galeria picker
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadPhoto(context, it) }
    }

    LaunchedEffect(editSuccess) {
        if (editSuccess) {
            snackbarHostState.showSnackbar(
                appText(en = "Profile updated!", pt = "Perfil atualizado!")
            )
            viewModel.resetEditState()
            onBack()
        }
    }

    LaunchedEffect(editError) {
        editError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.resetEditState()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = appText(en = "Edit Profile", pt = "Editar Perfil"),
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
            // Avatar clicável para mudar foto
            Box(contentAlignment = Alignment.BottomEnd) {
                if (!photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = appText(en = "Profile photo", pt = "Foto de perfil"),
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .clickable { launcher.launch("image/*") },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .clickable { launcher.launch("image/*") }
                    ) {
                        ForgeAvatar(
                            initials = SessionManager.userInitials,
                            size = 88
                        )
                    }
                }
                // Ícone de editar por cima do avatar
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = appText(en = "Tap to change photo", pt = "Toca para mudar a foto"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(28.dp))

            if (isLandscape) {
                // Landscape: 2 colunas
                Row(modifier = Modifier.fillMaxWidth()) {
                    ProfileTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = appText(en = "Name", pt = "Nome"),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    ProfileTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = appText(en = "Username", pt = "Username"),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    ProfileTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = appText(en = "Email", pt = "Email"),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Spacer(modifier = Modifier.weight(1f))
                }
            } else {
                // Portrait: 1 coluna
                ProfileTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = appText(en = "Name", pt = "Nome"),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                ProfileTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = appText(en = "Username", pt = "Username"),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                ProfileTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = appText(en = "Email", pt = "Email"),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                ForgePrimaryButton(
                    text = appText(en = "Save Changes", pt = "Guardar Alterações"),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.updateProfile(name, username, email) }
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
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
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
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.outline,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedLabelColor = MaterialTheme.colorScheme.outline,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    )
}