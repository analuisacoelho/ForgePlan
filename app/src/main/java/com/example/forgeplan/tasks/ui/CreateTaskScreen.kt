package com.example.forgeplan.tasks.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.model.User
import com.example.forgeplan.core.repository.TaskAttachmentRepository
import com.example.forgeplan.projects.viewmodel.ProjectViewModel
import com.example.forgeplan.tasks.viewmodel.TaskAssignmentViewModel
import com.example.forgeplan.tasks.viewmodel.TaskDependencyViewModel
import com.example.forgeplan.tasks.viewmodel.TaskViewModel
import com.example.forgeplan.tasks.viewmodel.UserViewModel

@Composable
fun CreateTaskScreen(
    projectId: Long?,
    onTaskCreated: () -> Unit,
    taskViewModel: TaskViewModel = viewModel(),
    projectViewModel: ProjectViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel(),
    assignmentViewModel: TaskAssignmentViewModel = viewModel(),
    dependencyViewModel: TaskDependencyViewModel = viewModel()
) {
    val context = LocalContext.current
    val attachmentRepository = remember { TaskAttachmentRepository() }

    val projects by projectViewModel.projects.collectAsState()
    val tasks by taskViewModel.tasks.collectAsState()
    val users by userViewModel.users.collectAsState()
    val taskError by taskViewModel.error.collectAsState()
    val assignmentError by assignmentViewModel.error.collectAsState()
    val dependencyError by dependencyViewModel.error.collectAsState()

    var selectedProject by remember { mutableStateOf<Project?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var selectedDependency by remember { mutableStateOf<Task?>(null) }
    var userSearch by remember { mutableStateOf("") }

    val selectedUsers = remember { mutableStateListOf<User>() }
    val attachments = remember { mutableStateListOf<Uri>() }

    var projectError by remember { mutableStateOf<String?>(null) }
    var titleError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val saveTask: () -> Unit = {
        var hasError = false
        val project = selectedProject

        if (project == null) {
            projectError = "Seleciona um projeto."
            hasError = true
        }

        if (title.isBlank()) {
            titleError = "O título é obrigatório."
            hasError = true
        }

        if (
            startDate.isNotBlank() &&
            endDate.isNotBlank() &&
            endDate < startDate
        ) {
            dateError = "A data de fim não pode ser anterior à data de início."
            hasError = true
        }

        if (!hasError && project != null && !isSaving) {
            isSaving = true
            message = "A guardar tarefa..."

            val newTask = Task(
                id = 0,
                project_id = project.id,
                created_by_id = null,
                title = title.trim(),
                description = description.trim().ifBlank { null },
                status = "PENDING",
                priority = priority,
                completion_rate = 0,
                start_date = startDate.trim().ifBlank { null },
                end_date = endDate.trim().ifBlank { null }
            )

            taskViewModel.createTaskReturning(
                task = newTask,
                onSuccess = { createdTask ->
                    val createdId = createdTask?.id

                    if (createdId == null || createdId == 0L) {
                        isSaving = false
                        message = "Erro: tarefa criada sem ID válido."
                        return@createTaskReturning
                    }

                    selectedDependency?.let { dependency ->
                        dependencyViewModel.createDependency(
                            taskId = createdId,
                            dependsOnTaskId = dependency.id,
                            onSuccess = {}
                        )
                    }

                    selectedUsers.forEach { user ->
                        assignmentViewModel.assignUserToTask(
                            taskId = createdId,
                            userId = user.id,
                            onSuccess = {}
                        )
                    }

                    val attachmentsToUpload = attachments.toList()

                    if (attachmentsToUpload.isEmpty()) {
                        isSaving = false
                        onTaskCreated()
                    } else {
                        uploadTaskAttachmentsSequentially(
                            context = context,
                            repository = attachmentRepository,
                            taskId = createdId,
                            attachments = attachmentsToUpload,
                            index = 0,
                            onSuccess = {
                                isSaving = false
                                onTaskCreated()
                            },
                            onError = { errorMessage ->
                                isSaving = false
                                message = errorMessage
                            }
                        )
                    }
                }
            )
        }
    }

    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            attachments.add(it)
            message = null
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            attachments.add(it)
            message = null
        }
    }

    LaunchedEffect(Unit) {
        projectViewModel.loadProjects()
        userViewModel.loadUsers()
    }

    LaunchedEffect(projects, projectId) {
        if (selectedProject == null && projects.isNotEmpty()) {
            selectedProject = projects.firstOrNull { it.id == projectId } ?: projects.first()
        }
    }

    LaunchedEffect(selectedProject?.id) {
        selectedProject?.let { project ->
            taskViewModel.loadTasks(project.id)
            selectedDependency = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NewTaskTopBar(
            onClose = onTaskCreated,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 22.dp)
        ) {
            Text("Project", style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(8.dp))

            ProjectDropdown(
                selectedProject = selectedProject,
                projects = projects,
                onProjectSelected = {
                    selectedProject = it
                    projectError = null
                    message = null
                }
            )

            projectError?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text("Task name", style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(8.dp))

            TaskTextField(
                value = title,
                onValueChange = {
                    title = it
                    titleError = null
                    message = null
                },
                placeholder = "Name your task",
                isError = titleError != null
            )

            titleError?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text("Description", style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(8.dp))

            TaskTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = "Describe the task and all it needs",
                height = 150.dp,
                leadingSymbol = "▤"
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Start", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    TaskTextField(
                        value = startDate,
                        onValueChange = {
                            startDate = it
                            dateError = null
                        },
                        placeholder = "mm/dd/yyyy",
                        height = 48.dp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("End", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    TaskTextField(
                        value = endDate,
                        onValueChange = {
                            endDate = it
                            dateError = null
                        },
                        placeholder = "mm/dd/yyyy",
                        height = 48.dp,
                        isError = dateError != null
                    )
                }
            }

            dateError?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text("Depends on", style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(8.dp))

            DependencyBox(
                tasks = tasks,
                selectedDependency = selectedDependency,
                onDependencySelected = {
                    selectedDependency = it
                    message = null
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text("Priority", style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PriorityButton(
                    text = "Low",
                    selected = priority == "LOW",
                    selectedColor = Color(0xFFFFF8EF),
                    borderColor = Color(0xFFE8B77E),
                    onClick = { priority = "LOW" }
                )

                PriorityButton(
                    text = "Medium",
                    selected = priority == "MEDIUM",
                    selectedColor = MaterialTheme.colorScheme.tertiary,
                    borderColor = MaterialTheme.colorScheme.primary,
                    onClick = { priority = "MEDIUM" }
                )

                PriorityButton(
                    text = "High",
                    selected = priority == "HIGH",
                    selectedColor = Color(0xFFFFB4A9),
                    borderColor = Color(0xFFB3261E),
                    onClick = { priority = "HIGH" }
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text("In charge", style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(10.dp))

            SearchUsersField(
                value = userSearch,
                onValueChange = { userSearch = it }
            )

            val filteredUsers =
                if (userSearch.isBlank()) {
                    emptyList()
                } else {
                    users.filter { user ->
                        selectedUsers.none { it.id == user.id } &&
                                (
                                        user.name.contains(userSearch, ignoreCase = true) ||
                                                user.username.contains(userSearch, ignoreCase = true) ||
                                                user.email.contains(userSearch, ignoreCase = true)
                                        )
                    }
                }

            if (filteredUsers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    filteredUsers.take(2).forEach { user ->
                        UserSelectableChip(
                            user = user,
                            onClick = {
                                selectedUsers.add(user)
                                userSearch = ""
                            }
                        )
                    }
                }
            }

            if (selectedUsers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    selectedUsers.take(2).forEach { user ->
                        SelectedUserChip(
                            user = user,
                            onRemove = { selectedUsers.remove(user) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text("Attachments", style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AttachmentBox(
                    symbol = "⇧",
                    text = "Upload Doc",
                    modifier = Modifier.weight(1f),
                    onClick = { documentPicker.launch("*/*") }
                )

                AttachmentBox(
                    symbol = "▣",
                    text = "Tirar Foto",
                    modifier = Modifier.weight(1f),
                    onClick = { imagePicker.launch("image/*") }
                )
            }

            if (attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                attachments.forEach { uri ->
                    AttachmentSelectedRow(
                        fileName = getFileNameFromUri(context, uri),
                        onRemove = { attachments.remove(uri) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            taskError?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            assignmentError?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            dependencyError?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            message?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = if (it.contains("Erro", ignoreCase = true)) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp),
                    onClick = onTaskCreated,
                    enabled = !isSaving,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp),
                    enabled = !isSaving,
                    onClick = saveTask,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("☑", style = MaterialTheme.typography.titleMedium)

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (isSaving) "Saving..." else "Save task",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun uploadTaskAttachmentsSequentially(
    context: Context,
    repository: TaskAttachmentRepository,
    taskId: Long,
    attachments: List<Uri>,
    index: Int,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    if (index >= attachments.size) {
        onSuccess()
        return
    }

    repository.uploadAttachment(
        context = context,
        taskId = taskId,
        uri = attachments[index],
        onSuccess = {
            uploadTaskAttachmentsSequentially(
                context = context,
                repository = repository,
                taskId = taskId,
                attachments = attachments,
                index = index + 1,
                onSuccess = onSuccess,
                onError = onError
            )
        },
        onError = { error ->
            onError("Erro ao guardar anexo: $error")
        }
    )
}

@Composable
fun NewTaskTopBar(
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .background(MaterialTheme.colorScheme.tertiary)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "×",
            color = MaterialTheme.colorScheme.onTertiary,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.clickable { onClose() }
        )

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = "ForgePlan",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiary,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "New Task",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiary
        )
    }
}

@Composable
fun ProjectDropdown(
    selectedProject: Project?,
    projects: List<Project>,
    onProjectSelected: (Project) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable { expanded = true },
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("□", style = MaterialTheme.typography.titleLarge)

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = selectedProject?.name ?: "Select your project",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )

                Text("⌄", style = MaterialTheme.typography.titleSmall)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            projects.forEach { project ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onClick = {
                        onProjectSelected(project)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun DependencyBox(
    tasks: List<Task>,
    selectedDependency: Task?,
    onDependencySelected: (Task?) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("□", style = MaterialTheme.typography.titleLarge)

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = selectedDependency?.title ?: "Select your tasks",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )

                Text("⌄", style = MaterialTheme.typography.titleSmall)
            }

            val visibleTasks = tasks.take(3)

            if (visibleTasks.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "No tasks available",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                visibleTasks.forEach { task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clickable {
                                onDependencySelected(
                                    if (selectedDependency?.id == task.id) null else task
                                )
                            }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )

                        RadioButton(
                            selected = selectedDependency?.id == task.id,
                            onClick = {
                                onDependencySelected(
                                    if (selectedDependency?.id == task.id) null else task
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 48.dp,
    leadingSymbol: String? = null,
    isError: Boolean = false
) {
    OutlinedTextField(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                leadingSymbol?.let {
                    Text(it, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(placeholder, style = MaterialTheme.typography.bodyMedium)
            }
        },
        isError = isError,
        shape = RoundedCornerShape(8.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = MaterialTheme.colorScheme.tertiary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.tertiary
        )
    )
}

@Composable
fun PriorityButton(
    text: String,
    selected: Boolean,
    selectedColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(104.dp)
            .height(42.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (selected) selectedColor else Color.Transparent,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected && text == "High") {
                    Color.White
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
fun SearchUsersField(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        value = value,
        onValueChange = onValueChange,
        leadingIcon = {
            Text(
                text = "⌕",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        },
        placeholder = {
            Text(
                text = "Search user",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(50),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFEDEDED),
            unfocusedContainerColor = Color(0xFFEDEDED),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Composable
fun UserSelectableChip(
    user: User,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserCircle(user = user)

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = shortName(user),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun SelectedUserChip(
    user: User,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserCircle(user = user)

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = shortName(user),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "×",
                modifier = Modifier.clickable { onRemove() },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun UserCircle(user: User) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFB4546D)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = userInitials(user),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AttachmentBox(
    symbol: String,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(170.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = symbol,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        }
    }
}

@Composable
fun AttachmentSelectedRow(
    fileName: String,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.tertiary
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "▤",
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )

            Text(
                text = "⌫",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.clickable { onRemove() }
            )
        }
    }
}

private fun getFileNameFromUri(
    context: Context,
    uri: Uri
): String {
    var fileName = "attachment"

    val cursor = context.contentResolver.query(
        uri,
        null,
        null,
        null,
        null
    )

    cursor?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)

        if (it.moveToFirst() && nameIndex >= 0) {
            fileName = it.getString(nameIndex)
        }
    }

    return fileName
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

private fun shortName(user: User): String {
    val parts = user.name.split(" ").filter { it.isNotBlank() }

    return when {
        parts.size >= 2 -> "${parts.first()} ${parts.last().first()}."
        parts.isNotEmpty() -> parts.first()
        else -> user.username
    }
}