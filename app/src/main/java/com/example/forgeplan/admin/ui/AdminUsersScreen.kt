package com.example.forgeplan.admin.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.admin.viewmodel.AdminViewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.model.User
import com.example.forgeplan.core.session.SessionManager
import com.example.forgeplan.core.ui.components.ForgeMiniChip
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.core.ui.components.ForgeSearchBar

@Composable
fun AdminUsersScreen(
    onBackClick: () -> Unit = {},
    onCreateUserClick: () -> Unit = {},
    onEditUserClick: (Long) -> Unit = {},
    viewModel: AdminViewModel = viewModel()
) {
    val users by viewModel.users.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    var searchText by remember { mutableStateOf("") }

    // Utilizador selecionado para confirmar toggle ativo/inativo
    var userToToggle by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }

    // Limpa mensagens depois de 2 segundos
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

    // Dialog de confirmação antes de ativar/desativar conta
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
                            pt = "${user.name} não conseguirá fazer login."
                        )
                    else
                        appText(
                            en = "${user.name} will be able to login again.",
                            pt = "${user.name} voltará a conseguir fazer login."
                        )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.toggleUserActive(user)
                    userToToggle = null
                }) {
                    Text(appText(en = "Confirm", pt = "Confirmar"))
                }
            },
            dismissButton = {
                TextButton(onClick = { userToToggle = null }) {
                    Text(appText(en = "Cancel", pt = "Cancelar"))
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ForgePlanTopBar(
                title = "ForgePlan",
                initials = SessionManager.userInitials
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                ForgeSearchBar(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = appText(en = "Search user", pt = "Pesquisar utilizador")
                )

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = appText(en = "Users", pt = "Utilizadores"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Mensagem de sucesso ou erro no topo da lista
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
                    isLoading -> CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )

                    visibleUsers.isEmpty() -> Text(
                        text = appText(en = "No users found.", pt = "Nenhum utilizador encontrado."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(bottom = 96.dp)
                        ) {
                            items(visibleUsers) { user ->
                                AdminUserCard(
                                    user = user,
                                    onEditClick = { onEditUserClick(user.id) },
                                    onToggleActive = { userToToggle = user }
                                )
                            }
                        }
                    }
                }
            }

            ForgePlanBottomBar(
                selectedItem = "Team",
                onProjectsClick = onBackClick,
                onTeamClick = {}
            )
        }

        // FAB para criar novo utilizador
        FloatingActionButton(
            onClick = onCreateUserClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 104.dp)
                .size(56.dp)
        ) {
            Text(text = "+", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
fun AdminUserCard(
    user: User,
    onEditClick: () -> Unit,
    onToggleActive: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
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
                    // Badge de role do utilizador
                    ForgeMiniChip(
                        text = user.role.lowercase().replaceFirstChar { it.uppercase() },
                        containerColor = when (user.role.uppercase()) {
                            "ADMIN" -> Color(0xFFD0E8FF)
                            "MANAGER" -> Color(0xFFE8D0FF)
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        },
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    // Badge de estado da conta
                    ForgeMiniChip(
                        text = if (user.is_active)
                            appText(en = "Active", pt = "Ativo")
                        else
                            appText(en = "Inactive", pt = "Inativo"),
                        containerColor = if (user.is_active) Color(0xFFB7EBC0) else Color(0xFFFFD0D0),
                        contentColor = if (user.is_active) Color(0xFF14532D) else Color(0xFF7F1D1D)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Toggle para ativar/desativar - pede confirmação antes de agir
                Switch(
                    checked = user.is_active,
                    onCheckedChange = { onToggleActive() }
                )

                TextButton(onClick = onEditClick) {
                    Text(
                        text = appText(en = "Edit", pt = "Editar"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}