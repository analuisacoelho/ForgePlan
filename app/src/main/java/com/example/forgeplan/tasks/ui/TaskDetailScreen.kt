package com.example.forgeplan.tasks.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonAddAlt1
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.model.ProjectUser
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.model.TaskAttachment
import com.example.forgeplan.core.model.TaskLog
import com.example.forgeplan.core.model.User
import com.example.forgeplan.core.network.SupabaseApi
import com.example.forgeplan.core.repository.TaskLogRepository
import com.example.forgeplan.core.ui.components.ForgeMiniChip
import com.example.forgeplan.core.ui.components.ForgePrimaryButton
import com.example.forgeplan.core.ui.components.UserAvatarChip
import com.example.forgeplan.projects.viewmodel.ProjectViewModel
import com.example.forgeplan.tasks.viewmodel.TaskAssignmentViewModel
import com.example.forgeplan.tasks.viewmodel.TaskViewModel
import com.example.forgeplan.tasks.viewmodel.UserViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun TaskDetailScreen(
    taskId: Long,
    onBackClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    isReadOnly: Boolean = false,
    taskViewModel: TaskViewModel = viewModel(),
    projectViewModel: ProjectViewModel = viewModel(),
    assignmentViewModel: TaskAssignmentViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {
    val task by taskViewModel.selectedTask.collectAsState()
    val isLoading by taskViewModel.isLoading.collectAsState()
    val error by taskViewModel.error.collectAsState()

    val projects by projectViewModel.projects.collectAsState()
    val assignments by assignmentViewModel.assignments.collectAsState()
    val users by userViewModel.users.collectAsState()
    val assignmentError by assignmentViewModel.error.collectAsState()

    val attachments = remember { mutableStateListOf<TaskAttachment>() }
    val taskLogs = remember { mutableStateListOf<TaskLog>() }
    val taskLogRepository = remember { TaskLogRepository() }

    var showManageWorkersDialog by remember { mutableStateOf(false) }

    LaunchedEffect(taskId) {
        taskViewModel.loadTaskById(taskId)
        projectViewModel.loadProjects()
        assignmentViewModel.loadAssignments(taskId)
        userViewModel.loadUsers()

        loadTaskDetailAttachments(
            taskId = taskId,
            onSuccess = {
                attachments.clear()
                attachments.addAll(it)
            }
        )

        taskLogRepository.getLogsByTaskId(
            taskId = taskId,
            onSuccess = { logs ->
                taskLogs.clear()
                taskLogs.addAll(logs.filter { !it.notes.isNullOrBlank() })
                // só mostra logs que têm observações escritas pelo trabalhador
                // logs de atualização de progresso sem nota são ignorados
            },
            onError = {
                taskLogs.clear()
            }
        )
    }

    val assignedUserIds = assignments.map { it.user_id }
    val assignedUsers = users.filter { assignedUserIds.contains(it.id) }
    val project = projects.firstOrNull { it.id == task?.project_id }

    val projectUserIds = remember { mutableStateListOf<Long>() }

    LaunchedEffect(project?.id) {
        project?.id?.let { projectId ->
            SupabaseApi.service.getProjectUsersByProjectId("eq.$projectId")
                .enqueue(object : Callback<List<ProjectUser>> {
                    override fun onResponse(
                        call: Call<List<ProjectUser>>,
                        response: Response<List<ProjectUser>>
                    ) {
                        projectUserIds.clear()
                        projectUserIds.addAll(
                            response.body()?.map { it.user_id } ?: emptyList()
                        )

                    }
                    override fun onFailure(call: Call<List<ProjectUser>>, t: Throwable) {
                        projectUserIds.clear()
                        // carrega os IDs dos membros do projeto para filtrar quem pode ser atribuído
                    }
                })
        }
    }

    val availableUsers = users.filter { user ->
        user.role?.uppercase() == "USER" && projectUserIds.contains(user.id)
    // só utilizadores com role USER, que sejam do mesmo projeto
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TaskDetailTopBar(onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                isLoading && task == null -> {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                error != null -> {
                    Text(
                        text = error ?: appText(en = "Unknown error", pt = "Erro desconhecido"),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                task == null -> {
                    Text(
                        text = appText(en = "Task not found.", pt = "Tarefa não encontrada."),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                else -> {
                    TaskDetailContent(
                        task = task!!,
                        project = project,
                        assignedUsers = assignedUsers,
                        allUsers = users,
                        assignmentError = assignmentError,
                        attachments = attachments,
                        taskLogs = taskLogs,
                        isReadOnly = isReadOnly,
                        onManageWorkersClick = { showManageWorkersDialog = true },
                        onEditClick = { onEditClick(taskId) }
                    )
                }
            }
        }
    }

    if (showManageWorkersDialog && !isReadOnly) {
        ManageTaskWorkersDialog(
            taskId = taskId,
            users = availableUsers,
            assignedUserIds = assignedUserIds,
            assignmentViewModel = assignmentViewModel,
            onDismiss = { showManageWorkersDialog = false }
        )
    }
}

@Composable
fun TaskDetailTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(MaterialTheme.colorScheme.tertiary)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "‹",
            color = MaterialTheme.colorScheme.onTertiary,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.clickable { onBackClick() }
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appText(en = "Task Details", pt = "Detalhes da Tarefa"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiary
            )
            Text(
                text = "ForgePlan",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
fun TaskDetailContent(
    task: Task,
    project: Project?,
    assignedUsers: List<User>,
    allUsers: List<User>,
    assignmentError: String?,
    attachments: List<TaskAttachment>,
    taskLogs: List<TaskLog>,
    isReadOnly: Boolean = false,
    onManageWorkersClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
    ) {
        TaskMainInfoCard(task = task, project = project)

        Spacer(modifier = Modifier.height(18.dp))

        TaskAssignedWorkersCard(
            users = assignedUsers,
            error = assignmentError,
            isReadOnly = isReadOnly,
            onManageClick = onManageWorkersClick
        )

        Spacer(modifier = Modifier.height(18.dp))

        TaskWorkerObservationsCard(
            taskLogs = taskLogs,
            users = allUsers
        )

        Spacer(modifier = Modifier.height(18.dp))

        TaskEvidenceCard(attachments = attachments)

        if (!isReadOnly) {
            Spacer(modifier = Modifier.height(22.dp))
            ForgePrimaryButton(
                text = appText(en = "Edit task", pt = "Editar tarefa"),
                modifier = Modifier.fillMaxWidth(),
                onClick = onEditClick
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ManageTaskWorkersDialog(
    taskId: Long,
    users: List<User>,
    assignedUserIds: List<Long>,
    assignmentViewModel: TaskAssignmentViewModel,
    onDismiss: () -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    val selectedUserIds = remember { mutableStateListOf<Long>() }

    LaunchedEffect(assignedUserIds) {
        selectedUserIds.clear()
        selectedUserIds.addAll(assignedUserIds)
    }

    val filteredUsers = users.filter { user ->
        searchText.isBlank() ||
                user.name.contains(searchText, ignoreCase = true) ||
                user.email.contains(searchText, ignoreCase = true) ||
                (user.role ?: "").contains(searchText, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = appText(en = "Assign Users", pt = "Gerir trabalhadores"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "×",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.clickable { onDismiss() }
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = appText(
                        en = "Select workers for this task",
                        pt = "Seleciona os trabalhadores desta tarefa"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            appText(
                                en = "Search by name or role...",
                                pt = "Pesquisar por nome ou função..."
                            )
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    filteredUsers.forEach { user ->
                        val isSelected = selectedUserIds.contains(user.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) selectedUserIds.remove(user.id)
                                    else selectedUserIds.add(user.id)
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (!selectedUserIds.contains(user.id)) selectedUserIds.add(user.id)
                                    } else {
                                        selectedUserIds.remove(user.id)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            UserAvatarChip(initials = taskDetailUserInitials(user))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${user.role ?: appText(en = "Worker", pt = "Trabalhador")} • ${user.email}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = appText(
                        en = "${selectedUserIds.size} users selected",
                        pt = "${selectedUserIds.size} utilizadores selecionados"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val originalIds = assignedUserIds.toSet()
                    val newIds = selectedUserIds.toSet()
                    val usersToAdd = newIds - originalIds // utilizadores a adicionar
                    val usersToRemove = originalIds - newIds // utilizadores a remover

                    // só faz chamadas à API para as diferenças — não recria todas as atribuições
                    usersToAdd.forEach { userId ->
                        assignmentViewModel.assignUserToTask(
                            taskId = taskId,
                            userId = userId,
                            onSuccess = { assignmentViewModel.loadAssignments(taskId) }
                        )
                    }
                    usersToRemove.forEach { userId ->
                        assignmentViewModel.removeUserFromTask(
                            taskId = taskId,
                            userId = userId,
                            onSuccess = { assignmentViewModel.loadAssignments(taskId) }
                        )
                    }
                    assignmentViewModel.loadAssignments(taskId)
                    onDismiss()
                }
            ) {
                Text(appText(en = "Confirm Assignment", pt = "Confirmar"))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(appText(en = "Cancel", pt = "Cancelar"))
            }
        }
    )
}

@Composable
fun TaskMainInfoCard(task: Task, project: Project?) {
    val progress = task.completion_rate ?: 0
    val isDone = task.status?.uppercase() == "DONE"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = appText(
                            en = "Project: ${project?.name ?: "Project ${task.project_id}"}",
                            pt = "Projeto: ${project?.name ?: "Projeto ${task.project_id}"}"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                    )
                }
                ForgeMiniChip(
                    text = readableTaskDetailStatus(task.status),
                    containerColor = if (isDone) Color(0xFFB7EBC0) else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (isDone) Color(0xFF14532D) else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            TaskDescriptionBox(
                text = task.description ?: appText(en = "No description", pt = "Sem descrição")
            )

            Spacer(modifier = Modifier.height(26.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = appText(en = "Overall Progress", pt = "Progresso geral"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
                Text(
                    text = "$progress%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress.coerceIn(0, 100) / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (isDone) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.secondaryContainer
            )

            Spacer(modifier = Modifier.height(28.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                TaskDetailIconInfo(
                    icon = "▣",
                    label = appText(en = "Start Date", pt = "Data de início"),
                    value = task.start_date ?: appText(en = "No date", pt = "Sem data"),
                    modifier = Modifier.weight(1f)
                )
                TaskDetailIconInfo(
                    icon = "▣",
                    label = appText(en = "End Date", pt = "Data de fim"),
                    value = task.end_date ?: appText(en = "No date", pt = "Sem data"),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                TaskDetailIconInfo(
                    icon = "⌖",
                    label = appText(en = "Group", pt = "Grupo"),
                    value = task.task_group ?: appText(en = "No group", pt = "Sem grupo"),
                    modifier = Modifier.weight(1f)
                )
                TaskDetailIconInfo(
                    icon = "◷",
                    label = appText(en = "Priority", pt = "Prioridade"),
                    value = task.priority ?: appText(en = "No priority", pt = "Sem prioridade"),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun TaskDescriptionBox(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = appText(en = "Description", pt = "Descrição"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f)
            )
        }
    }
}

@Composable
fun TaskDetailIconInfo(
    icon: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.Top) {
        Text(
            text = icon,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun TaskAssignedWorkersCard(
    users: List<User>,
    error: String?,
    isReadOnly: Boolean = false,
    onManageClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = appText(en = "Assigned Workers", pt = "Trabalhadores atribuídos"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (!isReadOnly) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.background,
                        tonalElevation = 1.dp,
                        modifier = Modifier.clickable { onManageClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PersonAddAlt1,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = appText(en = "Manage", pt = "Gerir"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (users.isEmpty()) {
                Text(
                    text = appText(
                        en = "No workers assigned to this task.",
                        pt = "Ainda não existem trabalhadores associados a esta tarefa."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            } else {
                users.forEach { user ->
                    TaskAssignedWorkerRow(user = user)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun TaskAssignedWorkerRow(user: User) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatarChip(initials = taskDetailUserInitials(user))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = user.role ?: appText(en = "Worker", pt = "Trabalhador"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.65f)
                )
            }
        }
    }
}

@Composable
fun TaskWorkerObservationsCard(taskLogs: List<TaskLog>, users: List<User>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = appText(en = "Worker Observations", pt = "Observações dos trabalhadores"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(22.dp))
            if (taskLogs.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(modifier = Modifier.padding(14.dp)) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(62.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = appText(
                                en = "No observations recorded yet.",
                                pt = "Ainda não existem observações registadas."
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                        )
                    }
                }
            } else {
                taskLogs.forEach { log ->
                    val author = users.firstOrNull { it.id == log.user_id }
                    TaskLogObservationRow(log = log, author = author)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun TaskLogObservationRow(log: TaskLog, author: User?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (author != null) {
                        UserAvatarChip(initials = taskDetailUserInitials(author))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = author?.name ?: appText(en = "Unknown user", pt = "Utilizador desconhecido"),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatLogDate(log.created_at).ifBlank { log.log_date ?: "" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.65f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = log.notes ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.82f)
                )
                if (!log.location.isNullOrBlank() || log.minutes_spent != null || log.completion_rate != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = listOfNotNull(
                            log.location,
                            log.minutes_spent?.let { "$it min" },
                            log.completion_rate?.let { "$it%" }
                        ).joinToString(" • "),
                        // listOfNotNull filtra automaticamente os campos null
                        // mostra só os metadados que existem: localização, tempo gasto e progresso
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.65f)
                    )
                }
            }
        }
    }
}

@Composable
fun TaskEvidenceCard(attachments: List<TaskAttachment>) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = appText(en = "Evidence & Documentation", pt = "Evidências e documentação"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(18.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = appText(en = "Files uploaded", pt = "Ficheiros carregados"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = appText(
                            en = "${attachments.size} ${if (attachments.size == 1) "file" else "files"}",
                            pt = "${attachments.size} ${if (attachments.size == 1) "ficheiro" else "ficheiros"}"
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (attachments.isEmpty()) {
                Text(
                    text = appText(
                        en = "No files uploaded yet.",
                        pt = "Ainda não existem ficheiros carregados."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            } else {
                attachments.forEach { attachment ->
                    TaskEvidenceFileRow(
                        fileName = attachment.file_name ?: appText(en = "Attachment", pt = "Anexo"),
                        onClick = { openTaskAttachment(context = context, attachment = attachment) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun TaskEvidenceFileRow(fileName: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "▤",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            Text(
                text = appText(en = "Open", pt = "Abrir"),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun readableTaskDetailStatus(status: String?): String {
    return when (status?.uppercase()) {
        "DONE" -> appText(en = "Done", pt = "Feita")
        "IN_PROGRESS" -> appText(en = "In Progress", pt = "Em progresso")
        "PENDING" -> appText(en = "To Do", pt = "Por fazer")
        else -> appText(en = "To Do", pt = "Por fazer")
    }
}

private fun taskDetailUserInitials(user: User): String {
    return user.name
        .split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()
        .ifBlank { user.username.take(2).uppercase() }
}

private fun formatLogDate(value: String?): String {
    return value
        ?.replace("T", " ")
        ?.substringBefore(".")
        ?.take(16)
        ?: ""
}

private fun openTaskAttachment(context: Context, attachment: TaskAttachment) {
    val fileUrl = attachment.file_url
    if (fileUrl.isNullOrBlank()) {
        Toast.makeText(context, "Ficheiro indisponível.", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val uri = Uri.parse(fileUrl)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, attachment.file_type ?: "*/*")
            // file_type define qual app abre o ficheiro
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open attachment"))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Não existe nenhuma app para abrir este ficheiro.", Toast.LENGTH_SHORT).show()
        // nenhuma app instalada suporta este tipo de ficheiro
    } catch (e: Exception) {
        Toast.makeText(context, "Não foi possível abrir o ficheiro.", Toast.LENGTH_SHORT).show()
        // erro genérico — URI inválido, permissões, etc.
    }
}

private fun loadTaskDetailAttachments(taskId: Long, onSuccess: (List<TaskAttachment>) -> Unit) {
    SupabaseApi.service.getTaskAttachmentsByTaskId("eq.$taskId")
        .enqueue(object : Callback<List<TaskAttachment>> {
            override fun onResponse(call: Call<List<TaskAttachment>>, response: Response<List<TaskAttachment>>) {
                onSuccess(response.body() ?: emptyList())
            }
            override fun onFailure(call: Call<List<TaskAttachment>>, t: Throwable) {
                onSuccess(emptyList())
            }
        })
}