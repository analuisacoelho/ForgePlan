package com.example.forgeplan.progress.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.projects.viewmodel.ProjectViewModel
import com.example.forgeplan.tasks.viewmodel.TaskViewModel

@Composable
fun ProgressScreen(
    onProjectsClick: () -> Unit = {},
    onTimelineClick: () -> Unit = {},
    onTeamClick: () -> Unit = {},
    projectViewModel: ProjectViewModel = viewModel(),
    taskViewModel: TaskViewModel = viewModel()
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val projects by projectViewModel.projects.collectAsState()
    val projectError by projectViewModel.error.collectAsState()
    val tasks by taskViewModel.tasks.collectAsState()
    val taskError by taskViewModel.error.collectAsState()

    var selectedProject by remember { mutableStateOf<Project?>(null) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    var progressValue by remember { mutableStateOf(0) }
    var timeSpentHours by remember { mutableStateOf(3) }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        projectViewModel.loadProjects()
    }

    LaunchedEffect(projects) {
        if (selectedProject == null && projects.isNotEmpty()) {
            selectedProject = projects.first()
        }
    }

    LaunchedEffect(selectedProject?.id) {
        selectedProject?.let {
            selectedTask = null
            taskViewModel.loadTasks(it.id)
        }
    }

    LaunchedEffect(tasks) {
        if (selectedTask == null && tasks.isNotEmpty()) {
            selectedTask = tasks.first()
            progressValue = tasks.first().completion_rate ?: 0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ForgePlanTopBar(
            title = "ForgePlan",
            initials = "UN"
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (isLandscape) 32.dp else 22.dp,
                    vertical = if (isLandscape) 14.dp else 18.dp
                )
        ) {
            Text(
                text = appText(en = "Progress", pt = "Progresso"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(if (isLandscape) 12.dp else 18.dp))

            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProjectSelector(
                        selectedProject = selectedProject,
                        projects = projects,
                        modifier = Modifier.weight(1f),
                        onProjectSelected = {
                            selectedProject = it
                            selectedTask = null
                            message = null
                        }
                    )

                    TaskSelector(
                        selectedTask = selectedTask,
                        tasks = tasks,
                        modifier = Modifier.weight(1f),
                        onTaskSelected = {
                            selectedTask = it
                            progressValue = it.completion_rate ?: 0
                            message = null
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        ProgressMainCard(
                            progress = progressValue,
                            onProgressChange = {
                                progressValue = it
                                message = null
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        TimeSpentRow(
                            hours = timeSpentHours,
                            onIncrease = { timeSpentHours++ },
                            onDecrease = {
                                if (timeSpentHours > 0) timeSpentHours--
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LocationRow(
                            value = location,
                            onValueChange = { location = it }
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        PhotoAttachmentCard(
                            selectedPhotoUri = selectedPhotoUri,
                            onPhotoSelected = { selectedPhotoUri = it },
                            onRemovePhoto = { selectedPhotoUri = null }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        NotesCard(
                            value = notes,
                            onValueChange = { notes = it }
                        )
                    }
                }
            } else {
                ProjectSelector(
                    selectedProject = selectedProject,
                    projects = projects,
                    onProjectSelected = {
                        selectedProject = it
                        selectedTask = null
                        message = null
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                TaskSelector(
                    selectedTask = selectedTask,
                    tasks = tasks,
                    onTaskSelected = {
                        selectedTask = it
                        progressValue = it.completion_rate ?: 0
                        message = null
                    }
                )

                Spacer(modifier = Modifier.height(28.dp))

                ProgressMainCard(
                    progress = progressValue,
                    onProgressChange = {
                        progressValue = it
                        message = null
                    }
                )

                Spacer(modifier = Modifier.height(26.dp))

                TimeSpentRow(
                    hours = timeSpentHours,
                    onIncrease = { timeSpentHours++ },
                    onDecrease = {
                        if (timeSpentHours > 0) timeSpentHours--
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                LocationRow(
                    value = location,
                    onValueChange = { location = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                PhotoAttachmentCard(
                    selectedPhotoUri = selectedPhotoUri,
                    onPhotoSelected = { selectedPhotoUri = it },
                    onRemovePhoto = { selectedPhotoUri = null }
                )

                Spacer(modifier = Modifier.height(14.dp))

                NotesCard(
                    value = notes,
                    onValueChange = { notes = it }
                )
            }

            projectError?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            taskError?.let {
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
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                onClick = {
                    val task = selectedTask

                    if (task == null) {
                        message = appText(
                            en = "Select a task before saving.",
                            pt = "Seleciona uma tarefa antes de guardar."
                        )
                    } else {
                        val newStatus = when {
                            progressValue >= 100 -> "DONE"
                            progressValue > 0 -> "IN_PROGRESS"
                            else -> "PENDING"
                        }

                        val updatedTask = task.copy(
                            completion_rate = progressValue,
                            status = newStatus
                        )

                        taskViewModel.updateTask(
                            task = updatedTask,
                            onSuccess = {
                                selectedTask = updatedTask
                                message = appText(
                                    en = "Progress saved successfully.",
                                    pt = "Progresso guardado com sucesso."
                                )
                            }
                        )
                    }
                }
            ) {
                Text("⇧", style = MaterialTheme.typography.titleMedium)

                Spacer(modifier = Modifier.size(8.dp))

                Text(
                    text = appText(en = "Save progress", pt = "Guardar progresso"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        ForgePlanBottomBar(
            selectedItem = "Progress",
            onProjectsClick = onProjectsClick,
            onTimelineClick = onTimelineClick,
            onTeamClick = onTeamClick
        )
    }
}

@Composable
fun ProjectSelector(
    selectedProject: Project?,
    projects: List<Project>,
    modifier: Modifier = Modifier,
    onProjectSelected: (Project) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        SelectorCard(
            text = selectedProject?.name ?: appText(
                en = "Select your project",
                pt = "Selecionar projeto"
            ),
            iconText = "□",
            onClick = { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            projects.forEach { project ->
                DropdownMenuItem(
                    text = { Text(project.name) },
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
fun TaskSelector(
    selectedTask: Task?,
    tasks: List<Task>,
    modifier: Modifier = Modifier,
    onTaskSelected: (Task) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        SelectorCard(
            text = selectedTask?.title ?: appText(
                en = "Select your task",
                pt = "Selecionar tarefa"
            ),
            iconText = "☑",
            onClick = { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            tasks.forEach { task ->
                DropdownMenuItem(
                    text = { Text(task.title) },
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
fun SelectorCard(
    text: String,
    iconText: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(6.dp),
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
            Text(
                text = iconText,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.size(10.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ProgressMainCard(
    progress: Int,
    onProgressChange: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primary
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "◔",
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.size(8.dp))

                Text(
                    text = appText(en = "Progress", pt = "Progresso"),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                ProgressBadge(progress)
            }

            Spacer(modifier = Modifier.height(30.dp))

            Slider(
                value = progress.toFloat(),
                onValueChange = { onProgressChange(it.toInt()) },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.tertiary,
                    activeTrackColor = MaterialTheme.colorScheme.tertiary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.35f)
                )
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "0%",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "100%",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun ProgressBadge(progress: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.tertiary)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${progress.coerceIn(0, 100)}%",
            color = MaterialTheme.colorScheme.onTertiary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TimeSpentRow(
    hours: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    val hourText =
        if (hours == 1) {
            appText(en = "1 hour", pt = "1 hora")
        } else {
            appText(en = "$hours hours", pt = "$hours horas")
        }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.size(10.dp))

            Text(
                text = appText(en = "Time spent", pt = "Tempo gasto"),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.tertiary)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = hourText,
                    color = MaterialTheme.colorScheme.onTertiary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.size(8.dp))

            Column {
                Text(
                    text = "▲",
                    modifier = Modifier.clickable { onIncrease() },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "▼",
                    modifier = Modifier.clickable { onDecrease() },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun LocationRow(
    value: String,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val locations = listOf(
        appText(en = "Workshop A", pt = "Oficina A"),
        appText(en = "Workshop B", pt = "Oficina B"),
        appText(en = "Office", pt = "Escritório"),
        appText(en = "Client site", pt = "Cliente"),
        appText(en = "Remote", pt = "Remoto")
    )

    val displayValue =
        value.ifBlank {
            appText(en = "Location", pt = "Localização")
        }

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clickable { expanded = true },
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.size(10.dp))

                Text(
                    text = displayValue,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            locations.forEach {
                DropdownMenuItem(
                    text = { Text(it) },
                    onClick = {
                        onValueChange(it)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun PhotoAttachmentCard(
    selectedPhotoUri: Uri?,
    onPhotoSelected: (Uri) -> Unit,
    onRemovePhoto: () -> Unit
) {
    val context = LocalContext.current

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onPhotoSelected(it) }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(164.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "▧",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.size(8.dp))

                Text(
                    text = appText(en = "Photo attachment", pt = "Anexo fotográfico"),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedPhotoUri == null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                        .clickable { photoLauncher.launch("image/*") },
                    shape = RoundedCornerShape(10.dp),
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
                            text = "▣",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = appText(en = "Add Photo", pt = "Adicionar foto"),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getFileNameFromUri(context, selectedPhotoUri),
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                            .clickable { onRemovePhoto() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "×",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotesCard(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = appText(
                    en = "Here you can write about your progress and any obstacles you may have encountered.",
                    pt = "Aqui podes escrever sobre o progresso e eventuais obstáculos encontrados."
                )
            )
        },
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("▤")

                Spacer(modifier = Modifier.size(8.dp))

                Text(appText(en = "Notes", pt = "Notas"))
            }
        },
        shape = RoundedCornerShape(8.dp)
    )
}

private fun getFileNameFromUri(
    context: android.content.Context,
    uri: Uri
): String {
    var fileName = "photo_attachment"

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