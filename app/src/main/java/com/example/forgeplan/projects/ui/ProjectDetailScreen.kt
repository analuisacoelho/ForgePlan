package com.example.forgeplan.projects.ui

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.model.User
import com.example.forgeplan.core.ui.components.ForgeCard
import com.example.forgeplan.core.ui.components.ForgeMiniChip
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.core.ui.components.ForgePrimaryButton
import com.example.forgeplan.core.ui.components.ForgeSecondaryButton
import com.example.forgeplan.core.ui.components.ForgeSectionTitle
import com.example.forgeplan.core.ui.components.StatusChip
import com.example.forgeplan.core.ui.components.UserAvatarChip
import com.example.forgeplan.projects.viewmodel.ProjectDetailViewModel
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
    viewModel: ProjectDetailViewModel = viewModel(),
    taskViewModel: TaskViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel(),
    projectUserViewModel: ProjectUserViewModel = viewModel()
) {
    val project by viewModel.project.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val tasks by taskViewModel.tasks.collectAsState()

    val users by userViewModel.users.collectAsState()
    val projectUsers by projectUserViewModel.projectUsers.collectAsState()
    val projectUserError by projectUserViewModel.error.collectAsState()

    val projectUserIds = projectUsers.map { it.user_id }
    val assignedProjectUsers = users.filter { projectUserIds.contains(it.id) }

    val completedTasks = tasks.filter { it.status?.uppercase() == "DONE" }
    val pendingTasks = tasks.filter { it.status?.uppercase() != "DONE" }

    val progressPercentage =
        if (tasks.isEmpty()) {
            0
        } else {
            ((completedTasks.size.toFloat() / tasks.size.toFloat()) * 100).toInt()
        }

    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
        taskViewModel.loadTasks(projectId)
        userViewModel.loadUsers()
        projectUserViewModel.loadProjectUsers(projectId)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ForgePlanTopBar(
            title = "ForgePlan",
            initials = "FP"
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp)
                .padding(bottom = 90.dp)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator()
                }

                error != null -> {
                    Text(
                        text = error ?: "Erro desconhecido",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                project == null -> {
                    Text(
                        text = "Projeto não encontrado.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                else -> {
                    ProjectHeaderCard(
                        name = project!!.name,
                        description = project!!.description ?: "Sem descrição",
                        status = project!!.status ?: "Sem estado",
                        priority = project!!.priority ?: "Sem prioridade",
                        startDate = project!!.start_date ?: "Sem data",
                        endDate = project!!.end_date ?: "Sem data",
                        progressPercentage = progressPercentage,
                        completedTasks = completedTasks.size,
                        totalTasks = tasks.size
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ForgeSecondaryButton(
                        text = "Editar projeto",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onEditProjectClick
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    ForgeSectionTitle(text = "Equipa do projeto")

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
                            text = "Ainda não existem utilizadores associados ao projeto.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            assignedProjectUsers.take(6).forEach { user ->
                                UserAvatarChip(
                                    initials = userInitials(user)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Adicionar utilizadores ao projeto",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    users.forEach { user ->
                        val isAssigned = projectUserIds.contains(user.id)

                        ProjectUserCard(
                            user = user,
                            isAssigned = isAssigned,
                            onAssignClick = {
                                projectUserViewModel.assignUserToProject(
                                    projectId = projectId,
                                    userId = user.id,
                                    onSuccess = {
                                        projectUserViewModel.loadProjectUsers(projectId)
                                    }
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    ForgePrimaryButton(
                        text = "Nova tarefa",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onCreateTaskClick
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    ForgeSectionTitle(text = "Tarefas pendentes")

                    Spacer(modifier = Modifier.height(10.dp))

                    if (pendingTasks.isEmpty()) {
                        Text(
                            text = "Não existem tarefas pendentes.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        pendingTasks.forEach { task ->
                            TaskCard(
                                task = task,
                                onClick = {
                                    onTaskClick(task.id)
                                },
                                onCompleteClick = {
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
                                }
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    ForgeSectionTitle(text = "Tarefas concluídas")

                    Spacer(modifier = Modifier.height(10.dp))

                    if (completedTasks.isEmpty()) {
                        Text(
                            text = "Ainda não existem tarefas concluídas.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        completedTasks.forEach { task ->
                            TaskCard(
                                task = task,
                                onClick = {
                                    onTaskClick(task.id)
                                },
                                onCompleteClick = {}
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
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
    ForgeCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row {
                StatusChip(text = status)

                Spacer(modifier = Modifier.width(8.dp))

                StatusChip(text = priority)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Início: $startDate",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Fim: $endDate",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row {
                ForgeMiniChip(text = "$progressPercentage% concluído")

                Spacer(modifier = Modifier.width(8.dp))

                ForgeMiniChip(text = "$completedTasks/$totalTasks tarefas")
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
    ForgeCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row {
                UserAvatarChip(initials = userInitials(user))

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleSmall
                    )

                    Text(
                        text = "@${user.username}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isAssigned) {
                Text(
                    text = "✓ Já associado",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                ForgePrimaryButton(
                    text = "Associar ao projeto",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onAssignClick?.invoke()
                    }
                )
            }
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    onClick: () -> Unit,
    onCompleteClick: () -> Unit
) {
    ForgeCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = task.description ?: "Sem descrição",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row {
                StatusChip(text = task.status ?: "Sem estado")

                Spacer(modifier = Modifier.width(8.dp))

                StatusChip(text = task.priority ?: "Sem prioridade")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Conclusão: ${task.completion_rate ?: 0}%",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Início: ${task.start_date ?: "Sem data"}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Fim: ${task.end_date ?: "Sem data"}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (task.status?.uppercase() != "DONE") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ForgeSecondaryButton(
                        text = "Editar",
                        modifier = Modifier.weight(1f),
                        onClick = onClick
                    )

                    ForgePrimaryButton(
                        text = "Concluir",
                        modifier = Modifier.weight(1f),
                        onClick = onCompleteClick
                    )
                }
            } else {
                ForgeSecondaryButton(
                    text = "Ver detalhes",
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