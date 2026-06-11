package com.example.forgeplan.admin.ui

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.admin.viewmodel.AdminViewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.model.User
import com.example.forgeplan.core.ui.components.ForgeMiniChip
import com.example.forgeplan.core.ui.components.ForgeSearchBar

@Composable
fun AdminUsersScreen(
    onBackClick: () -> Unit = {},
    onCreateUserClick: () -> Unit = {},
    onEditUserClick: (Long) -> Unit = {},
    onActivityClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: AdminViewModel = viewModel()
) {
    val users by viewModel.users.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    var searchText by remember { mutableStateOf("") }
    var userToToggle by remember { mutableStateOf<User?>(null) }

    // Mapa manager_id -> nº projetos ativos
    val projectCountByManager = projects
        .filter { it.manager_id != null && it.status?.uppercase() != "DONE" }
        .groupBy { it.manager_id!! }
        .mapValues { it.value.size }

    LaunchedEffect(Unit) {
        viewModel.loadUsers()
        viewModel.loadProjects()
    }

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearMessages()
        }
    }

    val visibleUsers = users.filter { user ->
        searchText.isBlank() ||
                user.name.contains(searchText, ignoreCase = true) ||
                user.email.contains(searchText, ignoreCase = true) ||
                user.role.contains(searchText, ignoreCase = true)
    }

    userToToggle?.let { user ->
        AlertDialog(
            onDismissRequest = { userToToggle = null },
            title = {
                Text(
                    text = if (user.is_active)
                        appText(en = "Deactivate account?", pt = "Desativar conta?")
                    else
                        appText(en = "Activate account?", pt = "Ativar conta?")
                )
            },
            text = {
                Text(
                    text = if (user.is_active)
                        appText(
                            en = "${user.name} will not be able to login.",
                            pt = "${user.name} nao conseguira fazer login."
                        )
                    else
                        appText(
                            en = "${user.name} will be able to login again.",
                            pt = "${user.name} voltara a conseguir fazer login."
                        )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.toggleUserActive(user)
                    userToToggle = null
                }) {
                    Text(
                        text = appText(en = "Confirm", pt = "Confirmar"),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { userToToggle = null }) {
                    Text(
                        text = appText(en = "Cancel", pt = "Cancelar"),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )
    }

    AdminScaffold(
        selectedItem = "Users",
        onProjectsClick = onBackClick,
        onUsersClick = {},
        onActivityClick = onActivityClick,
        onProfileClick = onProfileClick,
        onLogout = onLogout
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = if (isLandscape) 30.dp else 18.dp,
                        vertical = if (isLandscape) 12.dp else 16.dp
                    )
            ) {
                ForgeSearchBar(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = appText(en = "Search user", pt = "Pesquisar utilizador")
                )

                Spacer(modifier = Modifier.height(if (isLandscape) 14.dp else 22.dp))

                Text(
                    text = appText(en = "Users", pt = "Utilizadores"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(14.dp))

                successMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                when {
                    isLoading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    visibleUsers.isEmpty() -> Text(
                        text = appText(en = "No users found.", pt = "Nenhum utilizador encontrado."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    else -> {
                        if (isLandscape) {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                items(visibleUsers.chunked(2)) { rowUsers ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowUsers.forEach { user ->
                                            AdminUserCard(
                                                user = user,
                                                projectCount = projectCountByManager[user.id] ?: 0,
                                                onEditClick = { onEditUserClick(user.id) },
                                                onToggleActive = { userToToggle = user },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        if (rowUsers.size == 1) Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                items(visibleUsers) { user ->
                                    AdminUserCard(
                                        user = user,
                                        projectCount = projectCountByManager[user.id] ?: 0,
                                        onEditClick = { onEditUserClick(user.id) },
                                        onToggleActive = { userToToggle = user }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = onCreateUserClick,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 18.dp)
                    .size(56.dp)
            ) {
                Text(text = "+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}

@Composable
fun AdminUserCard(
    user: User,
    projectCount: Int = 0,
    onEditClick: () -> Unit,
    onToggleActive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isManager = user.role.uppercase() == "MANAGER"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ForgeMiniChip(
                        text = user.role.lowercase().replaceFirstChar { it.uppercase() },
                        containerColor = when (user.role.uppercase()) {
                            "ADMIN" -> Color(0xFFD0E8FF)
                            "MANAGER" -> Color(0xFFE8D0FF)
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        },
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    ForgeMiniChip(
                        text = if (user.is_active)
                            appText(en = "Active", pt = "Ativo")
                        else
                            appText(en = "Inactive", pt = "Inativo"),
                        containerColor = if (user.is_active) Color(0xFFB7EBC0) else Color(0xFFFFD0D0),
                        contentColor = if (user.is_active) Color(0xFF14532D) else Color(0xFF7F1D1D)
                    )
                    // Badge de carga só para managers
                    if (isManager) {
                        ForgeMiniChip(
                            text = appText(
                                en = "$projectCount active",
                                pt = "$projectCount ativos"
                            ),
                            containerColor = when {
                                projectCount == 0 -> Color(0xFFB7EBC0)
                                projectCount <= 2 -> Color(0xFFFFF3CD)
                                else -> Color(0xFFFFD0D0)
                            },
                            contentColor = when {
                                projectCount == 0 -> Color(0xFF14532D)
                                projectCount <= 2 -> Color(0xFF7B5200)
                                else -> Color(0xFF7F1D1D)
                            }
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Switch(checked = user.is_active, onCheckedChange = { onToggleActive() })
                TextButton(onClick = onEditClick) {
                    Text(
                        text = appText(en = "Edit", pt = "Editar"),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSystemInDarkTheme())
                            MaterialTheme.colorScheme.secondary
                        else
                            MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}