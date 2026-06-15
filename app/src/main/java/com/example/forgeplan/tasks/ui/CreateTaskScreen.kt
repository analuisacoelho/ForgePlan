package com.example.forgeplan.tasks.ui

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.model.User
import com.example.forgeplan.core.repository.TaskAttachmentRepository
import com.example.forgeplan.core.session.SessionManager
import com.example.forgeplan.projects.viewmodel.ProjectUserViewModel
import com.example.forgeplan.projects.viewmodel.ProjectViewModel
import com.example.forgeplan.tasks.viewmodel.TaskAssignmentViewModel
import com.example.forgeplan.tasks.viewmodel.TaskDependencyViewModel
import com.example.forgeplan.tasks.viewmodel.TaskGroupViewModel
import com.example.forgeplan.tasks.viewmodel.TaskViewModel
import com.example.forgeplan.tasks.viewmodel.UserViewModel
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun CreateTaskScreen(
    projectId: Long?,
    onTaskCreated: () -> Unit,
    taskViewModel: TaskViewModel = viewModel(),
    projectViewModel: ProjectViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel(),
    assignmentViewModel: TaskAssignmentViewModel = viewModel(),
    dependencyViewModel: TaskDependencyViewModel = viewModel(),
    taskGroupViewModel: TaskGroupViewModel = viewModel(),
    projectUserViewModel: ProjectUserViewModel = viewModel()
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val context = LocalContext.current
    val attachmentRepository = remember { TaskAttachmentRepository() }

    val projects by projectViewModel.projects.collectAsState()
    val tasks by taskViewModel.tasks.collectAsState()
    val users by userViewModel.users.collectAsState()
    val groups by taskGroupViewModel.groups.collectAsState()
    val projectUsers by projectUserViewModel.projectUsers.collectAsState()

    val taskError by taskViewModel.error.collectAsState()
    val assignmentError by assignmentViewModel.error.collectAsState()
    val dependencyError by dependencyViewModel.error.collectAsState()
    val groupError by taskGroupViewModel.error.collectAsState()

    var selectedProject by remember { mutableStateOf<Project?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var taskGroup by remember { mutableStateOf("") }
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

    val projectGroups = remember(groups, tasks, taskGroup) {
        // recalcula só quando groups, tasks ou taskGroup mudam
        (
                groups.map { it.name.trim() } + // grupos guardados na BD
                        tasks.mapNotNull { it.task_group?.trim() } + // grupos já usados nas tarefas
                        listOf(taskGroup.trim()) // grupo que o utilizador está a escrever agora
                )
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() } // remove duplicados ignorando capitalização
            .sortedBy { it.lowercase() } // ordena alfabeticamente
    }

    val saveTask: () -> Unit = {
        var hasError = false
        val project = selectedProject

        if (project == null) {
            projectError = appText(
                en = "Select a project.",
                pt = "Seleciona um projeto."
            )
            hasError = true
        }

        if (title.isBlank()) {
            titleError = appText(
                en = "The title is required.",
                pt = "O título é obrigatório."
            )
            hasError = true
        }

        val dateRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")
        val today = LocalDate.now().toString()

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
            dateRegex.matches(startDate) &&
            startDate < today
        ) {
            dateError = appText(
                en = "The start date cannot be earlier than today.",
                pt = "A data de início não pode ser anterior ao dia de hoje."
            )
            hasError = true
        }

        if (
            endDate.isNotBlank() &&
            dateRegex.matches(endDate) &&
            endDate < today
        ) {
            dateError = appText(
                en = "The end date cannot be earlier than today.",
                pt = "A data de fim não pode ser anterior ao dia de hoje."
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

        if (!hasError && project != null && !isSaving) {
            isSaving = true
            message = appText(
                en = "Saving task...",
                pt = "A guardar tarefa..."
            )

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
                end_date = endDate.trim().ifBlank { null },
                task_group = taskGroup.trim().ifBlank { "General" }
            )

            taskViewModel.createTaskReturning(
                task = newTask,
                onSuccess = { createdTask ->
                    val createdId = createdTask?.id

                    if (createdId == null || createdId == 0L) {
                        // proteção: se a tarefa foi criada mas sem ID válido, não tenta associar dados
                        isSaving = false
                        message = appText(
                            en = "Error: task created without a valid ID.",
                            pt = "Erro: tarefa criada sem ID válido."
                        )
                        return@createTaskReturning
                    }

                    selectedDependency?.let { dependency ->
                        dependencyViewModel.createDependency(
                            // cria a dependência só se o utilizador selecionou uma
                            taskId = createdId,
                            dependsOnTaskId = dependency.id,
                            onSuccess = {}
                        )
                    }

                    selectedUsers.forEach { user ->
                        assignmentViewModel.assignUserToTask(
                            // atribui cada utilizador selecionado à tarefa — chamadas paralelas
                            taskId = createdId,
                            userId = user.id,
                            onSuccess = {}
                        )
                    }

                    val attachmentsToUpload = attachments.toList()

                    if (attachmentsToUpload.isEmpty()) {
                        isSaving = false
                        onTaskCreated() // sem anexos → navega imediatamente
                    } else {
                        uploadTaskAttachmentsSequentially( // com anexos → upload sequencial
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
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            attachments.add(it)
            message = null
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

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

    // recarrega dados quando projeto muda
    LaunchedEffect(selectedProject?.id) {
        selectedProject?.let { project ->
            taskViewModel.loadTasks(project.id) // tarefas para dependências
            taskGroupViewModel.loadGroups(project.id) // grupos disponíveis
            projectUserViewModel.loadProjectUsers(project.id) // membros para atribuição
            selectedDependency = null // limpa dependência — era de outro projeto
            taskGroup = "" // limpa grupo — pode não existir no novo projeto
        }
    }

    val projectMemberUsers = users.filter { user ->
        val isInProject = projectUsers.any { projectUser ->
            projectUser.user_id == user.id
        }

        val isUser = user.role?.uppercase() == "USER"
        val isCurrentManager = user.id == SessionManager.userId &&
                user.role?.uppercase() == "MANAGER"

        isInProject && (isUser || isCurrentManager)
        // só membros do projeto com role USER ou o próprio manager podem ser atribuídos
        // outros managers e admins não aparecem na lista
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NewTaskTopBar(
            onClose = onTaskCreated
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (isLandscape) 32.dp else 22.dp,
                    vertical = if (isLandscape) 14.dp else 22.dp
                )
        ) {
            groupError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        CreateTaskMainFields(
                            selectedProject = selectedProject,
                            projects = projects,
                            projectError = projectError,
                            title = title,
                            titleError = titleError,
                            description = description,
                            taskGroup = taskGroup,
                            projectGroups = projectGroups,
                            startDate = startDate,
                            endDate = endDate,
                            dateError = dateError,
                            tasks = tasks,
                            selectedDependency = selectedDependency,
                            priority = priority,
                            onProjectSelected = {
                                selectedProject = it
                                projectError = null
                                message = null
                            },
                            onTitleChange = {
                                title = it
                                titleError = null
                                message = null
                            },
                            onDescriptionChange = { description = it },
                            onTaskGroupChange = {
                                taskGroup = it
                                message = null
                            },
                            onStartDateChange = {
                                startDate = it
                                if (endDate.isNotBlank() && endDate < it) {
                                    endDate = ""
                                    // se a data de fim era anterior à nova data de início, limpa-a automaticamente
                                    // evita que o formulário fique num estado inválido sem o utilizador reparar
                                }
                                dateError = null
                            },
                            onEndDateChange = {
                                endDate = it
                                dateError = null
                            },
                            onDependencySelected = {
                                selectedDependency = it
                                message = null
                                // clica na tarefa já selecionada → deseleciona (null)
                                // clica noutra tarefa → seleciona essa
                                // só é possível uma dependência de cada vez
                            },
                            onPriorityChange = { priority = it }
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        CreateTaskUsersAndAttachments(
                            users = projectMemberUsers,
                            userSearch = userSearch,
                            selectedUsers = selectedUsers,
                            attachments = attachments,
                            context = context,
                            message = message,
                            taskError = taskError,
                            assignmentError = assignmentError,
                            dependencyError = dependencyError,
                            onUserSearchChange = { userSearch = it },
                            onDocumentClick = { documentPicker.launch(arrayOf("*/*")) },
                            onImageClick = { imagePicker.launch(arrayOf("image/*")) },
                            onMessageColorIsError = {
                                it.contains("Erro", ignoreCase = true) ||
                                        it.contains("Error", ignoreCase = true)
                            }
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        CreateTaskActionButtons(
                            isSaving = isSaving,
                            onCancel = onTaskCreated,
                            onSave = saveTask
                        )
                    }
                }
            } else {
                CreateTaskMainFields(
                    selectedProject = selectedProject,
                    projects = projects,
                    projectError = projectError,
                    title = title,
                    titleError = titleError,
                    description = description,
                    taskGroup = taskGroup,
                    projectGroups = projectGroups,
                    startDate = startDate,
                    endDate = endDate,
                    dateError = dateError,
                    tasks = tasks,
                    selectedDependency = selectedDependency,
                    priority = priority,
                    onProjectSelected = {
                        selectedProject = it
                        projectError = null
                        message = null
                    },
                    onTitleChange = {
                        title = it
                        titleError = null
                        message = null
                    },
                    onDescriptionChange = { description = it },
                    onTaskGroupChange = {
                        taskGroup = it
                        message = null
                    },
                    onStartDateChange = {
                        startDate = it
                        if (endDate.isNotBlank() && endDate < it) {
                            endDate = ""
                        }
                        dateError = null
                    },
                    onEndDateChange = {
                        endDate = it
                        dateError = null
                    },
                    onDependencySelected = {
                        selectedDependency = it
                        message = null
                    },
                    onPriorityChange = { priority = it }
                )

                CreateTaskUsersAndAttachments(
                    users = projectMemberUsers,
                    userSearch = userSearch,
                    selectedUsers = selectedUsers,
                    attachments = attachments,
                    context = context,
                    message = message,
                    taskError = taskError,
                    assignmentError = assignmentError,
                    dependencyError = dependencyError,
                    onUserSearchChange = { userSearch = it },
                    onDocumentClick = { documentPicker.launch(arrayOf("*/*")) },
                    onImageClick = { imagePicker.launch(arrayOf("image/*")) },
                    onMessageColorIsError = {
                        it.contains("Erro", ignoreCase = true) ||
                                it.contains("Error", ignoreCase = true)
                    }
                )

                Spacer(modifier = Modifier.height(26.dp))

                CreateTaskActionButtons(
                    isSaving = isSaving,
                    onCancel = onTaskCreated,
                    onSave = saveTask
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun CreateTaskMainFields(
    selectedProject: Project?,
    projects: List<Project>,
    projectError: String?,
    title: String,
    titleError: String?,
    description: String,
    taskGroup: String,
    projectGroups: List<String>,
    startDate: String,
    endDate: String,
    dateError: String?,
    tasks: List<Task>,
    selectedDependency: Task?,
    priority: String,
    onProjectSelected: (Project) -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onTaskGroupChange: (String) -> Unit,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
    onDependencySelected: (Task?) -> Unit,
    onPriorityChange: (String) -> Unit
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

    projectError?.let {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }

    Spacer(modifier = Modifier.height(18.dp))

    Text(
        text = appText(en = "Task group", pt = "Grupo da tarefa"),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(8.dp))

    TaskGroupField(
        value = taskGroup,
        groups = projectGroups,
        onValueChange = onTaskGroupChange
    )

    Spacer(modifier = Modifier.height(18.dp))

    Text(
        text = appText(en = "Task name", pt = "Nome da tarefa"),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(8.dp))

    TaskTextField(
        value = title,
        onValueChange = onTitleChange,
        placeholder = appText(en = "Name your task", pt = "Nome da tarefa"),
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
        height = 150.dp,
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

            CreateTaskDatePickerField(
                value = startDate,
                onDateSelected = onStartDateChange,
                minDate = LocalDate.now(),
                placeholder = appText(
                    en = "Select date",
                    pt = "Selecionar data"
                ),
                isError = dateError != null
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appText(en = "End", pt = "Fim"),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            CreateTaskDatePickerField(
                value = endDate,
                onDateSelected = onEndDateChange,
                // campo da data de fim
                minDate = maxOf(
                    LocalDate.now(),
                    CreateTaskDateUtils.parse(startDate) ?: LocalDate.now()
                ),
                // a data mínima do fim é o máximo entre hoje e a data de início
                // garante que o utilizador não pode selecionar uma data de fim antes do início
                // e também não pode selecionar datas no passado
                placeholder = appText(
                    en = "Select date",
                    pt = "Selecionar data"
                ),
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

    Spacer(modifier = Modifier.height(18.dp))

    Text(
        text = appText(en = "Depends on", pt = "Depende de"),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(8.dp))

    DependencyBox(
        tasks = tasks,
        selectedDependency = selectedDependency,
        onDependencySelected = onDependencySelected
    )

    Spacer(modifier = Modifier.height(18.dp))

    Text(
        text = appText(en = "Priority", pt = "Prioridade"),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
    )

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
}

@Composable
fun TaskGroupField(
    value: String,
    groups: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val normalizedGroups = remember(groups) {
        groups
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }

    Column {
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
                        text = "▦",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = value.ifBlank {
                            appText(
                                en = "Select a group",
                                pt = "Selecionar grupo"
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (value.isBlank()) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
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
                normalizedGroups.forEach { group ->
                    DropdownMenuItem(
                        text = { Text(group) },
                        onClick = {
                            onValueChange(group)
                            expanded = false
                        }
                    )
                }

                if (normalizedGroups.isEmpty()) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                appText(
                                    en = "No groups available",
                                    pt = "Sem grupos disponíveis"
                                )
                            )
                        },
                        onClick = { expanded = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TaskTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = appText(
                en = "Or type a new group",
                pt = "Ou escreve um novo grupo"
            )
        )
    }
}

@Composable
fun CreateTaskUsersAndAttachments(
    users: List<User>,
    userSearch: String,
    selectedUsers: MutableList<User>,
    attachments: MutableList<Uri>,
    context: Context,
    message: String?,
    taskError: String?,
    assignmentError: String?,
    dependencyError: String?,
    onUserSearchChange: (String) -> Unit,
    onDocumentClick: () -> Unit,
    onImageClick: () -> Unit,
    onMessageColorIsError: (String) -> Boolean
) {
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
            selectedUsers.take(2).forEach { user ->
                SelectedUserChip(
                    user = user,
                    onRemove = { selectedUsers.remove(user) }
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
            color = if (onMessageColorIsError(it)) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun CreateTaskActionButtons(
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
                    appText(en = "Save task", pt = "Guardar tarefa")
                },
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

private fun uploadTaskAttachmentsSequentially(
    context: Context,
    repository: TaskAttachmentRepository,
    taskId: Long,
    attachments: List<Uri>,
    index: Int, // índice atual na lista
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    if (index >= attachments.size) {
        onSuccess() // condição de paragem: todos os anexos foram enviados
        return
    }

    repository.uploadAttachment(
        context = context,
        taskId = taskId,
        uri = attachments[index],
        onSuccess = {
            uploadTaskAttachmentsSequentially( // avança para o próximo anexo recursivamente
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
            onError("Erro ao guardar anexo: $error") // para no primeiro erro — os restantes anexos não são enviados
        }
    )
}
// sequencial em vez de paralelo para evitar sobrecarga da API e manter a ordem

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
            text = appText(en = "New Task", pt = "Nova Tarefa"),
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
                Text(
                    text = "□",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = selectedProject?.name ?: appText(
                        en = "Select your project",
                        pt = "Selecionar projeto"
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
                Text(
                    text = "□",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = selectedDependency?.title ?: appText(
                        en = "Select your tasks",
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
                        text = appText(
                            en = "No tasks available",
                            pt = "Sem tarefas disponíveis"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
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
                            color = MaterialTheme.colorScheme.onSurface,
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
    height: Dp = 48.dp,
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
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
        },
        isError = isError,
        shape = RoundedCornerShape(8.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = MaterialTheme.colorScheme.tertiary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.tertiary,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun PriorityButton(
    text: String,
    selected: Boolean,
    priorityKey: String,
    onClick: () -> Unit
) {
    val selectedColor =
        when (priorityKey) {
            "HIGH" -> MaterialTheme.colorScheme.error
            "MEDIUM" -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.secondaryContainer
        }

    val borderColor =
        when (priorityKey) {
            "HIGH" -> MaterialTheme.colorScheme.error
            "MEDIUM" -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.secondary
        }

    val contentColor =
        if (selected && priorityKey == "HIGH") {
            MaterialTheme.colorScheme.onPrimary
        } else if (selected && priorityKey == "MEDIUM") {
            MaterialTheme.colorScheme.onTertiary
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    Surface(
        modifier = Modifier
            .width(104.dp)
            .height(42.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (selected) selectedColor else MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
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
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
        },
        placeholder = {
            Text(
                text = appText(en = "Search user", pt = "Pesquisar utilizador"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(50),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "×",
                modifier = Modifier.clickable { onRemove() },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
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
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = userInitials(user),
            color = MaterialTheme.colorScheme.onPrimary,
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
        color = MaterialTheme.colorScheme.surface,
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
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
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
                color = MaterialTheme.colorScheme.onSurface,
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

private object CreateTaskDateUtils {
    fun parse(value: String): LocalDate? {
        return try {
            value.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
        } catch (_: Exception) {
            null
        }
    }

    fun toMillis(date: LocalDate): Long {
        return date
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}

@Composable
fun CreateTaskDatePickerField(
    value: String,
    onDateSelected: (String) -> Unit,
    minDate: LocalDate,
    placeholder: String,
    isError: Boolean = false
) {
    val context = LocalContext.current
    val initialDate = CreateTaskDateUtils.parse(value) ?: minDate

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable {
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                        onDateSelected(selectedDate.toString())
                    },
                    initialDate.year,
                    initialDate.monthValue - 1,
                    initialDate.dayOfMonth
                ).apply {
                    datePicker.minDate = CreateTaskDateUtils.toMillis(minDate)
                }.show()
            },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.tertiary
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📅",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = value.ifBlank { placeholder },
                style = MaterialTheme.typography.bodyMedium,
                color = if (value.isBlank()) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun getFileNameFromUri(
    // URIs de ficheiros não têm o nome visível diretamente
    // é necessário consultar o ContentResolver com OpenableColumns.DISPLAY_NAME
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
    // cursor?.use { } fecha o cursor automaticamente mesmo se houver exceção
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
