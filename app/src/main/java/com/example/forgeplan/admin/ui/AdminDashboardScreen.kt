package com.example.forgeplan.admin.ui

import android.content.res.Configuration
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.forgeplan.core.ui.components.ForgeSearchBar
import com.example.forgeplan.projects.ui.DashboardStatCard
import com.example.forgeplan.projects.ui.ProjectOverviewCard
import com.example.forgeplan.projects.viewmodel.ProjectViewModel

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
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val projects by viewModel.projects.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val taskRepository = remember { TaskRepository() }
    val projectUserRepository = remember { ProjectUserRepository() }

    val projectTasks = remember { mutableStateMapOf<Long, List<Task>>() }
    val projectUsers = remember { mutableStateMapOf<Long, List<ProjectUser>>() }

    var searchText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadProjects()
    }

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

    AdminScaffold(
        selectedItem = "Projects",
        onProjectsClick = {},
        onUsersClick = onUsersClick,
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
                    placeholder = appText(
                        en = "Search project",
                        pt = "Pesquisar projeto"
                    )
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
                        icon = "☑",
                        modifier = Modifier.weight(1f)
                    )

                    DashboardStatCard(
                        title = appText(en = "Active", pt = "Ativos"),
                        value = activeProjects.toString(),
                        icon = "↗",
                        modifier = Modifier.weight(1f)
                    )

                    DashboardStatCard(
                        title = appText(en = "Done", pt = "Concluídos"),
                        value = completedProjects.toString(),
                        icon = "✓",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(if (isLandscape) 14.dp else 18.dp))

                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    error != null -> {
                        Text(
                            text = error ?: appText(
                                en = "Unknown error",
                                pt = "Erro desconhecido"
                            ),
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    visibleProjects.isEmpty() -> {
                        Text(
                            text = appText(
                                en = "No projects found.",
                                pt = "Nenhum projeto encontrado."
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    else -> {
                        if (isLandscape) {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                items(visibleProjects.chunked(2)) { rowProjects ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        rowProjects.forEach { project ->
                                            ProjectOverviewCard(
                                                project = project,
                                                tasks = projectTasks[project.id] ?: emptyList(),
                                                teamCount = projectUsers[project.id]?.size ?: 0,
                                                modifier = Modifier.weight(1f),
                                                onClick = { onProjectClick(project.id) }
                                            )
                                        }

                                        if (rowProjects.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        } else {
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
                    .padding(end = 18.dp, bottom = 18.dp)
                    .size(56.dp)
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}