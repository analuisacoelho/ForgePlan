package com.example.forgeplan.progress.ui

import android.content.res.Configuration
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.session.SessionManager
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.progress.viewmodel.ProgressViewModel
import com.example.forgeplan.tasks.viewmodel.UserDashboardViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    taskId: Long,
    onProjectsClick: () -> Unit = {},
    onTimelineClick: () -> Unit = {},
    onTeamClick: () -> Unit = {},
    onBack: () -> Unit = {},
    dashboardViewModel: UserDashboardViewModel = viewModel(),
    progressViewModel: ProgressViewModel = viewModel()
) {
    val context       = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape   = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val projectsWithTasks by dashboardViewModel.projectsWithTasks.collectAsState()
    val isLoading         by dashboardViewModel.isLoading.collectAsState()
    val isSaving          by progressViewModel.isSaving.collectAsState()
    val saveResult        by progressViewModel.saveResult.collectAsState()

    val userProjects = remember(projectsWithTasks) { projectsWithTasks.keys.toList() }
    val userTasksForProject: (Project?) -> List<Task> = { proj ->
        if (proj == null) emptyList() else projectsWithTasks[proj] ?: emptyList()
    }

    var selectedProject  by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedTaskId   by rememberSaveable { mutableStateOf<Long?>(null) }
    var progressValue    by rememberSaveable { mutableStateOf(0) }
    // Time spent: hours and minutes separately (free input)
    var timeHours        by rememberSaveable { mutableStateOf("") }
    var timeMinutes      by rememberSaveable { mutableStateOf("") }
    var location         by rememberSaveable { mutableStateOf("") }
    var notes            by rememberSaveable { mutableStateOf("") }
    var selectedDate     by rememberSaveable { mutableStateOf("") }
    // Multiple photos
    val selectedPhotoUris = remember { mutableStateListOf<Uri>() }

    var timeError by remember { mutableStateOf<String?>(null) }

    val currentProject = userProjects.firstOrNull { it.id == selectedProject }
    val currentTasks   = userTasksForProject(currentProject)
    val currentTask    = currentTasks.firstOrNull { it.id == selectedTaskId }

    var showDatePicker  by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.of("UTC"))
                            .toLocalDate()
                        selectedDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    }
                    showDatePicker = false
                }) { Text(appText("OK", "OK")) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(appText("Cancel", "Cancelar"))
                }
            }
        ) { DatePicker(state = datePickerState) }
    }

    LaunchedEffect(Unit) { dashboardViewModel.loadDashboard() }

    LaunchedEffect(taskId, projectsWithTasks) {
        if (taskId != 0L) {
            val task = projectsWithTasks.values.flatten().firstOrNull { it.id == taskId }
            task?.let { t ->
                selectedTaskId  = t.id
                progressValue   = t.completion_rate ?: 0
                selectedDate    = t.start_date ?: ""
                selectedProject = projectsWithTasks.entries
                    .firstOrNull { it.value.any { task -> task.id == t.id } }?.key?.id
            }
        }
    }
    LaunchedEffect(userProjects) {
        if (selectedProject == null && userProjects.isNotEmpty())
            selectedProject = userProjects.first().id
    }
    LaunchedEffect(selectedProject, projectsWithTasks) {
        val tasks = userTasksForProject(currentProject)
        if (selectedTaskId == null && tasks.isNotEmpty()) {
            selectedTaskId = tasks.first().id
            progressValue  = tasks.first().completion_rate ?: 0
            selectedDate   = tasks.first().start_date ?: ""
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // Sair da tela após guardar com sucesso
    LaunchedEffect(saveResult) {
        saveResult?.let {
            if (it.startsWith("✓")) {
                notes = ""
                selectedPhotoUris.clear()
                timeHours = ""
                timeMinutes = ""
                onBack()
            } else {
                snackbarHostState.showSnackbar(it)
                progressViewModel.clearResult()
            }
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> selectedPhotoUris.addAll(uris) }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            ForgePlanTopBar(title = "ForgePlan", initials = SessionManager.userInitials)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = if (isLandscape) 32.dp else 22.dp,
                        vertical   = if (isLandscape) 14.dp else 18.dp
                    )
            ) {
                Text(
                    text       = appText("Progress", "Progresso"),
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onBackground
                )

                Spacer(Modifier.height(if (isLandscape) 12.dp else 18.dp))

                if (isLoading) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (isLandscape) {
                    if (taskId == 0L) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ProjectSelector(currentProject, userProjects, Modifier.weight(1f)) {
                                selectedProject = it.id; selectedTaskId = null
                            }
                            TaskSelector(currentTask, currentTasks, Modifier.weight(1f)) {
                                selectedTaskId = it.id; progressValue = it.completion_rate ?: 0; selectedDate = it.start_date ?: ""
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.weight(1f)) {
                            ProgressMainCard(progressValue) { progressValue = it }
                            Spacer(Modifier.height(12.dp))
                            DateRow(selectedDate) { showDatePicker = true }
                            Spacer(Modifier.height(12.dp))
                            TimeSpentFreeInput(timeHours, timeMinutes, timeError,
                                onHoursChange = { timeHours = it; timeError = null },
                                onMinutesChange = { timeMinutes = it; timeError = null }
                            )
                            Spacer(Modifier.height(12.dp))
                            LocationRow(location) { location = it }
                        }
                        Column(Modifier.weight(1f)) {
                            MultiPhotoAttachmentCard(
                                photoUris = selectedPhotoUris,
                                onAddPhotos = { photoLauncher.launch("image/*") },
                                onRemovePhoto = { selectedPhotoUris.remove(it) }
                            )
                            Spacer(Modifier.height(12.dp))
                            NotesCard(notes) { notes = it }
                        }
                    }
                } else {
                    if (taskId == 0L) {
                        ProjectSelector(currentProject, userProjects) {
                            selectedProject = it.id; selectedTaskId = null
                        }
                        Spacer(Modifier.height(12.dp))
                        TaskSelector(currentTask, currentTasks) {
                            selectedTaskId = it.id; progressValue = it.completion_rate ?: 0; selectedDate = it.start_date ?: ""
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                    ProgressMainCard(progressValue) { progressValue = it }
                    Spacer(Modifier.height(20.dp))
                    DateRow(selectedDate) { showDatePicker = true }
                    Spacer(Modifier.height(12.dp))
                    TimeSpentFreeInput(timeHours, timeMinutes, timeError,
                        onHoursChange = { timeHours = it; timeError = null },
                        onMinutesChange = { timeMinutes = it; timeError = null }
                    )
                    Spacer(Modifier.height(12.dp))
                    LocationRow(location) { location = it }
                    Spacer(Modifier.height(12.dp))
                    MultiPhotoAttachmentCard(
                        photoUris = selectedPhotoUris,
                        onAddPhotos = { photoLauncher.launch("image/*") },
                        onRemovePhoto = { selectedPhotoUris.remove(it) }
                    )
                    Spacer(Modifier.height(14.dp))
                    NotesCard(notes) { notes = it }
                }

                Spacer(Modifier.height(18.dp))

                Button(
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape    = RoundedCornerShape(8.dp),
                    enabled  = !isSaving,
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor   = MaterialTheme.colorScheme.onPrimary
                    ),
                    onClick = {
                        val task = currentTask ?: return@Button
                        val h = timeHours.toIntOrNull() ?: 0
                        val m = timeMinutes.toIntOrNull() ?: 0
                        val totalMinutes = h * 60 + m

                        if (totalMinutes == 0) {
                            timeError = appText(
                                "You must log at least some time spent.",
                                "Tens de registar pelo menos algum tempo gasto."
                            )
                            return@Button
                        }

                        progressViewModel.saveProgress(
                            task           = task,
                            logDate        = selectedDate,
                            location       = location,
                            completionRate = progressValue,
                            minutesSpent   = totalMinutes,
                            notes          = notes,
                            photoUri       = selectedPhotoUris.firstOrNull(),
                            context        = context,
                            successMsg     = appText("✓ Progress saved.", "✓ Progresso guardado."),
                            errorPrefix    = appText("Error saving", "Erro ao guardar")
                        )
                    }
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color    = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("⇧", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text       = appText("Save progress", "Guardar progresso"),
                            style      = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            ForgePlanBottomBar(
                selectedItem    = "Progress",
                onProjectsClick = onProjectsClick,
                onTimelineClick = onTimelineClick,
                onTeamClick     = onTeamClick
            )
        }
    }
}

// ── Time Spent (input livre) ───────────────────────────────────────────────────

@Composable
fun TimeSpentFreeInput(
    hours: String,
    minutes: String,
    error: String?,
    onHoursChange: (String) -> Unit,
    onMinutesChange: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.CheckCircle,
                null,
                Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Text(
                appText("Time spent", "Tempo gasto"),
                color    = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value         = hours,
                onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) onHoursChange(it) },
                label         = { Text("h") },
                modifier      = Modifier.width(72.dp),
                singleLine    = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape         = RoundedCornerShape(8.dp),
                isError       = error != null
            )
            Text(":", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            OutlinedTextField(
                value         = minutes,
                onValueChange = {
                    val v = it.toIntOrNull()
                    if (it.isEmpty() || (it.length <= 2 && it.all(Char::isDigit) && (v == null || v < 60)))
                        onMinutesChange(it)
                },
                label         = { Text("min") },
                modifier      = Modifier.width(78.dp),
                singleLine    = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape         = RoundedCornerShape(8.dp),
                isError       = error != null
            )
        }
        error?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ── Multi Photo Attachment ─────────────────────────────────────────────────────

@Composable
fun MultiPhotoAttachmentCard(
    photoUris: List<Uri>,
    onAddPhotos: () -> Unit,
    onRemovePhoto: (Uri) -> Unit
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(8.dp),
        color    = MaterialTheme.colorScheme.surface,
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("▧", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.size(8.dp))
                Text(
                    appText("Photo attachments", "Fotos anexadas"),
                    color    = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onAddPhotos) {
                    Text(appText("+ Add", "+ Adicionar"), style = MaterialTheme.typography.labelMedium)
                }
            }

            if (photoUris.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().height(80.dp).clickable { onAddPhotos() },
                    shape    = RoundedCornerShape(10.dp),
                    color    = MaterialTheme.colorScheme.background,
                    border   = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("▣", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f))
                        Text(
                            appText("Add Photos", "Adicionar fotos"),
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                photoUris.forEach { uri ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            getFileNameFromUri(context, uri),
                            modifier   = Modifier.weight(1f),
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines   = 1
                        )
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                                .clickable { onRemovePhoto(uri) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("×", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

// ── Campos existentes mantidos ────────────────────────────────────────────────

@Composable
fun DateRow(selectedDate: String, onPickDate: () -> Unit) {
    val displayText = if (selectedDate.isBlank()) {
        appText("Select date", "Selecionar data")
    } else {
        try {
            val parsed = LocalDate.parse(selectedDate, DateTimeFormatter.ISO_LOCAL_DATE)
            val fmt = DateTimeFormatter.ofPattern(
                appText("d MMMM yyyy", "d 'de' MMMM 'de' yyyy"),
                Locale(if (com.example.forgeplan.core.language.AppLanguage.isPortuguese()) "pt" else "en")
            )
            parsed.format(fmt)
        } catch (e: Exception) { selectedDate }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().height(46.dp).clickable { onPickDate() },
        shape    = RoundedCornerShape(8.dp),
        color    = MaterialTheme.colorScheme.surface,
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.size(10.dp))
            Text(displayText, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Icon(Icons.Outlined.KeyboardArrowDown, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun ProjectSelector(selectedProject: Project?, projects: List<Project>, modifier: Modifier = Modifier, onProjectSelected: (Project) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        SelectorCard(selectedProject?.name ?: appText("Select your project", "Selecionar projeto"), "□") { expanded = true }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (projects.isEmpty()) DropdownMenuItem(text = { Text(appText("No projects available", "Sem projetos disponíveis")) }, onClick = { expanded = false })
            projects.forEach { DropdownMenuItem(text = { Text(it.name) }, onClick = { onProjectSelected(it); expanded = false }) }
        }
    }
}

@Composable
fun TaskSelector(selectedTask: Task?, tasks: List<Task>, modifier: Modifier = Modifier, onTaskSelected: (Task) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        SelectorCard(selectedTask?.title ?: appText("Select your task", "Selecionar tarefa"), "☑") { expanded = true }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (tasks.isEmpty()) DropdownMenuItem(text = { Text(appText("Select a project first", "Seleciona um projeto primeiro")) }, onClick = { expanded = false })
            tasks.forEach { DropdownMenuItem(text = { Text(it.title) }, onClick = { onTaskSelected(it); expanded = false }) }
        }
    }
}

@Composable
fun SelectorCard(text: String, iconText: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(42.dp).clickable { onClick() },
        shape    = RoundedCornerShape(6.dp),
        color    = MaterialTheme.colorScheme.surface,
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(iconText, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.size(10.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.KeyboardArrowDown, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun ProgressMainCard(progress: Int, onProgressChange: (Int) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(132.dp),
        shape    = RoundedCornerShape(8.dp),
        color    = MaterialTheme.colorScheme.primary
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("◔", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.size(8.dp))
                Text(appText("Progress", "Progresso"), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                ProgressBadge(progress)
            }
            Spacer(Modifier.height(30.dp))
            Slider(
                value         = progress.toFloat(),
                onValueChange = { onProgressChange(it.toInt()) },
                valueRange    = 0f..100f,
                colors        = SliderDefaults.colors(
                    thumbColor         = MaterialTheme.colorScheme.tertiary,
                    activeTrackColor   = MaterialTheme.colorScheme.tertiary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.35f)
                )
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("0%",   color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                Text("100%", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun ProgressBadge(progress: Int) {
    Box(
        modifier         = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.tertiary).padding(horizontal = 10.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("${progress.coerceIn(0, 100)}%", color = MaterialTheme.colorScheme.onTertiary, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LocationRow(value: String, onValueChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val locations = listOf(
        appText("Workshop A", "Oficina A"),
        appText("Workshop B", "Oficina B"),
        appText("Office", "Escritório"),
        appText("Client site", "Cliente"),
        appText("Remote", "Remoto"),
        appText("Home", "Casa")
    )
    val displayValue = value.ifBlank { appText("Location", "Localização") }
    Box {
        Surface(
            modifier = Modifier.fillMaxWidth().height(46.dp).clickable { expanded = true },
            shape    = RoundedCornerShape(8.dp),
            color    = MaterialTheme.colorScheme.surface,
            border   = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
        ) {
            Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.size(10.dp))
                Text(displayValue, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.KeyboardArrowDown, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            locations.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { onValueChange(it); expanded = false }) }
        }
    }
}

@Composable
fun NotesCard(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        modifier      = Modifier.fillMaxWidth().height(150.dp),
        value         = value,
        onValueChange = onValueChange,
        placeholder   = { Text(appText("Write about your progress and any obstacles encountered.", "Escreve sobre o progresso e eventuais obstáculos encontrados.")) },
        label         = { Row(verticalAlignment = Alignment.CenterVertically) { Text("▤"); Spacer(Modifier.size(8.dp)); Text(appText("Notes", "Notas")) } },
        shape         = RoundedCornerShape(8.dp)
    )
}

private fun getFileNameFromUri(context: android.content.Context, uri: Uri): String {
    var fileName = "photo_attachment"
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (it.moveToFirst() && nameIndex >= 0) fileName = it.getString(nameIndex)
    }
    return fileName
}