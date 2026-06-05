package com.example.forgeplan.admin.ui

import android.content.res.Configuration
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.model.ProjectUser
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.repository.ProjectUserRepository
import com.example.forgeplan.core.repository.TaskRepository
import com.example.forgeplan.core.session.SessionManager
import com.example.forgeplan.core.ui.components.ForgeSideMenuScaffold
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.core.ui.components.ForgeSearchBar
import com.example.forgeplan.core.ui.components.SideMenuItem
import com.example.forgeplan.projects.ui.DashboardStatCard
import com.example.forgeplan.projects.ui.ProjectOverviewCard
import com.example.forgeplan.projects.viewmodel.ProjectViewModel
import kotlinx.coroutines.launch

@Composable
fun AdminDashboardScreen(
    onProjectClick: (Long) -> Unit = {},
    onCreateProjectClick: () -> Unit = {},
    onUsersClick: () -> Unit = {},
    onActivityClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: ProjectViewModel = viewModel()
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val projects by viewModel.projects.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val taskRepository = remember { TaskRepository() }
    val projectUserRepository = remember { ProjectUserRepository() }
    val projectTasks = remember { mutableStateMapOf<Long, List<Task>>() }
    val projectUsers = remember { mutableStateMapOf<Long, List<ProjectUser>>() }
    var searchText by remember { mutableStateOf("") }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.loadProjects() }

    LaunchedEffect(projects) {
        projects.forEach { project ->
            taskRepository.getTasksByProjectId(
                projectId = project.id,
                onSuccess = { tasks -> projectTasks[project.id] = tasks },
                onError = { projectTasks[project.id] = emptyList() }
            )
            projectUserRepository.getProjectUsersByProjectId(
                projectId = project.id,
                onSuccess = { users -> projectUsers[project.id] = users },
                onError = { projectUsers[project.id] = emptyList() }
            )
        }
    }

    val visibleProjects = projects.filter { project ->
        searchText.isBlank() ||
                project.name.contains(searchText, ignoreCase = true) ||
                (project.description ?: "").contains(searchText, ignoreCase = true)
    }

    val completedProjects = projects.count { project ->
        val tasks = projectTasks[project.id] ?: emptyList()
        tasks.isNotEmpty() && tasks.all { it.status?.uppercase() == "DONE" }
    }
    val activeProjects = projects.count { project ->
        val tasks = projectTasks[project.id] ?: emptyList()
        tasks.any { it.status?.uppercase() != "DONE" }
    }

    // Side menu com os itens de navegação do Admin
    ForgeSideMenuScaffold(
        selectedItem = "Projects",
        drawerState = drawerState,
        onLogout = {
            SessionManager.clear()
            onLogout()
        },
        items = listOf(
            SideMenuItem(appText(en = "Projects", pt = "Projetos"), "☑", "Projects") {},
            SideMenuItem(appText(en = "Users", pt = "Utilizadores"), "♧", "Users", onUsersClick),
            SideMenuItem(appText(en = "Activity", pt = "Atividade"), "▤", "Activity", onActivityClick),
            SideMenuItem(appText(en = "Profile", pt = "Perfil"), "◎", "Profile", onProfileClick)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Clicar nas iniciais abre o side menu
                ForgePlanTopBar(
                    title = "ForgePlan",
                    initials = SessionManager.userInitials,
                    onAvatarClick = { scope.launch { drawerState.open() } }
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            horizontal = if (isLandscape) 30.dp else 18.dp,
                            vertical = if (isLandscape) 12.dp else 16.dp
                        )
                ) {
                    ForgeSearchBar(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = appText(en = "Search project", pt = "Pesquisar projeto")
                    )

                    Spacer(modifier = Modifier.height(if (isLandscape) 14.dp else 22.dp))

                    Text(
                        text = appText(en = "All Projects", pt = "Todos os Projetos"),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DashboardStatCard(
                            title = appText(en = "Total", pt = "Total"),
                            value = projects.size.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        DashboardStatCard(
                            title = appText(en = "Active", pt = "Ativos"),
                            value = activeProjects.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        DashboardStatCard(
                            title = appText(en = "Done", pt = "Concluídos"),
                            value = completedProjects.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(if (isLandscape) 14.dp else 18.dp))

                    when {
                        isLoading -> CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                        error != null -> Text(
                            text = error ?: appText(en = "Unknown error", pt = "Erro desconhecido"),
                            color = MaterialTheme.colorScheme.error
                        )
                        visibleProjects.isEmpty() -> Text(
                            text = appText(en = "No projects found.", pt = "Nenhum projeto encontrado."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        else -> {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                items(visibleProjects) { project ->
                                    ProjectOverviewCard(
                                        project = project,
                                        tasks = projectTasks[project.id] ?: emptyList(),
                                        teamCount = projectUsers[project.id]?.size ?: 0,
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = { onProjectClick(project.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = onCreateProjectClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = if (isLandscape) 30.dp else 18.dp, bottom = 18.dp)
                    .size(56.dp)
            ) {
                Text(text = "+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}