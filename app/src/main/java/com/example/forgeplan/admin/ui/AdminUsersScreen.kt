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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.admin.viewmodel.AdminViewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.model.User
import com.example.forgeplan.core.network.SupabaseApi
import com.example.forgeplan.core.ui.components.ForgeAvatar
import com.example.forgeplan.core.ui.components.ForgeCard
import com.example.forgeplan.core.ui.components.ForgeMiniChip
import com.example.forgeplan.core.ui.components.ForgeSearchBar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    var searchText by remember { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var userToToggle by remember { mutableStateOf<User?>(null) }

    val workloadViewModel: AdminWorkloadViewModel = viewModel()
    val allUsers = workloadViewModel.users.value
    val projects = workloadViewModel.projects.value
    val workloadLoading = workloadViewModel.loading.value

    val tabs = listOf(
        appText(en = "Users", pt = "Utilizadores"),
        appText(en = "Workload", pt = "Carga")
    )

    LaunchedEffect(Unit) { viewModel.loadUsers() }

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

    AdminScaffold(
        selectedItem = "Users",
        onProjectsClick = onBackClick,
        onUsersClick = {},
        onActivityClick = onActivityClick,
        onProfileClick = onProfileClick,
        onLogout = onLogout
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Search bar + título
            Column(
                modifier = Modifier.padding(
                    horizontal = if (isLandscape) 30.dp else 18.dp,
                    vertical = if (isLandscape) 10.dp else 14.dp
                )
            ) {
                ForgeSearchBar(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = if (selectedTab == 0)
                        appText(en = "Search user", pt = "Pesquisar utilizador")
                    else
                        appText(en = "Search team", pt = "Pesquisar equipa")
                )
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> UsersTab(
                    users = visibleUsers,
                    isLoading = isLoading,
                    error = error,
                    successMessage = successMessage,
                    isLandscape = isLandscape,
                    onEditUserClick = onEditUserClick,
                    onToggleUser = { userToToggle = it },
                    onCreateUserClick = onCreateUserClick
                )
                1 -> WorkloadTab(
                    users = allUsers.filter { it.is_active }.filter { user ->
                        searchText.isBlank() ||
                                user.name.contains(searchText, ignoreCase = true) ||
                                user.email.contains(searchText, ignoreCase = true) ||
                                user.role.contains(searchText, ignoreCase = true)
                    },
                    projects = projects,
                    isLoading = workloadLoading,
                    isLandscape = isLandscape
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Tab 0 — Users
// ─────────────────────────────────────────────────────────

@Composable
private fun UsersTab(
    users: List<User>,
    isLoading: Boolean,
    error: String?,
    successMessage: String?,
    isLandscape: Boolean,
    onEditUserClick: (Long) -> Unit,
    onToggleUser: (User) -> Unit,
    onCreateUserClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (isLandscape) 30.dp else 18.dp,
                    vertical = 12.dp
                )
        ) {
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
                users.isEmpty() -> Text(
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
                            items(users.chunked(2)) { rowUsers ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowUsers.forEach { user ->
                                        AdminUserCard(
                                            user = user,
                                            onEditClick = { onEditUserClick(user.id) },
                                            onToggleActive = { onToggleUser(user) },
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
                            modifier = Modifier.padding(bottom = 80.dp)
                        ) {
                            items(users) { user ->
                                AdminUserCard(
                                    user = user,
                                    onEditClick = { onEditUserClick(user.id) },
                                    onToggleActive = { onToggleUser(user) }
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onCreateUserClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 18.dp)
                .size(56.dp)
        ) {
            Text(text = "+", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

// ─────────────────────────────────────────────────────────
// Tab 1 — Workload
// ─────────────────────────────────────────────────────────

@Composable
private fun WorkloadTab(
    users: List<User>,
    projects: List<Project>,
    isLoading: Boolean,
    isLandscape: Boolean
) {
    val projectCountByManager = projects
        .filter { it.manager_id != null && it.status?.uppercase() != "DONE" }
        .groupBy { it.manager_id!! }
        .mapValues { it.value.size }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = if (isLandscape) 30.dp else 18.dp,
                vertical = 12.dp
            )
    ) {
        when {
            isLoading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            users.isEmpty() -> Text(
                text = appText(en = "No users found.", pt = "Nenhum utilizador encontrado."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            else -> {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (isLandscape) {
                        users.chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                row.forEach { user ->
                                    WorkloadCard(
                                        user = user,
                                        projectCount = projectCountByManager[user.id] ?: 0,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    } else {
                        users.forEach { user ->
                            WorkloadCard(
                                user = user,
                                projectCount = projectCountByManager[user.id] ?: 0
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkloadCard(
    user: User,
    projectCount: Int,
    modifier: Modifier = Modifier
) {
    val isManager = user.role.uppercase() == "MANAGER"

    ForgeCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.Top
        ) {
            ForgeAvatar(
                initials = getWorkloadInitials(user.name),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.width(18.dp))

            Column(modifier = Modifier.weight(1f)) {
                ForgeMiniChip(text = appText(en = "Name", pt = "Nome"))
                Spacer(modifier = Modifier.height(5.dp))
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
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                ForgeMiniChip(
                    text = user.role.lowercase().replaceFirstChar { it.uppercase() },
                    containerColor = when (user.role.uppercase()) {
                        "ADMIN" -> Color(0xFFD0E8FF)
                        "MANAGER" -> Color(0xFFE8D0FF)
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    },
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                if (isManager) {
                    ForgeMiniChip(text = appText(en = "Workload", pt = "Carga"))
                    Spacer(modifier = Modifier.height(6.dp))
                    ForgeMiniChip(
                        text = appText(en = "$projectCount active", pt = "$projectCount ativos"),
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
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when {
                            projectCount == 0 -> appText(en = "Free", pt = "Disponível")
                            projectCount <= 2 -> appText(en = "Normal", pt = "Normal")
                            else -> appText(en = "Overloaded", pt = "Sobrecarregado")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            projectCount == 0 -> Color(0xFF14532D)
                            projectCount <= 2 -> Color(0xFF7B5200)
                            else -> Color(0xFF7F1D1D)
                        }
                    )
                } else {
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
        }
    }
}

private fun getWorkloadInitials(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts.first().first()}${parts.last().first()}".uppercase()
        parts.isNotEmpty() -> parts.first().take(2).uppercase()
        else -> "UN"
    }
}

// ─────────────────────────────────────────────────────────
// Cards existentes (mantidos)
// ─────────────────────────────────────────────────────────

@Composable
fun AdminUserCard(
    user: User,
    onEditClick: () -> Unit,
    onToggleActive: () -> Unit,
    modifier: Modifier = Modifier
) {
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

// ─────────────────────────────────────────────────────────
// ViewModel para Workload
// ─────────────────────────────────────────────────────────

class AdminWorkloadViewModel : ViewModel() {

    private val _users = mutableStateOf<List<User>>(emptyList())
    val users: State<List<User>> = _users

    private val _projects = mutableStateOf<List<Project>>(emptyList())
    val projects: State<List<Project>> = _projects

    private val _loading = mutableStateOf(true)
    val loading: State<Boolean> = _loading

    init {
        loadData()
    }

    private fun loadData() {
        SupabaseApi.service.getUsers()
            .enqueue(object : Callback<List<User>> {
                override fun onResponse(call: Call<List<User>>, response: Response<List<User>>) {
                    _users.value = response.body() ?: emptyList()
                    loadProjects()
                }
                override fun onFailure(call: Call<List<User>>, t: Throwable) {
                    _users.value = emptyList()
                    _loading.value = false
                }
            })
    }

    private fun loadProjects() {
        SupabaseApi.service.getProjects()
            .enqueue(object : Callback<List<Project>> {
                override fun onResponse(call: Call<List<Project>>, response: Response<List<Project>>) {
                    _projects.value = response.body() ?: emptyList()
                    _loading.value = false
                }
                override fun onFailure(call: Call<List<Project>>, t: Throwable) {
                    _projects.value = emptyList()
                    _loading.value = false
                }
            })
    }
}