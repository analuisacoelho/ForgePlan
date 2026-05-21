package com.example.forgeplan.tasks.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.model.User
import com.example.forgeplan.core.ui.components.ForgeCard
import com.example.forgeplan.core.ui.components.ForgeOutlinedCard
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.core.ui.components.ForgePrimaryButton
import com.example.forgeplan.core.ui.components.ForgeSectionTitle
import com.example.forgeplan.core.ui.components.UserAvatarChip
import com.example.forgeplan.tasks.viewmodel.TaskAssignmentViewModel
import com.example.forgeplan.tasks.viewmodel.TaskDependencyViewModel
import com.example.forgeplan.tasks.viewmodel.TaskViewModel
import com.example.forgeplan.tasks.viewmodel.UserViewModel

@Composable
fun EditTaskScreen(
    taskId: Long,
    onTaskUpdated: () -> Unit,
    viewModel: TaskViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel(),
    assignmentViewModel: TaskAssignmentViewModel = viewModel(),
    dependencyViewModel: TaskDependencyViewModel = viewModel()
) {
    val selectedTask by viewModel.selectedTask.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val tasks by viewModel.tasks.collectAsState()

    val users by userViewModel.users.collectAsState()
    val assignments by assignmentViewModel.assignments.collectAsState()
    val assignmentError by assignmentViewModel.error.collectAsState()

    val dependencies by dependencyViewModel.dependencies.collectAsState()
    val dependencyError by dependencyViewModel.error.collectAsState()

    val assignedUserIds = assignments.map { it.user_id }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("") }
    var completionRate by remember { mutableStateOf("") }

    var selectedDependencyTask by remember { mutableStateOf<Task?>(null) }
    var dependencyMessage by remember { mutableStateOf<String?>(null) }
    var assignmentMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(taskId) {
        viewModel.loadTaskById(taskId)
        userViewModel.loadUsers()
        assignmentViewModel.loadAssignments(taskId)
        dependencyViewModel.loadDependencies(taskId)
    }

    LaunchedEffect(selectedTask) {
        selectedTask?.let { task ->
            title = task.title
            description = task.description ?: ""
            status = task.status ?: ""
            priority = task.priority ?: ""
            completionRate = (task.completion_rate ?: 0).toString()

            viewModel.loadTasks(task.project_id)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ForgePlanTopBar(
            title = "Edit Task",
            initials = "FP"
        )

        if (isLoading && selectedTask == null) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(18.dp)
            ) {
                CircularProgressIndicator()
            }

            ForgePlanBottomBar(
                selectedItem = "Projects"
            )

            return@Column
        }

        selectedTask?.let { task ->

            val currentDependencyIds =
                dependencies.map { it.depends_on_task_id }

            val dependencyTasks =
                tasks.filter {
                    it.id != task.id &&
                            !currentDependencyIds.contains(it.id)
                }

            val currentDependencyTasks =
                tasks.filter { currentDependencyIds.contains(it.id) }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                ForgeSectionTitle(text = "Edit task")

                Spacer(modifier = Modifier.height(14.dp))

                ForgeCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Task name") },
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Status",
                            style = MaterialTheme.typography.titleSmall
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TaskOptionChip(
                                text = "PENDING",
                                selected = status == "PENDING",
                                onClick = {
                                    status = "PENDING"
                                    completionRate = "0"
                                }
                            )

                            TaskOptionChip(
                                text = "IN_PROGRESS",
                                selected = status == "IN_PROGRESS",
                                onClick = {
                                    status = "IN_PROGRESS"
                                    if (completionRate == "0") completionRate = "40"
                                }
                            )

                            TaskOptionChip(
                                text = "DONE",
                                selected = status == "DONE",
                                onClick = {
                                    status = "DONE"
                                    completionRate = "100"
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Priority",
                            style = MaterialTheme.typography.titleSmall
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TaskOptionChip(
                                text = "LOW",
                                selected = priority == "LOW",
                                onClick = { priority = "LOW" }
                            )

                            TaskOptionChip(
                                text = "MEDIUM",
                                selected = priority == "MEDIUM",
                                onClick = { priority = "MEDIUM" }
                            )

                            TaskOptionChip(
                                text = "HIGH",
                                selected = priority == "HIGH",
                                onClick = { priority = "HIGH" }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = completionRate,
                            onValueChange = { completionRate = it },
                            label = { Text("Completion (%)") },
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                ForgePrimaryButton(
                    text = "Save changes",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val updatedTask = Task(
                            id = task.id,
                            project_id = task.project_id,
                            created_by_id = task.created_by_id,
                            title = title,
                            description = description,
                            status = status,
                            priority = priority,
                            completion_rate = completionRate.toIntOrNull() ?: 0,
                            start_date = task.start_date,
                            end_date = task.end_date
                        )

                        viewModel.updateTask(
                            task = updatedTask,
                            onSuccess = onTaskUpdated
                        )
                    }
                )

                Spacer(modifier = Modifier.height(28.dp))

                ForgeSectionTitle(text = "Dependências da tarefa")

                Spacer(modifier = Modifier.height(10.dp))

                ForgeCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(
                            text = "Esta tarefa depende de:",
                            style = MaterialTheme.typography.titleSmall
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (currentDependencyTasks.isEmpty()) {
                            Text(
                                text = "Ainda não existem precedências definidas.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            currentDependencyTasks.forEach { dependencyTask ->
                                Text(
                                    text = "• ${dependencyTask.title}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        DependencySelector(
                            selectedTask = selectedDependencyTask,
                            tasks = dependencyTasks,
                            onTaskSelected = {
                                selectedDependencyTask = it
                                dependencyMessage = null
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        ForgePrimaryButton(
                            text = "Guardar precedência",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val dependencyTask = selectedDependencyTask

                                when {
                                    dependencyTask == null -> {
                                        dependencyMessage =
                                            "Seleciona uma tarefa predecessora."
                                    }

                                    currentDependencyIds.contains(dependencyTask.id) -> {
                                        dependencyMessage =
                                            "Esta precedência já existe."
                                    }

                                    else -> {
                                        dependencyViewModel.createDependency(
                                            taskId = task.id,
                                            dependsOnTaskId = dependencyTask.id,
                                            onSuccess = {
                                                dependencyMessage =
                                                    "Precedência guardada com sucesso."
                                                selectedDependencyTask = null
                                                dependencyViewModel.loadDependencies(task.id)
                                            }
                                        )
                                    }
                                }
                            }
                        )

                        dependencyMessage?.let {
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        dependencyError?.let {
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                ForgeSectionTitle(text = "Utilizadores associados")

                Spacer(modifier = Modifier.height(10.dp))

                val assignedUsers = users.filter { assignedUserIds.contains(it.id) }

                if (assignedUsers.isEmpty()) {
                    Text(
                        text = "Ainda não existem utilizadores associados.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    ForgeCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            assignedUsers.forEach { user ->
                                Row {
                                    UserAvatarChip(initials = userInitials(user))

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Text(
                                            text = user.name,
                                            style = MaterialTheme.typography.titleSmall
                                        )

                                        Text(
                                            text = "@${user.username}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                ForgeSectionTitle(text = "Associar utilizador à tarefa")

                Spacer(modifier = Modifier.height(10.dp))

                assignmentMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                assignmentError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (users.isEmpty()) {
                    Text(
                        text = "Não existem utilizadores disponíveis.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    users.forEach { user ->
                        val isAssigned = assignedUserIds.contains(user.id)

                        UserAssignmentCard(
                            user = user,
                            isAssigned = isAssigned,
                            onAssignClick = {
                                assignmentViewModel.assignUserToTask(
                                    taskId = task.id,
                                    userId = user.id,
                                    onSuccess = {
                                        assignmentMessage =
                                            "Utilizador ${user.name} associado à tarefa."
                                    }
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        ForgePlanBottomBar(
            selectedItem = "Projects"
        )
    }
}

@Composable
fun DependencySelector(
    selectedTask: Task?,
    tasks: List<Task>,
    onTaskSelected: (Task) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ForgeOutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                expanded = true
            }
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = selectedTask?.title ?: "Selecionar tarefa predecessora",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = {
            expanded = false
        }
    ) {
        tasks.forEach { task ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                onClick = {
                    onTaskSelected(task)
                    expanded = false
                }
            )
        }
    }
}

@Composable
fun UserAssignmentCard(
    user: User,
    isAssigned: Boolean,
    onAssignClick: () -> Unit
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
                    text = "Associar",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onAssignClick
                )
            }
        }
    }
}

@Composable
fun TaskOptionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
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

private fun userInitials(user: User): String {
    return user.name
        .split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()
        .ifBlank { user.username.take(2).uppercase() }
}