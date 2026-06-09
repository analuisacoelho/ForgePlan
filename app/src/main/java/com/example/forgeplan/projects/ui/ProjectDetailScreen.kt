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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.model.TaskGroup
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
import com.example.forgeplan.tasks.viewmodel.TaskGroupViewModel
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
    projectUserViewModel: ProjectUserViewModel = viewModel(),
    taskGroupViewModel: TaskGroupViewModel = viewModel()
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

    val taskGroups by taskGroupViewModel.groups.collectAsState()
    val taskGroupError by taskGroupViewModel.error.collectAsState()

    var searchText by remember { mutableStateOf("") }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var showCreateGroup by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }

    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
        taskViewModel.loadTasks(projectId)
        userViewModel.loadUsers()
        projectUserViewModel.loadProjectUsers(projectId)
        evaluationViewModel.loadEvaluations(projectId)
        taskGroupViewModel.loadGroups(projectId)
    }

    val assignedUserIds = projectUsers.map { it.user_id }
    val assignedUsers = users.filter { assignedUserIds.contains(it.id) }
    val availableUsers = users.filter { user -> !assignedUserIds.contains(user.id) }

    val progress = calculateProjectDetailProgress(tasks)

    val filteredTasks = tasks.filter { task ->
        searchText.isBlank() ||
                task.title.contains(searchText, ignoreCase = true) ||
                (task.description ?: "").contains(searchText, ignoreCase = true) ||
                (task.task_group ?: "").contains(searchText, ignoreCase = true)
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

                    val isCompleted = tasks.isNotEmpty() && tasks.all {
                        it.status?.uppercase() == "DONE"
                    }

                    val hasReview = evaluations.isNotEmpty()

                    ProjectDetailHeader(
                        project = currentProject,
                        totalTasks = tasks.size,
                        teamMembers = assignedUsers.size,
                        progress = progress,
                        isCompleted = isCompleted,
                        hasReview = hasReview,
                        onReviewProjectClick = onReviewProjectClick
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    ProjectDetailActions(
                        onCreateTaskClick = onCreateTaskClick,
                        onAddGroupClick = { showCreateGroup = !showCreateGroup }
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    taskGroupError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (isLandscape) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            Column(modifier = Modifier.weight(1.2f)) {
                                ProjectDetailTasksArea(
                                    searchText = searchText,
                                    onSearchChange = { searchText = it },
                                    tasks = filteredTasks,
                                    allTasks = tasks,
                                    taskGroups = taskGroups,
                                    showCreateGroup = showCreateGroup,
                                    newGroupName = newGroupName,
                                    onNewGroupNameChange = { newGroupName = it },
                                    onShowCreateGroupChange = { showCreateGroup = it },
                                    onCreateGroup = {
                                        if (newGroupName.isNotBlank()) {
                                            taskGroupViewModel.createGroup(
                                                projectId = projectId,
                                                name = newGroupName.trim(),
                                                onSuccess = {
                                                    newGroupName = ""
                                                    showCreateGroup = false
                                                    taskGroupViewModel.loadGroups(projectId)
                                                }
                                            )
                                        }
                                    },
                                    onTaskClick = onTaskClick
                                )
                            }

                            Column(modifier = Modifier.weight(0.8f)) {
                                ProjectDetailTeamSection(
                                    users = assignedUsers,
                                    error = projectUserError,
                                    onAddUserClick = { showAddUserDialog = true }
                                )
                            }
                        }
                    } else {
                        ProjectDetailTasksArea(
                            searchText = searchText,
                            onSearchChange = { searchText = it },
                            tasks = filteredTasks,
                            allTasks = tasks,
                            taskGroups = taskGroups,
                            showCreateGroup = showCreateGroup,
                            newGroupName = newGroupName,
                            onNewGroupNameChange = { newGroupName = it },
                            onShowCreateGroupChange = { showCreateGroup = it },
                            onCreateGroup = {
                                if (newGroupName.isNotBlank()) {
                                    taskGroupViewModel.createGroup(
                                        projectId = projectId,
                                        name = newGroupName.trim(),
                                        onSuccess = {
                                            newGroupName = ""
                                            showCreateGroup = false
                                            taskGroupViewModel.loadGroups(projectId)
                                        }
                                    )
                                }
                            },
                            onTaskClick = onTaskClick
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        ProjectDetailTeamSection(
                            users = assignedUsers,
                            error = projectUserError,
                            onAddUserClick = { showAddUserDialog = true }
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

    if (showAddUserDialog) {
        AddUserDialog(
            users = availableUsers,
            onDismiss = { showAddUserDialog = false },
            onAddUser = { user ->
                projectUserViewModel.assignUserToProject(
                    userId = user.id,
                    projectId = projectId,
                    onSuccess = {
                        projectUserViewModel.loadProjectUsers(projectId)
                    }
                )

                showAddUserDialog = false
            }
        )
    }
}

@Composable
fun ProjectDetailTasksArea(
    searchText: String,
    onSearchChange: (String) -> Unit,
    tasks: List<Task>,
    allTasks: List<Task>,
    taskGroups: List<TaskGroup>,
    showCreateGroup: Boolean,
    newGroupName: String,
    onNewGroupNameChange: (String) -> Unit,
    onShowCreateGroupChange: (Boolean) -> Unit,
    onCreateGroup: () -> Unit,
    onTaskClick: (Long) -> Unit
) {
    Text(
        text = appText(en = "Tasks", pt = "Tarefas"),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(10.dp))

    if (showCreateGroup) {
        ForgeCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = newGroupName,
                    onValueChange = onNewGroupNameChange,
                    label = {
                        Text(appText(en = "Group name", pt = "Nome do grupo"))
                    },
                    placeholder = {
                        Text(
                            appText(
                                en = "Example: Planning & Preparation",
                                pt = "Exemplo: Planeamento e Preparação"
                            )
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ForgeSecondaryButton(
                        text = appText(en = "Cancel", pt = "Cancelar"),
                        modifier = Modifier.weight(1f),
                        onClick = { onShowCreateGroupChange(false) }
                    )

                    ForgePrimaryButton(
                        text = appText(en = "Create group", pt = "Criar grupo"),
                        modifier = Modifier.weight(1f),
                        onClick = onCreateGroup
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
    }

    ForgeSearchBar(
        value = searchText,
        onValueChange = onSearchChange,
        placeholder = appText(en = "Search task", pt = "Pesquisar tarefa")
    )

    Spacer(modifier = Modifier.height(14.dp))

    ProjectDetailTaskList(
        tasks = tasks,
        allTasks = allTasks,
        taskGroups = taskGroups,
        onTaskClick = onTaskClick
    )
}

@Composable
fun ProjectDetailTaskList(
    tasks: List<Task>,
    allTasks: List<Task>,
    taskGroups: List<TaskGroup>,
    onTaskClick: (Long) -> Unit
) {
    val groupedTasks = tasks.groupBy { task ->
        task.task_group?.takeIf { it.isNotBlank() }
            ?: appText(en = "No group", pt = "Sem grupo")
    }

    val savedGroupNames = taskGroups.map { it.name }
    val existingGroupNames = groupedTasks.keys.toSet()

    val emptySavedGroups = savedGroupNames
        .filter { groupName -> groupName !in existingGroupNames }
        .distinctBy { it.lowercase() }

    if (groupedTasks.isEmpty() && emptySavedGroups.isEmpty()) {
        ForgeCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = appText(
                    en = "There are no tasks in this project.",
                    pt = "Ainda não existem tarefas neste projeto."
                ),
                modifier = Modifier.padding(14.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    } else {
        savedGroupNames.forEach { groupName ->
            val groupTasks = groupedTasks[groupName].orEmpty()

            if (groupTasks.isEmpty()) {
                ProjectDetailCustomGroup(title = groupName)
            } else {
                ProjectDetailTaskGroup(
                    title = groupName,
                    tasks = groupTasks,
                    onTaskClick = onTaskClick
                )
            }
        }

        groupedTasks
            .filterKeys { it !in savedGroupNames }
            .forEach { (groupName, groupTasks) ->
                ProjectDetailTaskGroup(
                    title = groupName,
                    tasks = groupTasks,
                    onTaskClick = onTaskClick
                )
            }
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
fun AddUserDialog(
    users: List<User>,
    onDismiss: () -> Unit,
    onAddUser: (User) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = appText(en = "Add users", pt = "Adicionar utilizadores"))
        },
        text = {
            Column {
                users.forEach { user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddUser(user) }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(user.name)
                        Text("+")
                    }
                }

                if (users.isEmpty()) {
                    Text(
                        text = appText(
                            en = "No users available",
                            pt = "Não existem utilizadores disponíveis"
                        )
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Text(
                text = appText(en = "Close", pt = "Fechar"),
                modifier = Modifier.clickable { onDismiss() }
            )
        }
    )
}

@Composable
fun ProjectDetailHeader(
    project: Project,
    totalTasks: Int,
    teamMembers: Int,
    progress: Int,
    isCompleted: Boolean,
    hasReview: Boolean,
    onReviewProjectClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
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

            Spacer(modifier = Modifier.height(8.dp))

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

        Spacer(modifier = Modifier.width(10.dp))

        if (isCompleted) {
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFB7EBC0))
                    .clickable { onReviewProjectClick() }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (hasReview) {
                        appText(en = "Review", pt = "Review")
                    } else {
                        appText(en = "Evaluate", pt = "Avaliar")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF14532D)
                )
            }
        } else if (project.priority?.uppercase() == "HIGH") {
            ForgeMiniChip(
                text = appText(en = "Urgent", pt = "Urgente"),
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        }
    }
}

@Composable
fun ProjectDetailActions(
    onCreateTaskClick: () -> Unit,
    onAddGroupClick: () -> Unit
) {
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
            text = appText(en = "Add group", pt = "Adicionar grupo"),
            modifier = Modifier.weight(1f),
            onClick = onAddGroupClick
        )
    }
}

@Composable
fun ProjectDetailCustomGroup(
    title: String
) {
    Text(
        text = "$title (0)",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(8.dp))

    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = appText(
                en = "No tasks in this group yet.",
                pt = "Ainda não existem tarefas neste grupo."
            ),
            modifier = Modifier.padding(14.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
    }

    Spacer(modifier = Modifier.height(14.dp))
}

@Composable
fun ProjectDetailTaskGroup(
    title: String,
    tasks: List<Task>,
    onTaskClick: (Long) -> Unit
) {
    if (tasks.isEmpty()) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$title (${tasks.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    tasks.forEach { task ->
        ProjectDetailTaskCard(
            task = task,
            onClick = { onTaskClick(task.id) }
        )

        Spacer(modifier = Modifier.height(10.dp))
    }

    Spacer(modifier = Modifier.height(10.dp))
}

@Composable
fun ProjectDetailTaskCard(
    task: Task,
    onClick: () -> Unit
) {
    val isDone = task.status?.uppercase() == "DONE"
    val isInProgress = task.status?.uppercase() == "IN_PROGRESS"
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

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = projectDetailTaskDates(task),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
                    )
                }

                ForgeMiniChip(
                    text = readableProjectDetailTaskStatus(task.status),
                    containerColor = when {
                        isDone -> Color(0xFFB7EBC0)
                        isInProgress -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    },
                    contentColor = when {
                        isDone -> Color(0xFF14532D)
                        isInProgress -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onSecondaryContainer
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
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.secondaryContainer
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = appText(en = "$progress% complete", pt = "$progress% concluída"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            ForgeSecondaryButton(
                text = appText(en = "Details", pt = "Detalhes"),
                modifier = Modifier.fillMaxWidth(),
                onClick = onClick
            )
        }
    }
}

@Composable
fun ProjectDetailTeamSection(
    users: List<User>,
    error: String?,
    onAddUserClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = appText(en = "Project Team", pt = "Equipa do Projeto"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        ForgePrimaryButton(
            text = appText(en = "Add", pt = "Adicionar"),
            onClick = onAddUserClick
        )
    }

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

private fun readableProjectDetailTaskStatus(status: String?): String {
    return when (status?.uppercase()) {
        "DONE" -> appText(en = "Done", pt = "Feita")
        "IN_PROGRESS" -> appText(en = "In Progress", pt = "Em progresso")
        "PENDING" -> appText(en = "To Do", pt = "Por fazer")
        else -> appText(en = "To Do", pt = "Por fazer")
    }
}

private fun projectDetailTaskDates(task: Task): String {
    val start = task.start_date ?: appText(en = "No start date", pt = "Sem data inicial")
    val end = task.end_date ?: appText(en = "No end date", pt = "Sem data final")
    return "$start → $end"
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