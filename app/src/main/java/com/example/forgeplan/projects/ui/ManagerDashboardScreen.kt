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
import androidx.compose.ui.graphics.Color
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
import com.example.forgeplan.core.session.SessionManager
import com.example.forgeplan.core.ui.components.ForgeMiniChip
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.core.ui.components.ForgeSearchBar
import com.example.forgeplan.notifications.viewmodel.NotificationViewModel
import com.example.forgeplan.projects.viewmodel.ProjectViewModel

@Composable
fun ManagerDashboardScreen(
    onProjectClick: (Long) -> Unit,
    onCreateProjectClick: () -> Unit,
    onEditTaskClick: (Long) -> Unit,
    onTimelineClick: () -> Unit,
    onProgressClick: () -> Unit,
    onTeamClick: () -> Unit,
    onProfileClick: () -> Unit,
    onNotificationClick: () -> Unit = {},
    viewModel: ProjectViewModel = viewModel()
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val projects by viewModel.projects.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val notifVm: NotificationViewModel = viewModel()
    val unreadCount by notifVm.unreadCount.collectAsState()

    val taskRepository = remember { TaskRepository() }
    val projectUserRepository = remember { ProjectUserRepository() }

    val projectTasks = remember { mutableStateMapOf<Long, List<Task>>() }
    val projectUsers = remember { mutableStateMapOf<Long, List<ProjectUser>>() }

    var searchText by remember { mutableStateOf("") }
    var managerProjectIds by remember { mutableStateOf<List<Long>?>(null) }

    LaunchedEffect(Unit) {
        // corre uma única vez quando o ecrã abre (Unit nunca muda)
        viewModel.loadProjects()
        notifVm.load()

        projectUserRepository.getProjectIdsByUserId(
            userId = SessionManager.userId,
            onSuccess = { ids -> managerProjectIds = ids },
            // filtra os projetos para mostrar só os que o manager gere
            onError = { managerProjectIds = emptyList() }
        )
    }

    LaunchedEffect(projects) {
        // corre sempre que a lista de projetos muda
        // carrega tarefas e utilizadores para cada projeto
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

    val managerProjects = projects.filter { project ->
        managerProjectIds?.contains(project.id) == true
    }

    val visibleProjects = managerProjects.filter { project ->
        searchText.isBlank() ||
                project.name.contains(searchText, ignoreCase = true) ||
                (project.description ?: "").contains(searchText, ignoreCase = true)
        // pesquisa local, sem chamadas à API
    }

    val activeProjects = managerProjects.count { project ->
        val tasks = projectTasks[project.id] ?: emptyList()
        calculateProjectProgress(tasks) < 100
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
                initials = SessionManager.userInitials,
                onNotificationClick = onNotificationClick,
                unreadCount = unreadCount
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        horizontal = if (isLandscape) 28.dp else 18.dp,
                        vertical = if (isLandscape) 14.dp else 16.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item {
                    ForgeSearchBar(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = appText(
                            en = "Search project",
                            pt = "Pesquisar projeto"
                        )
                    )

                    Spacer(modifier = Modifier.height(if (isLandscape) 18.dp else 22.dp))

                    Text(
                        text = appText(en = "My Projects", pt = "Os meus projetos"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = appText(
                            en = "Managing $activeProjects active projects",
                            pt = "A gerir $activeProjects projetos ativos"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    DashboardStatsRow(
                        totalProjects = managerProjects.size,
                        activeProjects = activeProjects,
                        totalTeamMembers = totalTeamMembers,
                        isLandscape = isLandscape
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    Text(
                        text = appText(en = "Projects Overview", pt = "Visão geral dos projetos"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                }

                when {
                    isLoading -> {
                        item {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    error != null -> {
                        item {
                            Text(
                                text = error ?: appText(en = "Unknown error", pt = "Erro desconhecido"),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    visibleProjects.isEmpty() -> {
                        item {
                            Text(
                                text = appText(
                                    en = "No projects found.",
                                    pt = "Nenhum projeto encontrado."
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    isLandscape -> {
                        // divide a lista em grupos de 3 para criar uma grelha de 3 colunas
                        items(visibleProjects.chunked(3)) { rowProjects ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
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

                                repeat(3 - rowProjects.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    // preenche colunas vazias na última linha para manter o alinhamento
                                }
                            }
                        }
                    }

                    else -> {
                        items(visibleProjects) { project ->
                            ProjectOverviewCard(
                                project = project,
                                tasks = projectTasks[project.id] ?: emptyList(),
                                teamCount = projectUsers[project.id]?.size ?: 0,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                onClick = { onProjectClick(project.id) }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            ForgePlanBottomBar(
                selectedItem = "Projects",
                onProjectsClick = {},
                onTimelineClick = onTimelineClick,
                onProgressClick = onProgressClick,
                onTeamClick = onTeamClick,
                onProfileClick = onProfileClick
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
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            DashboardStatCard(
                title = appText(en = "Total Projects", pt = "Total de projetos"),
                value = totalProjects.toString(),
                icon = "☑",
                modifier = Modifier.weight(1f)
            )

            DashboardStatCard(
                title = appText(en = "Active", pt = "Ativos"),
                value = activeProjects.toString(),
                icon = "↗",
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
                icon = "☑",
                modifier = Modifier.weight(1f)
            )

            DashboardStatCard(
                title = appText(en = "Active", pt = "Ativos"),
                value = activeProjects.toString(),
                icon = "↗",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun DashboardStatCard(
    title: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(84.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.size(12.dp))

            Column {
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
            .height(185.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                            containerColor = Color(0xFFB7EBC0),
                            contentColor = Color(0xFF14532D)
                        )
                    }
                    project.priority?.uppercase() == "HIGH" -> {
                        ForgeMiniChip(
                            text = appText(en = "Urgent", pt = "Urgente"),
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    }
                    project.priority?.uppercase() == "MEDIUM" -> {
                        ForgeMiniChip(
                            text = appText(en = "Medium", pt = "Média"),
                            containerColor = Color(0xFFFFF3CD),
                            contentColor = Color(0xFF7B5200)
                        )
                    }
                    project.priority?.uppercase() == "LOW" -> {
                        ForgeMiniChip(
                            text = appText(en = "Low", pt = "Baixa"),
                            containerColor = Color(0xFFB7EBC0),
                            contentColor = Color(0xFF14532D)
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
                    text = appText(en = "Overall Progress", pt = "Progresso geral"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
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
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.secondaryContainer
            )
        }
    }
}

private fun calculateProjectProgress(tasks: List<Task>): Int {
    if (tasks.isEmpty()) return 0

    val averageCompletion =
        tasks.map { it.completion_rate ?: 0 }.average().toInt()
    // média da percentagem de conclusão de cada tarefa individualmente

    val doneProgress =
        ((tasks.count { it.status?.uppercase() == "DONE" }.toFloat() / tasks.size.toFloat()) * 100).toInt()
    // percentagem de tarefas marcadas como DONE

    return maxOf(averageCompletion, doneProgress).coerceIn(0, 100)
    // usa o valor mais alto dos dois critérios
    // garante que o progresso nunca fica abaixo do número de tarefas concluídas
}
