package com.example.forgeplan.projects.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.model.ProjectEvaluationPayload
import com.example.forgeplan.core.model.ProjectPayload
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.model.User
import com.example.forgeplan.core.ui.components.ForgeCard
import com.example.forgeplan.core.ui.components.ForgeMiniChip
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.core.ui.components.ForgePrimaryButton
import com.example.forgeplan.core.ui.components.ForgeSecondaryButton
import com.example.forgeplan.core.ui.components.StatusChip
import com.example.forgeplan.core.ui.components.UserAvatarChip
import com.example.forgeplan.projects.viewmodel.ProjectDetailViewModel
import com.example.forgeplan.projects.viewmodel.ProjectEvaluationViewModel
import com.example.forgeplan.projects.viewmodel.ProjectUserViewModel
import com.example.forgeplan.projects.viewmodel.ProjectViewModel
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
    viewModel: ProjectDetailViewModel = viewModel(),
    projectViewModel: ProjectViewModel = viewModel(),
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
    val evaluationError by evaluationViewModel.error.collectAsState()

    val tasks by taskViewModel.tasks.collectAsState()
    val users by userViewModel.users.collectAsState()
    val projectUsers by projectUserViewModel.projectUsers.collectAsState()
    val projectUserError by projectUserViewModel.error.collectAsState()

    var rating by remember { mutableStateOf(5) }
    var comment by remember { mutableStateOf("") }
    var evaluationMessage by remember { mutableStateOf<String?>(null) }

    val projectUserIds = projectUsers.map { it.user_id }
    val assignedProjectUsers = users.filter { projectUserIds.contains(it.id) }

    val completedTasks = tasks.filter { it.status?.uppercase() == "DONE" }
    val pendingTasks = tasks.filter { it.status?.uppercase() != "DONE" }

    val progressPercentage =
        if (tasks.isEmpty()) 0
        else ((completedTasks.size.toFloat() / tasks.size.toFloat()) * 100).toInt()

    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
        taskViewModel.loadTasks(projectId)
        userViewModel.loadUsers()
        projectUserViewModel.loadProjectUsers(projectId)
        evaluationViewModel.loadEvaluations(projectId)
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
            when {
                isLoading -> CircularProgressIndicator()

                error != null -> {
                    Text(
                        text = error ?: appText(en = "Unknown error", pt = "Erro desconhecido"),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                project == null -> {
                    Text(
                        text = appText(
                            en = "Project not found.",
                            pt = "Projeto não encontrado."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                else -> {
                    val currentProject = project!!

                    if (isLandscape) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                ProjectHeaderCard(
                                    name = currentProject.name,
                                    description = currentProject.description ?: appText(
                                        en = "No description",
                                        pt = "Sem descrição"
                                    ),
                                    status = currentProject.status ?: appText(
                                        en = "No status",
                                        pt = "Sem estado"
                                    ),
                                    priority = currentProject.priority ?: appText(
                                        en = "No priority",
                                        pt = "Sem prioridade"
                                    ),
                                    startDate = currentProject.start_date ?: appText(
                                        en = "No date",
                                        pt = "Sem data"
                                    ),
                                    endDate = currentProject.end_date ?: appText(
                                        en = "No date",
                                        pt = "Sem data"
                                    ),
                                    progressPercentage = progressPercentage,
                                    completedTasks = completedTasks.size,
                                    totalTasks = tasks.size
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                ForgeSecondaryButton(
                                    text = appText(en = "Edit project", pt = "Editar projeto"),
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = onEditProjectClick
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                ProjectCompletionCard(
                                    isDone = currentProject.status?.uppercase() == "DONE",
                                    rating = rating,
                                    comment = comment,
                                    evaluations = evaluations,
                                    message = evaluationMessage,
                                    error = evaluationError,
                                    onRatingSelected = {
                                        rating = it
                                        evaluationMessage = null
                                    },
                                    onCommentChange = {
                                        comment = it
                                        evaluationMessage = null
                                    },
                                    onCompleteProjectClick = {
                                        val payload = ProjectPayload(
                                            created_by_id = currentProject.created_by_id,
                                            manager_id = currentProject.manager_id,
                                            name = currentProject.name,
                                            description = currentProject.description,
                                            priority = currentProject.priority,
                                            status = "DONE",
                                            start_date = currentProject.start_date,
                                            end_date = currentProject.end_date
                                        )

                                        projectViewModel.updateProject(
                                            projectId = currentProject.id,
                                            project = payload,
                                            onSuccess = {
                                                evaluationViewModel.createEvaluation(
                                                    evaluation = ProjectEvaluationPayload(
                                                        project_id = currentProject.id,
                                                        rating = rating,
                                                        comment = comment.trim().ifBlank { null }
                                                    ),
                                                    onSuccess = {
                                                        evaluationMessage = appText(
                                                            en = "Project completed and review saved.",
                                                            pt = "Projeto concluído e avaliação guardada."
                                                        )
                                                        viewModel.loadProject(projectId)
                                                        evaluationViewModel.loadEvaluations(projectId)
                                                    }
                                                )
                                            }
                                        )
                                    }
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                ProjectTeamSection(
                                    users = users,
                                    assignedProjectUsers = assignedProjectUsers,
                                    projectUserIds = projectUserIds,
                                    projectUserError = projectUserError,
                                    onAssignUser = { user ->
                                        projectUserViewModel.assignUserToProject(
                                            projectId = projectId,
                                            userId = user.id,
                                            onSuccess = {
                                                projectUserViewModel.loadProjectUsers(projectId)
                                            }
                                        )
                                    }
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                ProjectTasksSection(
                                    pendingTasks = pendingTasks,
                                    completedTasks = completedTasks,
                                    onTaskClick = onTaskClick,
                                    onCompleteTask = { task ->
                                        val updatedTask = task.copy(
                                            status = "DONE",
                                            completion_rate = 100
                                        )

                                        taskViewModel.updateTask(
                                            task = updatedTask,
                                            onSuccess = {
                                                taskViewModel.loadTasks(task.project_id)
                                            }
                                        )
                                    },
                                    onCreateTaskClick = onCreateTaskClick
                                )
                            }
                        }
                    } else {
                        ProjectHeaderCard(
                            name = currentProject.name,
                            description = currentProject.description ?: appText(
                                en = "No description",
                                pt = "Sem descrição"
                            ),
                            status = currentProject.status ?: appText(
                                en = "No status",
                                pt = "Sem estado"
                            ),
                            priority = currentProject.priority ?: appText(
                                en = "No priority",
                                pt = "Sem prioridade"
                            ),
                            startDate = currentProject.start_date ?: appText(
                                en = "No date",
                                pt = "Sem data"
                            ),
                            endDate = currentProject.end_date ?: appText(
                                en = "No date",
                                pt = "Sem data"
                            ),
                            progressPercentage = progressPercentage,
                            completedTasks = completedTasks.size,
                            totalTasks = tasks.size
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        ForgeSecondaryButton(
                            text = appText(en = "Edit project", pt = "Editar projeto"),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onEditProjectClick
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        ProjectCompletionCard(
                            isDone = currentProject.status?.uppercase() == "DONE",
                            rating = rating,
                            comment = comment,
                            evaluations = evaluations,
                            message = evaluationMessage,
                            error = evaluationError,
                            onRatingSelected = {
                                rating = it
                                evaluationMessage = null
                            },
                            onCommentChange = {
                                comment = it
                                evaluationMessage = null
                            },
                            onCompleteProjectClick = {
                                val payload = ProjectPayload(
                                    created_by_id = currentProject.created_by_id,
                                    manager_id = currentProject.manager_id,
                                    name = currentProject.name,
                                    description = currentProject.description,
                                    priority = currentProject.priority,
                                    status = "DONE",
                                    start_date = currentProject.start_date,
                                    end_date = currentProject.end_date
                                )

                                projectViewModel.updateProject(
                                    projectId = currentProject.id,
                                    project = payload,
                                    onSuccess = {
                                        evaluationViewModel.createEvaluation(
                                            evaluation = ProjectEvaluationPayload(
                                                project_id = currentProject.id,
                                                rating = rating,
                                                comment = comment.trim().ifBlank { null }
                                            ),
                                            onSuccess = {
                                                evaluationMessage = appText(
                                                    en = "Project completed and review saved.",
                                                    pt = "Projeto concluído e avaliação guardada."
                                                )
                                                viewModel.loadProject(projectId)
                                                evaluationViewModel.loadEvaluations(projectId)
                                            }
                                        )
                                    }
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        ProjectTeamSection(
                            users = users,
                            assignedProjectUsers = assignedProjectUsers,
                            projectUserIds = projectUserIds,
                            projectUserError = projectUserError,
                            onAssignUser = { user ->
                                projectUserViewModel.assignUserToProject(
                                    projectId = projectId,
                                    userId = user.id,
                                    onSuccess = {
                                        projectUserViewModel.loadProjectUsers(projectId)
                                    }
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        ProjectTasksSection(
                            pendingTasks = pendingTasks,
                            completedTasks = completedTasks,
                            onTaskClick = onTaskClick,
                            onCompleteTask = { task ->
                                val updatedTask = task.copy(
                                    status = "DONE",
                                    completion_rate = 100
                                )

                                taskViewModel.updateTask(
                                    task = updatedTask,
                                    onSuccess = {
                                        taskViewModel.loadTasks(task.project_id)
                                    }
                                )
                            },
                            onCreateTaskClick = onCreateTaskClick
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
fun ProjectTeamSection(
    users: List<User>,
    assignedProjectUsers: List<User>,
    projectUserIds: List<Long>,
    projectUserError: String?,
    onAssignUser: (User) -> Unit
) {
    Text(
        text = appText(en = "Project team", pt = "Equipa do projeto"),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(10.dp))

    projectUserError?.let {
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))
    }

    if (assignedProjectUsers.isEmpty()) {
        Text(
            text = appText(
                en = "There are no users assigned to this project yet.",
                pt = "Ainda não existem utilizadores associados ao projeto."
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            assignedProjectUsers.take(6).forEach { user ->
                UserAvatarChip(initials = userInitials(user))
            }
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    Text(
        text = appText(
            en = "Add users to project",
            pt = "Adicionar utilizadores ao projeto"
        ),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(8.dp))

    users.take(4).forEach { user ->
        val isAssigned = projectUserIds.contains(user.id)

        ProjectUserCard(
            user = user,
            isAssigned = isAssigned,
            onAssignClick = {
                onAssignUser(user)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun ProjectTasksSection(
    pendingTasks: List<Task>,
    completedTasks: List<Task>,
    onTaskClick: (Long) -> Unit,
    onCompleteTask: (Task) -> Unit,
    onCreateTaskClick: () -> Unit
) {
    ForgePrimaryButton(
        text = appText(en = "New task", pt = "Nova tarefa"),
        modifier = Modifier.fillMaxWidth(),
        onClick = onCreateTaskClick
    )

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = appText(en = "Pending tasks", pt = "Tarefas pendentes"),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(10.dp))

    if (pendingTasks.isEmpty()) {
        Text(
            text = appText(
                en = "There are no pending tasks.",
                pt = "Não existem tarefas pendentes."
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    } else {
        pendingTasks.take(3).forEach { task ->
            ProjectDetailTaskCard(
                task = task,
                onClick = { onTaskClick(task.id) },
                onCompleteClick = { onCompleteTask(task) }
            )

            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = appText(en = "Completed tasks", pt = "Tarefas concluídas"),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(10.dp))

    if (completedTasks.isEmpty()) {
        Text(
            text = appText(
                en = "There are no completed tasks yet.",
                pt = "Ainda não existem tarefas concluídas."
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    } else {
        completedTasks.take(3).forEach { task ->
            ProjectDetailTaskCard(
                task = task,
                onClick = { onTaskClick(task.id) },
                onCompleteClick = {}
            )

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun ProjectCompletionCard(
    isDone: Boolean,
    rating: Int,
    comment: String,
    evaluations: List<com.example.forgeplan.core.model.ProjectEvaluation>,
    message: String?,
    error: String?,
    onRatingSelected: (Int) -> Unit,
    onCommentChange: (String) -> Unit,
    onCompleteProjectClick: () -> Unit
) {
    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = appText(en = "Completion and review", pt = "Conclusão e avaliação"),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isDone) {
                StatusChip(text = appText(en = "Project completed", pt = "Projeto concluído"))
            } else {
                Text(
                    text = appText(
                        en = "Review the project before marking it as completed.",
                        pt = "Avalia o projeto antes de o marcar como concluído."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (evaluations.isEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = appText(en = "Rating", pt = "Avaliação"),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..5).forEach { value ->
                        Text(
                            text = if (value <= rating) "★" else "☆",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                onRatingSelected(value)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    value = comment,
                    onValueChange = onCommentChange,
                    label = {
                        Text(appText(en = "Comment", pt = "Comentário"))
                    },
                    placeholder = {
                        Text(
                            appText(
                                en = "Write a final review of the project",
                                pt = "Escreve uma avaliação final do projeto"
                            )
                        )
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                ForgePrimaryButton(
                    text = appText(en = "Complete project", pt = "Concluir projeto"),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onCompleteProjectClick
                )
            }

            message?.let {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            error?.let {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (evaluations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = appText(en = "Saved review", pt = "Avaliação guardada"),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                evaluations.firstOrNull()?.let { evaluation ->
                    Text(
                        text = "★".repeat(evaluation.rating) +
                                "☆".repeat(5 - evaluation.rating),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = evaluation.comment ?: appText(
                            en = "No comment",
                            pt = "Sem comentário"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun ProjectHeaderCard(
    name: String,
    description: String,
    status: String,
    priority: String,
    startDate: String,
    endDate: String,
    progressPercentage: Int,
    completedTasks: Int,
    totalTasks: Int
) {
    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row {
                StatusChip(text = status)

                Spacer(modifier = Modifier.width(8.dp))

                StatusChip(text = priority)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = appText(en = "Start: $startDate", pt = "Início: $startDate"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = appText(en = "End: $endDate", pt = "Fim: $endDate"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row {
                ForgeMiniChip(
                    text = appText(
                        en = "$progressPercentage% completed",
                        pt = "$progressPercentage% concluído"
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                ForgeMiniChip(
                    text = appText(
                        en = "$completedTasks/$totalTasks tasks",
                        pt = "$completedTasks/$totalTasks tarefas"
                    )
                )
            }
        }
    }
}

@Composable
fun ProjectUserCard(
    user: User,
    isAssigned: Boolean,
    onAssignClick: (() -> Unit)? = null
) {
    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row {
                UserAvatarChip(initials = userInitials(user))

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "@${user.username}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isAssigned) {
                Text(
                    text = appText(en = "✓ Already assigned", pt = "✓ Já associado"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                ForgePrimaryButton(
                    text = appText(en = "Assign to project", pt = "Associar ao projeto"),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onAssignClick?.invoke() }
                )
            }
        }
    }
}

@Composable
fun ProjectDetailTaskCard(
    task: Task,
    onClick: () -> Unit,
    onCompleteClick: () -> Unit
) {
    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = task.description ?: appText(en = "No description", pt = "Sem descrição"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row {
                StatusChip(
                    text = task.status ?: appText(en = "No status", pt = "Sem estado")
                )

                Spacer(modifier = Modifier.width(8.dp))

                StatusChip(
                    text = task.priority ?: appText(en = "No priority", pt = "Sem prioridade")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = appText(
                    en = "Completion: ${task.completion_rate ?: 0}%",
                    pt = "Conclusão: ${task.completion_rate ?: 0}%"
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = appText(
                    en = "Start: ${task.start_date ?: appText(en = "No date", pt = "Sem data")}",
                    pt = "Início: ${task.start_date ?: appText(en = "No date", pt = "Sem data")}"
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = appText(
                    en = "End: ${task.end_date ?: appText(en = "No date", pt = "Sem data")}",
                    pt = "Fim: ${task.end_date ?: appText(en = "No date", pt = "Sem data")}"
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (task.status?.uppercase() != "DONE") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ForgeSecondaryButton(
                        text = appText(en = "Edit", pt = "Editar"),
                        modifier = Modifier.weight(1f),
                        onClick = onClick
                    )

                    ForgePrimaryButton(
                        text = appText(en = "Complete", pt = "Concluir"),
                        modifier = Modifier.weight(1f),
                        onClick = onCompleteClick
                    )
                }
            } else {
                ForgeSecondaryButton(
                    text = appText(en = "View details", pt = "Ver detalhes"),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onClick
                )
            }
        }
    }
}

private fun userInitials(user: User): String {
    return user.name
        .split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()
        .ifBlank { user.username.take(2).uppercase() }
}