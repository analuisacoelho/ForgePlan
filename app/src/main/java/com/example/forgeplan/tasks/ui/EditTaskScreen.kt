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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
        loadTaskAttachments(
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

        if (task == null || project == null) {
            message = "Erro: tarefa ou projeto inválido."
            hasError = true
        }

        val dateRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")

        if (startDate.isNotBlank() && !dateRegex.matches(startDate)) {
            dateError = "A data de início deve estar no formato YYYY-MM-DD."
            hasError = true
        }

        if (endDate.isNotBlank() && !dateRegex.matches(endDate)) {
            dateError = "A data de fim deve estar no formato YYYY-MM-DD."
            hasError = true
        }

        if (!hasError && task != null && project != null && !isSaving) {
            isSaving = true
            message = "A guardar alterações..."

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
                .padding(horizontal = 22.dp, vertical = 22.dp)
        ) {
            Text("Project", style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(8.dp))

            ProjectDropdown(
                selectedProject = selectedProject,
                projects = projects,
                onProjectSelected = {
                    selectedProject = it
                    taskViewModel.loadTasks(it.id)
                    message = null
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text("Task", style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(8.dp))

            EditTaskDropdown(
                selectedTask = selectedTaskForDropdown,
                tasks = tasks,
                onTaskSelected = { task ->
                    selectedTaskForDropdown = task
                    taskViewModel.loadTaskById(task.id)
                    assignmentViewModel.loadAssignments(task.id)
                    dependencyViewModel.loadDependencies(task.id)
                    loadTaskAttachments(
                        taskId = task.id,
                        onSuccess = {
                            existingAttachments.clear()
                            existingAttachments.addAll(it)
                        }
                    )
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text("Edit name", style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(8.dp))

            TaskTextField(
                value = title,
                onValueChange = {
                    title = it
                    titleError = null
                    message = null
                },
                placeholder = "Edit",
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
                height = 170.dp,
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Priority",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "+ Add",
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

            if (existingAttachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                existingAttachments.forEach { attachment ->
                    ExistingAttachmentRow(
                        fileName = attachment.file_name ?: "attachment"
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
                    onClick = onTaskUpdated,
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
                    onClick = saveChanges,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("☑", style = MaterialTheme.typography.titleMedium)

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (isSaving) "Saving..." else "Save changes",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
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
            text = "Edit Task",
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
                Text("☑", style = MaterialTheme.typography.titleLarge)

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = selectedTask?.title ?: "Select your task",
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
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
        }
    }
}

private fun loadTaskAttachments(
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
            onError("Erro ao guardar anexo: $error")
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