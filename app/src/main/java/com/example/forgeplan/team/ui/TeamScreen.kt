package com.example.forgeplan.team.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.model.ProjectUser
import com.example.forgeplan.core.model.User
import com.example.forgeplan.core.network.SupabaseApi
import com.example.forgeplan.core.repository.ProjectUserRepository
import com.example.forgeplan.core.repository.UserRepository
import com.example.forgeplan.core.ui.components.ForgeCard
import com.example.forgeplan.core.ui.components.ForgeMiniChip
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.core.ui.components.ForgeSearchBar
import com.example.forgeplan.core.ui.components.UserAvatarChip
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume

data class TeamMemberUi(
    val initials: String,
    val name: String,
    val username: String,
    val email: String,
    val role: String,
    val projects: String
)

@Composable
fun TeamScreen(
    onProjectsClick: () -> Unit = {},
    onTimelineClick: () -> Unit = {},
    onProgressClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var searchText by remember { mutableStateOf("") }

    val viewModel: TeamViewModel = viewModel()
    val users = viewModel.users.value
    val loading = viewModel.loading.value

    val projectCounts = viewModel.projectCounts.value

    val members = users
        .filter { it.is_active }
        .map { user ->
            val isOnline = viewModel.onlineUserIds.value.contains(user.id)
            TeamMemberUi(
                initials = getInitials(user.name),
                name = user.name,
                username = user.username ?: "",
                email = user.email,
                role = translatedRole(user.role),
                status = if (isOnline) appText(en = "Online", pt = "Online")
                else appText(en = "Offline", pt = "Offline"),
                projects = (projectCounts[user.id] ?: 0).toString()
            )
        }

    val filteredMembers = members.filter { member ->
        member.name.contains(searchText, ignoreCase = true) ||
                member.username.contains(searchText, ignoreCase = true) ||
                member.email.contains(searchText, ignoreCase = true) ||
                member.role.contains(searchText, ignoreCase = true) ||
                member.status.contains(searchText, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ForgePlanTopBar(
            title = "ForgePlan",
            initials = "FP"
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (isLandscape) 28.dp else 18.dp,
                    vertical = if (isLandscape) 12.dp else 16.dp
                )
                .padding(bottom = 90.dp)
        ) {
            ForgeSearchBar(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = appText(
                    en = "Search your colleagues",
                    pt = "Pesquisar colegas"
                )
            )

            Spacer(modifier = Modifier.height(if (isLandscape) 14.dp else 24.dp))

            Text(
                text = appText(en = "Your Team", pt = "A tua equipa"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(if (isLandscape) 12.dp else 16.dp))

            when {
                loading -> {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                filteredMembers.isEmpty() -> {
                    Text(
                        text = appText(
                            en = "No team members found.",
                            pt = "Nenhum membro encontrado."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                else -> {
                    filteredMembers.forEach { member ->
                        TeamMemberCard(member = member)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }

        ForgePlanBottomBar(
            selectedItem = "Team",
            onProjectsClick = onProjectsClick,
            onTimelineClick = onTimelineClick,
            onProgressClick = onProgressClick,
            onProfileClick = onProfileClick
        )
    }
}

@Composable
fun TeamMemberCard(
    member: TeamMemberUi,
    modifier: Modifier = Modifier
) {
    ForgeCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.Top
        ) {
            UserAvatarChip(initials = member.initials)

            Spacer(modifier = Modifier.width(18.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                ForgeMiniChip(text = appText(en = "Name", pt = "Nome"))

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                ForgeMiniChip(text = appText(en = "Role", pt = "Função"))

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = "• ${member.role}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                ForgeMiniChip(text = appText(en = "Currently in", pt = "Atualmente em"))

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = appText(
                        en = "${member.projects} Projects",
                        pt = "${member.projects} Projetos"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun translatedRole(role: String?): String {
    return when (role?.uppercase()) {
        "ADMIN" -> appText(en = "Admin", pt = "Administrador")
        "PROJECT_MANAGER" -> appText(en = "Project Manager", pt = "Gestor de Projeto")
        "MANAGER" -> appText(en = "Manager", pt = "Gestor")
        "USER" -> appText(en = "User", pt = "Utilizador")
        "WORKER" -> appText(en = "Worker", pt = "Trabalhador")
        else -> appText(en = "User", pt = "Utilizador")
    }
}

private fun getInitials(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }

    return when {
        parts.size >= 2 -> "${parts.first().first()}${parts.last().first()}".uppercase()
        parts.isNotEmpty() -> parts.first().take(2).uppercase()
        else -> "UN"
    }
}

class TeamViewModel : ViewModel() {

    private val userRepo = UserRepository()
    private val projectUserRepo = ProjectUserRepository()

    private val _users = mutableStateOf<List<User>>(emptyList())
    val users: State<List<User>> = _users

    private val _loading = mutableStateOf(true)
    val loading: State<Boolean> = _loading

    private val _projectCounts = mutableStateOf<Map<Long, Int>>(emptyMap())
    val projectCounts: State<Map<Long, Int>> = _projectCounts

    init {
        loadUsers()
    }

    private fun loadUsers() {
        userRepo.getUsers(
            onSuccess = { userList ->
                _users.value = userList
                _loading.value = false
                loadProjectCounts(userList.filter { it.is_active }.map { it.id })
            },
            onError = {
                _users.value = emptyList()
                _loading.value = false
            }
        )
    }

    private fun loadProjectCounts(userIds: List<Long>) {
        if (userIds.isEmpty()) return

        viewModelScope.launch {
            val counts = mutableMapOf<Long, Int>()

            userIds.forEach { userId ->
                val count = suspendCancellableCoroutine<Int> { cont ->
                    projectUserRepo.getProjectIdsByUserId(
                        userId = userId,
                        onSuccess = { cont.resume(it.size) },
                        onError = { cont.resume(0) }
                    )
                }
                counts[userId] = count
            }

            _projectCounts.value = counts
        }
    }
}