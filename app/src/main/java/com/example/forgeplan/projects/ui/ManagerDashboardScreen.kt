package com.example.forgeplan.projects.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.model.ProjectUser
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.repository.ProjectUserRepository
import com.example.forgeplan.core.repository.TaskRepository
import com.example.forgeplan.core.ui.components.ForgeMiniChip
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.core.ui.components.ForgeSearchBar
import com.example.forgeplan.projects.viewmodel.ProjectViewModel

@Composable
fun ManagerDashboardScreen(
    onProjectClick: (Long) -> Unit,
    onCreateProjectClick: () -> Unit,
    onEditTaskClick: (Long) -> Unit,
    onTimelineClick: () -> Unit,
    onProgressClick: () -> Unit,
    onTeamClick: () -> Unit,
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

    LaunchedEffect(Unit) {
        viewModel.loadProjects()
    }

    LaunchedEffect(projects) {
        projects.forEach { project ->
            taskRepository.getTasksByProjectId(
                projectId = project.id,
                onSuccess = { tasks ->
                    projectTasks[project.id] = tasks
                },
                onError = {
                    projectTasks[project.id] = emptyList()
                }
            )

            projectUserRepository.getProjectUsersByProjectId(
                projectId = project.id,
                onSuccess = { users ->
                    projectUsers[project.id] = users
                },
                onError = {
                    projectUsers[project.id] = emptyList()
                }
            )
        }
    }

    val visibleProjects = projects.filter { project ->
        searchText.isBlank() ||
                project.name.contains(searchText, ignoreCase = true) ||
                (project.description ?: "").contains(searchText, ignoreCase = true)
    }

    val activeProjects = projects.count { project ->
        val tasks = projectTasks[project.id] ?: emptyList()
        val progress = calculateProjectProgress(tasks)
        progress < 100
    }

    val totalTeamMembers = projectUsers.values.flatten().map { it.user_id }.distinct().size

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ForgePlanTopBar(
                title = "ForgePlan",
                initials = "FP"
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
                    placeholder = appText(
                        en = "Search project",
                        pt = "Pesquisar projeto"
                    )
                )

                Spacer(modifier = Modifier.height(if (isLandscape) 14.dp else 22.dp))

                Text(
                    text = appText(en = "Projects", pt = "Projetos"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(14.dp))

                DashboardStatsRow(
                    totalProjects = projects.size,
                    activeProjects = activeProjects,
                    totalTeamMembers = totalTeamMembers,
                    isLandscape = isLandscape
                )

                Spacer(modifier = Modifier.height(if (isLandscape) 14.dp else 18.dp))

                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    error != null -> {
                        Text(
                            text = error ?: appText(en = "Unknown error", pt = "Erro desconhecido"),
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

                    isLandscape -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.padding(bottom = 96.dp)
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
                    }

                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(bottom = 96.dp)
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

            ForgePlanBottomBar(
                selectedItem = "Projects",
                onTimelineClick = onTimelineClick,
                onProgressClick = onProgressClick,
                onTeamClick = onTeamClick
            )
        }

        FloatingActionButton(
            onClick = onCreateProjectClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = if (isLandscape) 30.dp else 18.dp,
                    bottom = if (isLandscape) 88.dp else 104.dp
                )
                .size(56.dp)
        ) {
            Text(
                text = "+",
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

@Composable
fun DashboardStatsRow(
    totalProjects: Int,
    activeProjects: Int,
    totalTeamMembers: Int,
    isLandscape: Boolean
) {
    if (isLandscape) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardStatCard(
                title = appText(en = "Total projects", pt = "Total de projetos"),
                value = totalProjects.toString(),
                modifier = Modifier.weight(1f)
            )

            DashboardStatCard(
                title = appText(en = "Active", pt = "Ativos"),
                value = activeProjects.toString(),
                modifier = Modifier.weight(1f)
            )

            DashboardStatCard(
                title = appText(en = "Team members", pt = "Membros da equipa"),
                value = totalTeamMembers.toString(),
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DashboardStatCard(
                title = appText(en = "Projects", pt = "Projetos"),
                value = totalProjects.toString(),
                modifier = Modifier.weight(1f)
            )

            DashboardStatCard(
                title = appText(en = "Active", pt = "Ativos"),
                value = activeProjects.toString(),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun DashboardStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(78.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
fun ProjectOverviewCard(
    project: Project,
    tasks: List<Task>,
    teamCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val completedTasks = tasks.count { it.status?.uppercase() == "DONE" }
    val pendingTasks = tasks.count { it.status?.uppercase() != "DONE" }
    val progress = calculateProjectProgress(tasks)

    val isCompleted = progress >= 100
    val isUrgent = project.priority?.uppercase() == "HIGH" && !isCompleted

    Card(
        modifier = modifier
            .height(190.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = project.description ?: appText(
                            en = "No description",
                            pt = "Sem descrição"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        maxLines = 2
                    )
                }

                when {
                    isCompleted -> {
                        ForgeMiniChip(
                            text = appText(en = "Completed", pt = "Concluído"),
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    }

                    isUrgent -> {
                        ForgeMiniChip(
                            text = appText(en = "Urgent", pt = "Urgente"),
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ForgeMiniChip(
                    text = appText(
                        en = "$pendingTasks pending",
                        pt = "$pendingTasks pendentes"
                    )
                )

                ForgeMiniChip(
                    text = appText(
                        en = "$completedTasks done",
                        pt = "$completedTasks feitas"
                    )
                )

                ForgeMiniChip(
                    text = appText(
                        en = "$teamCount users",
                        pt = "$teamCount utilizadores"
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = appText(en = "Progress", pt = "Progresso"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "$progress%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progress.coerceIn(0, 100) / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (isCompleted) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.tertiary
                },
                trackColor = MaterialTheme.colorScheme.secondaryContainer
            )
        }
    }
}

private fun calculateProjectProgress(tasks: List<Task>): Int {
    if (tasks.isEmpty()) return 0

    val averageCompletion =
        tasks.map { it.completion_rate ?: 0 }.average().toInt()

    val doneProgress =
        ((tasks.count { it.status?.uppercase() == "DONE" }.toFloat() / tasks.size.toFloat()) * 100).toInt()

    return maxOf(averageCompletion, doneProgress).coerceIn(0, 100)
}