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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.model.User
import com.example.forgeplan.core.ui.components.ForgeCard
import com.example.forgeplan.core.ui.components.ForgeMiniChip
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.core.ui.components.ForgePrimaryButton
import com.example.forgeplan.core.ui.components.ForgeSearchBar
import com.example.forgeplan.core.ui.components.ForgeSecondaryButton
import com.example.forgeplan.core.ui.components.UserAvatarChip
import com.example.forgeplan.projects.viewmodel.ProjectDetailViewModel
import com.example.forgeplan.projects.viewmodel.ProjectEvaluationViewModel
import com.example.forgeplan.projects.viewmodel.ProjectUserViewModel
import com.example.forgeplan.tasks.viewmodel.TaskViewModel
import com.example.forgeplan.tasks.viewmodel.UserViewModel

@Composable
fun ProjectDetailScreen(
    projectId: Long,
    onCreateTaskClick: () -> Unit,
    onEditProjectClick: () -> Unit,
    onTaskClick: (Long) -> Unit,
    onProjectsClick: () -> Unit = {},
    onTimelineClick: () -> Unit = {},
    onProgressClick: () -> Unit = {},
    onTeamClick: () -> Unit = {},
    onReviewProjectClick: () -> Unit = {},
    viewModel: ProjectDetailViewModel = viewModel(),
    evaluationViewModel: ProjectEvaluationViewModel = viewModel(),
    taskViewModel: TaskViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel(),
    projectUserViewModel: ProjectUserViewModel = viewModel()
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val project by viewModel.project.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val evaluations by evaluationViewModel.evaluations.collectAsState()
    val tasks by taskViewModel.tasks.collectAsState()
    val users by userViewModel.users.collectAsState()
    val projectUsers by projectUserViewModel.projectUsers.collectAsState()
    val projectUserError by projectUserViewModel.error.collectAsState()

    var searchText by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var selectedView by remember { mutableStateOf("LIST") }

    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
        taskViewModel.loadTasks(projectId)
        userViewModel.loadUsers()
        projectUserViewModel.loadProjectUsers(projectId)
        evaluationViewModel.loadEvaluations(projectId)
    }

    val assignedUserIds = projectUsers.map { it.user_id }
    val assignedUsers = users.filter { assignedUserIds.contains(it.id) }

    val completedTasks = tasks.count { it.status?.uppercase() == "DONE" }
    val progress = calculateProjectDetailProgress(tasks)

    val filteredTasks = tasks.filter { task ->
        val status = task.status?.uppercase()

        val matchesSearch =
            searchText.isBlank() ||
                    task.title.contains(searchText, ignoreCase = true) ||
                    (task.description ?: "").contains(searchText, ignoreCase = true)

        val matchesFilter =
            when (selectedFilter) {
                "TODO" -> status != "DONE" && status != "IN_PROGRESS"
                "ACTIVE" -> status == "IN_PROGRESS"
                "DONE" -> status == "DONE"
                else -> true
            }

        matchesSearch && matchesFilter
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
                .padding(bottom = 96.dp)
        ) {
            when {
                isLoading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)

                error != null -> Text(
                    text = error ?: appText(en = "Unknown error", pt = "Erro desconhecido"),
                    color = MaterialTheme.colorScheme.error
                )

                project == null -> Text(
                    text = appText(en = "Project not found.", pt = "Projeto não encontrado."),
                    color = MaterialTheme.colorScheme.onBackground
                )

                else -> {
                    val currentProject = project!!
                    val isCompleted = currentProject.status?.uppercase() == "DONE" || progress >= 100
                    val hasReview = evaluations.isNotEmpty()

                    ProjectDetailHeader(
                        project = currentProject,
                        totalTasks = tasks.size,
                        teamMembers = assignedUsers.size
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    ProjectDetailViewSelector(
                        selectedView = selectedView,
                        onSelectedView = { selectedView = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isLandscape) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            Column(modifier = Modifier.weight(0.9f)) {
                                ProjectDetailSummaryCard(
                                    project = currentProject,
                                    totalTasks = tasks.size,
                                    completedTasks = completedTasks,
                                    progress = progress
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                ProjectDetailActions(
                                    isCompleted = isCompleted,
                                    hasReview = hasReview,
                                    onCreateTaskClick = onCreateTaskClick,
                                    onEditProjectClick = onEditProjectClick,
                                    onReviewProjectClick = onReviewProjectClick
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                ProjectDetailTeamSection(
                                    users = assignedUsers,
                                    error = projectUserError
                                )
                            }

                            Column(modifier = Modifier.weight(1.2f)) {
                                ProjectDetailTasksArea(
                                    selectedView = selectedView,
                                    searchText = searchText,
                                    onSearchChange = { searchText = it },
                                    selectedFilter = selectedFilter,
                                    onFilterChange = { selectedFilter = it },
                                    tasks = filteredTasks,
                                    allTasks = tasks,
                                    onTaskClick = onTaskClick,
                                    onCompleteTask = { task ->
                                        val updatedTask = task.copy(
                                            status = "DONE",
                                            completion_rate = 100
                                        )

                                        taskViewModel.updateTask(
                                            task = updatedTask,
                                            onSuccess = { taskViewModel.loadTasks(task.project_id) }
                                        )
                                    }
                                )
                            }
                        }
                    } else {
                        ProjectDetailSummaryCard(
                            project = currentProject,
                            totalTasks = tasks.size,
                            completedTasks = completedTasks,
                            progress = progress
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        ProjectDetailActions(
                            isCompleted = isCompleted,
                            hasReview = hasReview,
                            onCreateTaskClick = onCreateTaskClick,
                            onEditProjectClick = onEditProjectClick,
                            onReviewProjectClick = onReviewProjectClick
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        ProjectDetailTasksArea(
                            selectedView = selectedView,
                            searchText = searchText,
                            onSearchChange = { searchText = it },
                            selectedFilter = selectedFilter,
                            onFilterChange = { selectedFilter = it },
                            tasks = filteredTasks,
                            allTasks = tasks,
                            onTaskClick = onTaskClick,
                            onCompleteTask = { task ->
                                val updatedTask = task.copy(
                                    status = "DONE",
                                    completion_rate = 100
                                )

                                taskViewModel.updateTask(
                                    task = updatedTask,
                                    onSuccess = { taskViewModel.loadTasks(task.project_id) }
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        ProjectDetailTeamSection(
                            users = assignedUsers,
                            error = projectUserError
                        )
                    }
                }
            }
        }

        ForgePlanBottomBar(
            selectedItem = "Projects",
            onProjectsClick = onProjectsClick,
            onTimelineClick = onTimelineClick,
            onProgressClick = onProgressClick,
            onTeamClick = onTeamClick
        )
    }
}

@Composable
fun ProjectDetailHeader(
    project: Project,
    totalTasks: Int,
    teamMembers: Int
) {
    Text(
        text = project.name,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(3.dp))

    Text(
        text = appText(
            en = "$totalTasks tasks • $teamMembers team members",
            pt = "$totalTasks tarefas • $teamMembers membros da equipa"
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
    )
}

@Composable
fun ProjectDetailViewSelector(
    selectedView: String,
    onSelectedView: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProjectDetailModeChip(
            text = appText(en = "List View", pt = "Lista"),
            selected = selectedView == "LIST",
            onClick = { onSelectedView("LIST") },
            modifier = Modifier.weight(1f)
        )

        ProjectDetailModeChip(
            text = appText(en = "Gantt View", pt = "Gantt"),
            selected = selectedView == "GANTT",
            onClick = { onSelectedView("GANTT") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ProjectDetailModeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color =
                if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ProjectDetailSummaryCard(
    project: Project,
    totalTasks: Int,
    completedTasks: Int,
    progress: Int
) {
    val isCompleted = project.status?.uppercase() == "DONE" || progress >= 100

    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = project.description ?: appText(en = "No description", pt = "Sem descrição"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                    )
                }

                if (isCompleted) {
                    ForgeMiniChip(
                        text = appText(en = "Completed", pt = "Concluído"),
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                } else if (project.priority?.uppercase() == "HIGH") {
                    ForgeMiniChip(
                        text = appText(en = "Urgent", pt = "Urgente"),
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ForgeMiniChip(text = readableProjectDetailStatus(project.status, progress))

                project.priority?.let {
                    ForgeMiniChip(text = readableProjectDetailPriority(it))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = appText(
                    en = "$completedTasks/$totalTasks tasks completed",
                    pt = "$completedTasks/$totalTasks tarefas concluídas"
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress.coerceIn(0, 100) / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (isCompleted) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.tertiary
                },
                trackColor = MaterialTheme.colorScheme.secondaryContainer
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "$progress%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ProjectDetailActions(
    isCompleted: Boolean,
    hasReview: Boolean,
    onCreateTaskClick: () -> Unit,
    onEditProjectClick: () -> Unit,
    onReviewProjectClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ForgePrimaryButton(
                text = appText(en = "New task", pt = "Nova tarefa"),
                modifier = Modifier.weight(1f),
                onClick = onCreateTaskClick
            )

            ForgeSecondaryButton(
                text = appText(en = "Edit project", pt = "Editar projeto"),
                modifier = Modifier.weight(1f),
                onClick = onEditProjectClick
            )
        }

        if (isCompleted) {
            ForgePrimaryButton(
                text = if (hasReview) {
                    appText(en = "View evaluation", pt = "Ver avaliação")
                } else {
                    appText(en = "Finish & Evaluate", pt = "Concluir e avaliar")
                },
                modifier = Modifier.fillMaxWidth(),
                onClick = onReviewProjectClick
            )
        }
    }
}

@Composable
fun ProjectDetailTasksArea(
    selectedView: String,
    searchText: String,
    onSearchChange: (String) -> Unit,
    selectedFilter: String,
    onFilterChange: (String) -> Unit,
    tasks: List<Task>,
    allTasks: List<Task>,
    onTaskClick: (Long) -> Unit,
    onCompleteTask: (Task) -> Unit
) {
    Text(
        text = appText(en = "Tasks", pt = "Tarefas"),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(10.dp))

    ForgeSearchBar(
        value = searchText,
        onValueChange = onSearchChange,
        placeholder = appText(en = "Search task", pt = "Pesquisar tarefa")
    )

    Spacer(modifier = Modifier.height(10.dp))

    ProjectDetailTaskFilterRow(
        selectedFilter = selectedFilter,
        onFilterChange = onFilterChange
    )

    Spacer(modifier = Modifier.height(14.dp))

    if (selectedView == "GANTT") {
        ProjectDetailGanttPlaceholder(tasks = tasks)
    } else {
        ProjectDetailTaskList(
            tasks = tasks,
            allTasks = allTasks,
            selectedFilter = selectedFilter,
            onTaskClick = onTaskClick,
            onCompleteTask = onCompleteTask
        )
    }
}

@Composable
fun ProjectDetailTaskList(
    tasks: List<Task>,
    allTasks: List<Task>,
    selectedFilter: String,
    onTaskClick: (Long) -> Unit,
    onCompleteTask: (Task) -> Unit
) {
    if (tasks.isEmpty()) {
        ForgeCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = appText(
                    en = "There are no tasks matching this filter.",
                    pt = "Não existem tarefas para este filtro."
                ),
                modifier = Modifier.padding(14.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    } else if (selectedFilter == "ALL") {
        ProjectDetailTaskGroup(
            title = appText(en = "To Do", pt = "Por Fazer"),
            tasks = tasks.filter {
                val status = it.status?.uppercase()
                status != "DONE" && status != "IN_PROGRESS"
            },
            onTaskClick = onTaskClick,
            onCompleteTask = onCompleteTask
        )

        ProjectDetailTaskGroup(
            title = appText(en = "In Progress", pt = "Em Progresso"),
            tasks = tasks.filter { it.status?.uppercase() == "IN_PROGRESS" },
            onTaskClick = onTaskClick,
            onCompleteTask = onCompleteTask
        )

        ProjectDetailTaskGroup(
            title = appText(en = "Completed", pt = "Concluídas"),
            tasks = tasks.filter { it.status?.uppercase() == "DONE" },
            onTaskClick = onTaskClick,
            onCompleteTask = onCompleteTask
        )
    } else {
        ProjectDetailTaskGroup(
            title = projectDetailFilterTitle(selectedFilter),
            tasks = tasks,
            onTaskClick = onTaskClick,
            onCompleteTask = onCompleteTask
        )
    }

    Spacer(modifier = Modifier.height(6.dp))

    Text(
        text = appText(
            en = "${allTasks.size} tasks in this project",
            pt = "${allTasks.size} tarefas neste projeto"
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    )
}

@Composable
fun ProjectDetailTaskGroup(
    title: String,
    tasks: List<Task>,
    onTaskClick: (Long) -> Unit,
    onCompleteTask: (Task) -> Unit
) {
    if (tasks.isEmpty()) return

    Text(
        text = "$title (${tasks.size})",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(8.dp))

    tasks.forEach { task ->
        ProjectDetailTaskCard(
            task = task,
            onClick = { onTaskClick(task.id) },
            onCompleteClick = { onCompleteTask(task) }
        )

        Spacer(modifier = Modifier.height(10.dp))
    }

    Spacer(modifier = Modifier.height(10.dp))
}

@Composable
fun ProjectDetailTaskFilterRow(
    selectedFilter: String,
    onFilterChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProjectDetailFilterChip(
            text = appText(en = "All", pt = "Todas"),
            selected = selectedFilter == "ALL",
            onClick = { onFilterChange("ALL") },
            modifier = Modifier.weight(1f)
        )

        ProjectDetailFilterChip(
            text = appText(en = "To do", pt = "Por fazer"),
            selected = selectedFilter == "TODO",
            onClick = { onFilterChange("TODO") },
            modifier = Modifier.weight(1f)
        )

        ProjectDetailFilterChip(
            text = appText(en = "Active", pt = "Ativas"),
            selected = selectedFilter == "ACTIVE",
            onClick = { onFilterChange("ACTIVE") },
            modifier = Modifier.weight(1f)
        )

        ProjectDetailFilterChip(
            text = appText(en = "Done", pt = "Feitas"),
            selected = selectedFilter == "DONE",
            onClick = { onFilterChange("DONE") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ProjectDetailFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        modifier = modifier,
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall
            )
        }
    )
}

@Composable
fun ProjectDetailTaskCard(
    task: Task,
    onClick: () -> Unit,
    onCompleteClick: () -> Unit
) {
    val isDone = task.status?.uppercase() == "DONE"
    val progress = task.completion_rate ?: 0

    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = task.description ?: appText(en = "No description", pt = "Sem descrição"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        maxLines = 2
                    )
                }

                ForgeMiniChip(
                    text = readableProjectDetailTaskStatus(task.status),
                    containerColor = if (isDone) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    contentColor = if (isDone) {
                        MaterialTheme.colorScheme.onSecondary
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress.coerceIn(0, 100) / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.secondaryContainer
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = appText(en = "$progress% complete", pt = "$progress% concluída"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ForgeSecondaryButton(
                    text = appText(en = "Details", pt = "Detalhes"),
                    modifier = Modifier.weight(1f),
                    onClick = onClick
                )

                if (!isDone) {
                    ForgePrimaryButton(
                        text = appText(en = "Complete", pt = "Concluir"),
                        modifier = Modifier.weight(1f),
                        onClick = onCompleteClick
                    )
                }
            }
        }
    }
}

@Composable
fun ProjectDetailGanttPlaceholder(
    tasks: List<Task>
) {
    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = appText(en = "Gantt View", pt = "Vista Gantt"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            tasks.take(6).forEachIndexed { index, task ->
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(5.dp))

                LinearProgressIndicator(
                    progress = { ((task.completion_rate ?: 0).coerceIn(0, 100)) / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .padding(start = (index * 8).dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.secondaryContainer
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun ProjectDetailTeamSection(
    users: List<User>,
    error: String?
) {
    Text(
        text = appText(en = "Project Team", pt = "Equipa do Projeto"),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(10.dp))

    error?.let {
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(8.dp))
    }

    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (users.isEmpty()) {
                Text(
                    text = appText(
                        en = "There are no members assigned to this project yet.",
                        pt = "Ainda não existem membros associados a este projeto."
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                users.forEach { user ->
                    ProjectDetailTeamMemberRow(user = user)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun ProjectDetailTeamMemberRow(
    user: User
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        UserAvatarChip(initials = projectDetailUserInitials(user))

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = user.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
        }

        ForgeMiniChip(text = user.role ?: appText(en = "Member", pt = "Membro"))
    }
}

private fun calculateProjectDetailProgress(tasks: List<Task>): Int {
    if (tasks.isEmpty()) return 0

    val averageCompletion = tasks.map { it.completion_rate ?: 0 }.average().toInt()
    val doneProgress =
        ((tasks.count { it.status?.uppercase() == "DONE" }.toFloat() / tasks.size.toFloat()) * 100).toInt()

    return maxOf(averageCompletion, doneProgress).coerceIn(0, 100)
}

private fun readableProjectDetailStatus(status: String?, progress: Int): String {
    return when {
        status?.uppercase() == "DONE" || progress >= 100 -> appText(en = "Completed", pt = "Concluído")
        status?.uppercase() == "IN_PROGRESS" -> appText(en = "In progress", pt = "Em progresso")
        status?.uppercase() == "PENDING" -> appText(en = "Pending", pt = "Pendente")
        else -> appText(en = "No status", pt = "Sem estado")
    }
}

private fun readableProjectDetailPriority(priority: String): String {
    return when (priority.uppercase()) {
        "HIGH" -> appText(en = "High priority", pt = "Prioridade alta")
        "MEDIUM" -> appText(en = "Medium priority", pt = "Prioridade média")
        "LOW" -> appText(en = "Low priority", pt = "Prioridade baixa")
        else -> priority
    }
}

private fun readableProjectDetailTaskStatus(status: String?): String {
    return when (status?.uppercase()) {
        "DONE" -> appText(en = "Done", pt = "Feita")
        "IN_PROGRESS" -> appText(en = "In Progress", pt = "Em progresso")
        "PENDING" -> appText(en = "To Do", pt = "Por fazer")
        else -> appText(en = "To Do", pt = "Por fazer")
    }
}

private fun projectDetailFilterTitle(filter: String): String {
    return when (filter) {
        "TODO" -> appText(en = "To Do", pt = "Por Fazer")
        "ACTIVE" -> appText(en = "In Progress", pt = "Em Progresso")
        "DONE" -> appText(en = "Completed", pt = "Concluídas")
        else -> appText(en = "Tasks", pt = "Tarefas")
    }
}

private fun projectDetailUserInitials(user: User): String {
    return user.name
        .split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()
        .ifBlank { user.username.take(2).uppercase() }
}