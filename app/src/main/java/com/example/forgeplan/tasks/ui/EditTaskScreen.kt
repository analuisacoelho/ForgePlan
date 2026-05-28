package com.example.forgeplan.tasks.ui

import android.content.Context
import android.content.res.Configuration
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.model.TaskAttachment
import com.example.forgeplan.core.model.User
import com.example.forgeplan.core.network.SupabaseApi
import com.example.forgeplan.core.repository.TaskAttachmentRepository
import com.example.forgeplan.projects.viewmodel.ProjectViewModel
import com.example.forgeplan.tasks.viewmodel.TaskAssignmentViewModel
import com.example.forgeplan.tasks.viewmodel.TaskDependencyViewModel
import com.example.forgeplan.tasks.viewmodel.TaskViewModel
import com.example.forgeplan.tasks.viewmodel.UserViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun EditTaskScreen(
    taskId: Long,
    onTaskUpdated: () -> Unit,
    taskViewModel: TaskViewModel = viewModel(),
    projectViewModel: ProjectViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel(),
    assignmentViewModel: TaskAssignmentViewModel = viewModel(),
    dependencyViewModel: TaskDependencyViewModel = viewModel()
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val context = LocalContext.current
    val attachmentRepository = remember { TaskAttachmentRepository() }

    val selectedTask by taskViewModel.selectedTask.collectAsState()
    val isLoading by taskViewModel.isLoading.collectAsState()
    val projects by projectViewModel.projects.collectAsState()
    val tasks by taskViewModel.tasks.collectAsState()
    val users by userViewModel.users.collectAsState()
    val assignments by assignmentViewModel.assignments.collectAsState()
    val dependencies by dependencyViewModel.dependencies.collectAsState()

    val taskError by taskViewModel.error.collectAsState()
    val assignmentError by assignmentViewModel.error.collectAsState()
    val dependencyError by dependencyViewModel.error.collectAsState()

    var selectedProject by remember { mutableStateOf<Project?>(null) }
    var selectedTaskForDropdown by remember { mutableStateOf<Task?>(null) }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var completionRate by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf("PENDING") }

    var selectedDependency by remember { mutableStateOf<Task?>(null) }
    var userSearch by remember { mutableStateOf("") }

    val selectedUsers = remember { mutableStateListOf<User>() }
    val newAttachments = remember { mutableStateListOf<Uri>() }
    val existingAttachments = remember { mutableStateListOf<TaskAttachment>() }

    var message by remember { mutableStateOf<String?>(null) }
    var titleError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            newAttachments.add(it)
            message = null
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            newAttachments.add(it)
            message = null
        }
    }

    LaunchedEffect(taskId) {
        taskViewModel.loadTaskById(taskId)
        projectViewModel.loadProjects()
        userViewModel.loadUsers()
        assignmentViewModel.loadAssignments(taskId)
        dependencyViewModel.loadDependencies(taskId)

        loadEditTaskAttachments(
            taskId = taskId,
            onSuccess = {
                existingAttachments.clear()
                existingAttachments.addAll(it)
            }
        )
    }

    LaunchedEffect(selectedTask) {
        selectedTask?.let { task ->
            selectedTaskForDropdown = task
            title = task.title
            description = task.description ?: ""
            priority = task.priority ?: "MEDIUM"
            startDate = task.start_date ?: ""
            endDate = task.end_date ?: ""
            completionRate = task.completion_rate ?: 0
            status = task.status ?: "PENDING"

            taskViewModel.loadTasks(task.project_id)
            selectedProject = projects.firstOrNull { it.id == task.project_id }
        }
    }

    LaunchedEffect(projects, selectedTask) {
        selectedTask?.let { task ->
            if (selectedProject == null) {
                selectedProject = projects.firstOrNull { it.id == task.project_id }
            }
        }
    }

    LaunchedEffect(assignments, users) {
        val assignedIds = assignments.map { it.user_id }
        val assignedUsers = users.filter { assignedIds.contains(it.id) }

        selectedUsers.clear()
        selectedUsers.addAll(assignedUsers)
    }

    LaunchedEffect(dependencies, tasks) {
        val firstDependencyId = dependencies.firstOrNull()?.depends_on_task_id
        selectedDependency = tasks.firstOrNull { it.id == firstDependencyId }
    }

    val saveChanges: () -> Unit = {
        val task = selectedTaskForDropdown
        val project = selectedProject
        var hasError = false

        if (title.isBlank()) {
            titleError = appText(
                en = "The title is required.",
                pt = "O título é obrigatório."
            )
            hasError = true
        }

        val dateRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")

        if (startDate.isNotBlank() && !dateRegex.matches(startDate)) {
            dateError = appText(
                en = "The start date must be in YYYY-MM-DD format.",
                pt = "A data de início deve estar no formato YYYY-MM-DD."
            )
            hasError = true
        }

        if (endDate.isNotBlank() && !dateRegex.matches(endDate)) {
            dateError = appText(
                en = "The end date must be in YYYY-MM-DD format.",
                pt = "A data de fim deve estar no formato YYYY-MM-DD."
            )
            hasError = true
        }

        if (
            startDate.isNotBlank() &&
            endDate.isNotBlank() &&
            endDate < startDate
        ) {
            dateError = appText(
                en = "The end date cannot be earlier than the start date.",
                pt = "A data de fim não pode ser anterior à data de início."
            )
            hasError = true
        }

        if (task == null || project == null) {
            message = appText(
                en = "Error: invalid task or project.",
                pt = "Erro: tarefa ou projeto inválido."
            )
            hasError = true
        }

        if (!hasError && task != null && project != null && !isSaving) {
            isSaving = true
            message = appText(
                en = "Saving changes...",
                pt = "A guardar alterações..."
            )

            val updatedStatus = when {
                completionRate >= 100 -> "DONE"
                completionRate > 0 -> "IN_PROGRESS"
                else -> status.ifBlank { "PENDING" }
            }

            val updatedTask = Task(
                id = task.id,
                project_id = project.id,
                created_by_id = task.created_by_id,
                title = title.trim(),
                description = description.trim().ifBlank { null },
                status = updatedStatus,
                priority = priority,
                completion_rate = completionRate,
                start_date = startDate.trim().ifBlank { null },
                end_date = endDate.trim().ifBlank { null }
            )

            taskViewModel.updateTask(
                task = updatedTask,
                onSuccess = {
                    val alreadyAssignedIds = assignments.map { it.user_id }

                    selectedUsers
                        .filter { !alreadyAssignedIds.contains(it.id) }
                        .forEach { user ->
                            assignmentViewModel.assignUserToTask(
                                taskId = task.id,
                                userId = user.id,
                                onSuccess = {}
                            )
                        }

                    val existingDependencyIds = dependencies.map { it.depends_on_task_id }

                    selectedDependency?.let { dependency ->
                        if (!existingDependencyIds.contains(dependency.id)) {
                            dependencyViewModel.createDependency(
                                taskId = task.id,
                                dependsOnTaskId = dependency.id,
                                onSuccess = {}
                            )
                        }
                    }

                    val attachmentsToUpload = newAttachments.toList()

                    if (attachmentsToUpload.isEmpty()) {
                        isSaving = false
                        onTaskUpdated()
                    } else {
                        uploadEditTaskAttachmentsSequentially(
                            context = context,
                            repository = attachmentRepository,
                            taskId = task.id,
                            attachments = attachmentsToUpload,
                            index = 0,
                            onSuccess = {
                                isSaving = false
                                onTaskUpdated()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        EditTaskTopBar(
            onClose = onTaskUpdated
        )

        if (isLoading && selectedTask == null) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(22.dp)
            ) {
                CircularProgressIndicator()
            }

            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (isLandscape) 32.dp else 22.dp,
                    vertical = if (isLandscape) 14.dp else 22.dp
                )
        ) {
            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        EditTaskMainFields(
                            selectedProject = selectedProject,
                            projects = projects,
                            selectedTaskForDropdown = selectedTaskForDropdown,
                            tasks = tasks,
                            title = title,
                            titleError = titleError,
                            description = description,
                            startDate = startDate,
                            endDate = endDate,
                            dateError = dateError,
                            onProjectSelected = {
                                selectedProject = it
                                taskViewModel.loadTasks(it.id)
                                message = null
                            },
                            onTaskSelected = { task ->
                                selectedTaskForDropdown = task
                                taskViewModel.loadTaskById(task.id)
                                assignmentViewModel.loadAssignments(task.id)
                                dependencyViewModel.loadDependencies(task.id)
                                loadEditTaskAttachments(
                                    taskId = task.id,
                                    onSuccess = {
                                        existingAttachments.clear()
                                        existingAttachments.addAll(it)
                                    }
                                )
                            },
                            onTitleChange = {
                                title = it
                                titleError = null
                                message = null
                            },
                            onDescriptionChange = { description = it },
                            onStartDateChange = {
                                startDate = it
                                dateError = null
                            },
                            onEndDateChange = {
                                endDate = it
                                dateError = null
                            }
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        EditTaskSideFields(
                            priority = priority,
                            userSearch = userSearch,
                            users = users,
                            selectedUsers = selectedUsers,
                            existingAttachments = existingAttachments,
                            newAttachments = newAttachments,
                            context = context,
                            taskError = taskError,
                            assignmentError = assignmentError,
                            dependencyError = dependencyError,
                            message = message,
                            onPriorityChange = { priority = it },
                            onUserSearchChange = { userSearch = it },
                            onDocumentClick = { documentPicker.launch("*/*") },
                            onImageClick = { imagePicker.launch("image/*") }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        EditTaskButtons(
                            isSaving = isSaving,
                            onCancel = onTaskUpdated,
                            onSave = saveChanges
                        )
                    }
                }
            } else {
                EditTaskMainFields(
                    selectedProject = selectedProject,
                    projects = projects,
                    selectedTaskForDropdown = selectedTaskForDropdown,
                    tasks = tasks,
                    title = title,
                    titleError = titleError,
                    description = description,
                    startDate = startDate,
                    endDate = endDate,
                    dateError = dateError,
                    onProjectSelected = {
                        selectedProject = it
                        taskViewModel.loadTasks(it.id)
                        message = null
                    },
                    onTaskSelected = { task ->
                        selectedTaskForDropdown = task
                        taskViewModel.loadTaskById(task.id)
                        assignmentViewModel.loadAssignments(task.id)
                        dependencyViewModel.loadDependencies(task.id)
                        loadEditTaskAttachments(
                            taskId = task.id,
                            onSuccess = {
                                existingAttachments.clear()
                                existingAttachments.addAll(it)
                            }
                        )
                    },
                    onTitleChange = {
                        title = it
                        titleError = null
                        message = null
                    },
                    onDescriptionChange = { description = it },
                    onStartDateChange = {
                        startDate = it
                        dateError = null
                    },
                    onEndDateChange = {
                        endDate = it
                        dateError = null
                    }
                )

                EditTaskSideFields(
                    priority = priority,
                    userSearch = userSearch,
                    users = users,
                    selectedUsers = selectedUsers,
                    existingAttachments = existingAttachments,
                    newAttachments = newAttachments,
                    context = context,
                    taskError = taskError,
                    assignmentError = assignmentError,
                    dependencyError = dependencyError,
                    message = message,
                    onPriorityChange = { priority = it },
                    onUserSearchChange = { userSearch = it },
                    onDocumentClick = { documentPicker.launch("*/*") },
                    onImageClick = { imagePicker.launch("image/*") }
                )

                Spacer(modifier = Modifier.height(26.dp))

                EditTaskButtons(
                    isSaving = isSaving,
                    onCancel = onTaskUpdated,
                    onSave = saveChanges
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun EditTaskMainFields(
    selectedProject: Project?,
    projects: List<Project>,
    selectedTaskForDropdown: Task?,
    tasks: List<Task>,
    title: String,
    titleError: String?,
    description: String,
    startDate: String,
    endDate: String,
    dateError: String?,
    onProjectSelected: (Project) -> Unit,
    onTaskSelected: (Task) -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit
) {
    Text(
        text = appText(en = "Project", pt = "Projeto"),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(8.dp))

    ProjectDropdown(
        selectedProject = selectedProject,
        projects = projects,
        onProjectSelected = onProjectSelected
    )

    Spacer(modifier = Modifier.height(18.dp))

    Text(
        text = appText(en = "Task", pt = "Tarefa"),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(8.dp))

    EditTaskDropdown(
        selectedTask = selectedTaskForDropdown,
        tasks = tasks,
        onTaskSelected = onTaskSelected
    )

    Spacer(modifier = Modifier.height(18.dp))

    Text(
        text = appText(en = "Edit name", pt = "Editar nome"),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(8.dp))

    TaskTextField(
        value = title,
        onValueChange = onTitleChange,
        placeholder = appText(en = "Edit", pt = "Editar"),
        isError = titleError != null
    )

    titleError?.let {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }

    Spacer(modifier = Modifier.height(18.dp))

    Text(
        text = appText(en = "Description", pt = "Descrição"),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(8.dp))

    TaskTextField(
        value = description,
        onValueChange = onDescriptionChange,
        placeholder = appText(
            en = "Describe the task and all it needs",
            pt = "Descreve a tarefa e tudo o que é necessário"
        ),
        height = 170.dp,
        leadingSymbol = "▤"
    )

    Spacer(modifier = Modifier.height(18.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appText(en = "Start", pt = "Início"),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            TaskTextField(
                value = startDate,
                onValueChange = onStartDateChange,
                placeholder = "YYYY-MM-DD",
                height = 48.dp
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appText(en = "End", pt = "Fim"),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            TaskTextField(
                value = endDate,
                onValueChange = onEndDateChange,
                placeholder = "YYYY-MM-DD",
                height = 48.dp,
                isError = dateError != null
            )
        }
    }

    dateError?.let {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }

    Spacer(modifier = Modifier.height(22.dp))
}

@Composable
fun EditTaskSideFields(
    priority: String,
    userSearch: String,
    users: List<User>,
    selectedUsers: MutableList<User>,
    existingAttachments: MutableList<TaskAttachment>,
    newAttachments: MutableList<Uri>,
    context: Context,
    taskError: String?,
    assignmentError: String?,
    dependencyError: String?,
    message: String?,
    onPriorityChange: (String) -> Unit,
    onUserSearchChange: (String) -> Unit,
    onDocumentClick: () -> Unit,
    onImageClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = appText(en = "Priority", pt = "Prioridade"),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = appText(en = "+ Add", pt = "+ Adicionar"),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        PriorityButton(
            text = appText(en = "Low", pt = "Baixa"),
            selected = priority == "LOW",
            priorityKey = "LOW",
            onClick = { onPriorityChange("LOW") }
        )

        PriorityButton(
            text = appText(en = "Medium", pt = "Média"),
            selected = priority == "MEDIUM",
            priorityKey = "MEDIUM",
            onClick = { onPriorityChange("MEDIUM") }
        )

        PriorityButton(
            text = appText(en = "High", pt = "Alta"),
            selected = priority == "HIGH",
            priorityKey = "HIGH",
            onClick = { onPriorityChange("HIGH") }
        )
    }

    Spacer(modifier = Modifier.height(22.dp))

    Text(
        text = appText(en = "In charge", pt = "Responsável"),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(10.dp))

    SearchUsersField(
        value = userSearch,
        onValueChange = onUserSearchChange
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
                        onUserSearchChange("")
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
            selectedUsers.take(3).forEach { user ->
                SelectedUserChip(
                    user = user,
                    onRemove = {
                        selectedUsers.remove(user)
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(22.dp))

    Text(
        text = appText(en = "Attachments", pt = "Anexos"),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AttachmentBox(
            symbol = "⇧",
            text = appText(en = "Upload Doc", pt = "Carregar documento"),
            modifier = Modifier.weight(1f),
            onClick = onDocumentClick
        )

        AttachmentBox(
            symbol = "▣",
            text = appText(en = "Take Photo", pt = "Adicionar foto"),
            modifier = Modifier.weight(1f),
            onClick = onImageClick
        )
    }

    if (existingAttachments.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))

        existingAttachments.forEach { attachment ->
            ExistingAttachmentRow(
                fileName = attachment.file_name ?: appText(en = "Attachment", pt = "Anexo")
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (newAttachments.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))

        newAttachments.forEach { uri ->
            AttachmentSelectedRow(
                fileName = editGetFileNameFromUri(context, uri),
                onRemove = { newAttachments.remove(uri) }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    taskError?.let {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }

    assignmentError?.let {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }

    dependencyError?.let {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }

    message?.let {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = it,
            color = if (
                it.contains("Erro", ignoreCase = true) ||
                it.contains("Error", ignoreCase = true)
            ) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun EditTaskButtons(
    isSaving: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        OutlinedButton(
            modifier = Modifier
                .weight(1f)
                .height(58.dp),
            onClick = onCancel,
            enabled = !isSaving,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = appText(en = "Cancel", pt = "Cancelar"),
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
            onClick = onSave,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("☑", style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = if (isSaving) {
                    appText(en = "Saving...", pt = "A guardar...")
                } else {
                    appText(en = "Save changes", pt = "Guardar alterações")
                },
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun EditTaskTopBar(
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
            text = appText(en = "Edit Task", pt = "Editar Tarefa"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiary
        )
    }
}

@Composable
fun EditTaskDropdown(
    selectedTask: Task?,
    tasks: List<Task>,
    onTaskSelected: (Task) -> Unit
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
                Text(
                    text = "☑",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = selectedTask?.title ?: appText(
                        en = "Select your task",
                        pt = "Selecionar tarefa"
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "⌄",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
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
}

@Composable
fun ExistingAttachmentRow(
    fileName: String
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
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
        }
    }
}

private fun loadEditTaskAttachments(
    taskId: Long,
    onSuccess: (List<TaskAttachment>) -> Unit
) {
    SupabaseApi.service.getTaskAttachmentsByTaskId("eq.$taskId")
        .enqueue(object : Callback<List<TaskAttachment>> {
            override fun onResponse(
                call: Call<List<TaskAttachment>>,
                response: Response<List<TaskAttachment>>
            ) {
                onSuccess(response.body() ?: emptyList())
            }

            override fun onFailure(
                call: Call<List<TaskAttachment>>,
                t: Throwable
            ) {
                onSuccess(emptyList())
            }
        })
}

private fun uploadEditTaskAttachmentsSequentially(
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
            uploadEditTaskAttachmentsSequentially(
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
            onError(
                appText(
                    en = "Error saving attachment: $error",
                    pt = "Erro ao guardar anexo: $error"
                )
            )
        }
    )
}

private fun editGetFileNameFromUri(
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