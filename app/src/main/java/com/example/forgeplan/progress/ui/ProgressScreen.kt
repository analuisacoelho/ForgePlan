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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.session.SessionManager
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.tasks.viewmodel.UserDashboardViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Localização: app/src/main/java/com/example/forgeplan/progress/ui/ProgressScreen.kt
// SUBSTITUI completamente o ficheiro existente.
//
// Alterações principais:
//  1. Usa UserDashboardViewModel (já existente) para carregar apenas os
//     projectos e tarefas do utilizador logado via project_users e task_assignments
//  2. Adiciona campo de DATA com DatePickerDialog nativo
//  3. Mantém local, taxa de conclusão, tempo e notas existentes
//  4. Guarda todas as alterações na tarefa (status, completion_rate, start_date)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    taskId: Long,
    onProjectsClick: () -> Unit = {},
    onTimelineClick: () -> Unit = {},
    onTeamClick: () -> Unit = {},
    dashboardViewModel: UserDashboardViewModel = viewModel()
) {
    val configuration = LocalConfiguration.current
    val isLandscape   = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // ── Dados do utilizador (já filtrados por user) ──────────────────────────
    val projectsWithTasks by dashboardViewModel.projectsWithTasks.collectAsState()
    val isLoading         by dashboardViewModel.isLoading.collectAsState()

    // Lista plana de projectos e tarefas derivada do ViewModel
    val userProjects = remember(projectsWithTasks) { projectsWithTasks.keys.toList() }
    val userTasksForProject: (Project?) -> List<Task> = { proj ->
        if (proj == null) emptyList() else projectsWithTasks[proj] ?: emptyList()
    }

    // ── Estado do formulário (rememberSaveable → sobrevive a rotações) ───────
    var selectedProject   by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedTaskId    by rememberSaveable { mutableStateOf<Long?>(null) }
    var progressValue     by rememberSaveable { mutableStateOf(0) }
    var timeSpentHours    by rememberSaveable { mutableStateOf(0) }
    var location          by rememberSaveable { mutableStateOf("") }
    var notes             by rememberSaveable { mutableStateOf("") }
    var selectedPhotoUri  by remember { mutableStateOf<Uri?>(null) }
    var selectedDate      by rememberSaveable { mutableStateOf("") }  // "yyyy-MM-dd"
    var message           by remember { mutableStateOf<String?>(null) }

    // Objectos derivados do estado salvo
    val currentProject = userProjects.firstOrNull { it.id == selectedProject }
    val currentTasks   = userTasksForProject(currentProject)
    val currentTask    = currentTasks.firstOrNull { it.id == selectedTaskId }

    // ── DatePickerDialog ──────────────────────────────────────────────────────
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    LaunchedEffect(taskId, projectsWithTasks) {
        if (taskId != 0L) {
            val task = projectsWithTasks
                .values
                .flatten()
                .firstOrNull { it.id == taskId }

            task?.let { t ->
                selectedTaskId = t.id
                progressValue = t.completion_rate ?: 0
                selectedDate = t.start_date ?: ""

                // encontrar o projeto automaticamente
                val project = projectsWithTasks
                    .entries
                    .firstOrNull { it.value.any { task -> task.id == t.id } }
                    ?.key

                selectedProject = project?.id
            }
        }
    }
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
                }) {
                    Text(appText("OK", "OK"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(appText("Cancel", "Cancelar"))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ── Carrega dados ao entrar ───────────────────────────────────────────────
    LaunchedEffect(Unit) {
        dashboardViewModel.loadDashboard()
    }

    // Selecciona automaticamente o primeiro projecto/tarefa quando chegam dados
    LaunchedEffect(userProjects) {
        if (selectedProject == null && userProjects.isNotEmpty()) {
            val first = userProjects.first()
            selectedProject = first.id
        }
    }
    LaunchedEffect(selectedProject, projectsWithTasks) {
        val tasks = userTasksForProject(currentProject)
        if (selectedTaskId == null && tasks.isNotEmpty()) {
            selectedTaskId  = tasks.first().id
            progressValue   = tasks.first().completion_rate ?: 0
            selectedDate    = tasks.first().start_date ?: ""
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ForgePlanTopBar(
            title    = "ForgePlan",
            initials = SessionManager.userInitials
        )

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
                // ── Landscape: 2 colunas ──────────────────────────────────

                if (taskId == 0L) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                        ProjectSelector(
                            selectedProject = currentProject,
                            projects = userProjects,
                            modifier = Modifier.weight(1f),
                            onProjectSelected = {
                                selectedProject = it.id
                                selectedTaskId = null
                                message = null
                            }
                        )
                        TaskSelector(
                            selectedTask = currentTask,
                            tasks = currentTasks,
                            modifier = Modifier.weight(1f),
                            onTaskSelected = {
                                selectedTaskId = it.id
                                progressValue = it.completion_rate ?: 0
                                selectedDate = it.start_date ?: ""
                                message = null
                            }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f)) {
                        ProgressMainCard(progress = progressValue, onProgressChange = { progressValue = it; message = null })
                        Spacer(Modifier.height(12.dp))
                        DateRow(selectedDate = selectedDate, onPickDate = { showDatePicker = true })
                        Spacer(Modifier.height(12.dp))
                        TimeSpentRow(hours = timeSpentHours, onIncrease = { timeSpentHours++ }, onDecrease = { if (timeSpentHours > 0) timeSpentHours-- })
                        Spacer(Modifier.height(12.dp))
                        LocationRow(value = location, onValueChange = { location = it })
                    }
                    Column(Modifier.weight(1f)) {
                        PhotoAttachmentCard(selectedPhotoUri = selectedPhotoUri, onPhotoSelected = { selectedPhotoUri = it }, onRemovePhoto = { selectedPhotoUri = null })
                        Spacer(Modifier.height(12.dp))
                        NotesCard(value = notes, onValueChange = { notes = it })
                    }
                }
            } else {
                // ── Portrait: coluna única ────────────────────────────────
                if (taskId == 0L) {ProjectSelector(
                    selectedProject = currentProject,
                    projects        = userProjects,
                    onProjectSelected = {
                        selectedProject = it.id
                        selectedTaskId  = null
                        message         = null
                    }
                )

                Spacer(Modifier.height(12.dp))

                TaskSelector(
                    selectedTask = currentTask,
                    tasks        = currentTasks,
                    onTaskSelected = {
                        selectedTaskId = it.id
                        progressValue  = it.completion_rate ?: 0
                        selectedDate   = it.start_date ?: ""
                        message        = null
                    }
                )}

                Spacer(Modifier.height(28.dp))

                ProgressMainCard(progress = progressValue, onProgressChange = { progressValue = it; message = null })

                Spacer(Modifier.height(20.dp))

                // ── DATA ─────────────────────────────────────────────────
                DateRow(selectedDate = selectedDate, onPickDate = { showDatePicker = true })

                Spacer(Modifier.height(12.dp))

                TimeSpentRow(hours = timeSpentHours, onIncrease = { timeSpentHours++ }, onDecrease = { if (timeSpentHours > 0) timeSpentHours-- })

                Spacer(Modifier.height(12.dp))

                LocationRow(value = location, onValueChange = { location = it })

                Spacer(Modifier.height(12.dp))

                PhotoAttachmentCard(selectedPhotoUri = selectedPhotoUri, onPhotoSelected = { selectedPhotoUri = it }, onRemovePhoto = { selectedPhotoUri = null })

                Spacer(Modifier.height(14.dp))

                NotesCard(value = notes, onValueChange = { notes = it })
            }

            // ── Mensagens de erro/sucesso ─────────────────────────────────
            message?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text  = it,
                    color = if (it.startsWith("✓") || it.contains("sucesso") || it.contains("success"))
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(18.dp))

            // ── Botão Guardar ─────────────────────────────────────────────
            Button(
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape    = RoundedCornerShape(8.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor   = MaterialTheme.colorScheme.onPrimary
                ),
                onClick = {
                    val task = currentTask

                    if (task == null) {
                        message = appText(
                            "Select a task before saving.",
                            "Seleciona uma tarefa antes de guardar."
                        )
                        return@Button
                    }

                    // 1️⃣ Guardar progresso (tasks)
                    val newStatus = when {
                        progressValue >= 100 -> "Done"
                        progressValue > 0    -> "IN_PROGRESS"
                        else                 -> "PENDING"
                    }

                    val updatedTask = task.copy(
                        completion_rate = progressValue,
                        status          = newStatus,
                        start_date      = selectedDate.ifBlank { task.start_date },
                    )

                    dashboardViewModel.updateTaskProgress(
                        task = updatedTask,
                        onSuccess = {

                            // 2️⃣ Guardar comentário (comments)
                            if (notes.isNotBlank()) {
                                dashboardViewModel.insertComment(
                                    taskId = task.id,
                                    content = notes,
                                    onSuccess = {
                                        message = appText(
                                            "✓ Progress and comment saved.",
                                            "✓ Progresso e comentário guardados."
                                        )
                                        notes = "" // limpa campo
                                    },
                                    onError = { err ->
                                        message = err
                                    }
                                )
                            } else {
                                message = appText(
                                    "✓ Progress saved.",
                                    "✓ Progresso guardado."
                                )
                            }
                        },
                        onError = { err ->
                            message = err
                        }
                    )
                }
            ) {
                Text("⇧", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.size(8.dp))
                Text(
                    text       = appText("Save progress", "Guardar progresso"),
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
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

// ── Campo de Data ────────────────────────────────────────────────────────────

@Composable
fun DateRow(
    selectedDate: String,
    onPickDate: () -> Unit
) {
    val displayText = if (selectedDate.isBlank()) {
        appText("Select date", "Selecionar data")
    } else {
        // Converte de "yyyy-MM-dd" para formato legível
        try {
            val parsed = LocalDate.parse(selectedDate, DateTimeFormatter.ISO_LOCAL_DATE)
            val fmt = DateTimeFormatter.ofPattern(
                appText("d MMMM yyyy", "d 'de' MMMM 'de' yyyy"),
                Locale(if (com.example.forgeplan.core.language.AppLanguage.isPortuguese()) "pt" else "en")
            )
            parsed.format(fmt)
        } catch (e: Exception) {
            selectedDate
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clickable { onPickDate() },
        shape  = RoundedCornerShape(8.dp),
        color  = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                modifier           = Modifier.size(20.dp),
                tint               = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text     = displayText,
                color    = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                style    = MaterialTheme.typography.bodyMedium
            )
            Icon(
                imageVector        = Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                modifier           = Modifier.size(20.dp),
                tint               = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ── Selectors (iguais ao original, mas com parâmetros de objecto) ────────────

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
            text      = selectedProject?.name ?: appText("Select your project", "Selecionar projeto"),
            iconText  = "□",
            onClick   = { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (projects.isEmpty()) {
                DropdownMenuItem(
                    text    = { Text(appText("No projects available", "Sem projectos disponíveis")) },
                    onClick = { expanded = false }
                )
            }
            projects.forEach { project ->
                DropdownMenuItem(
                    text    = { Text(project.name) },
                    onClick = { onProjectSelected(project); expanded = false }
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
            text     = selectedTask?.title ?: appText("Select your task", "Selecionar tarefa"),
            iconText = "☑",
            onClick  = { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (tasks.isEmpty()) {
                DropdownMenuItem(
                    text    = { Text(appText("Select a project first", "Seleciona um projecto primeiro")) },
                    onClick = { expanded = false }
                )
            }
            tasks.forEach { task ->
                DropdownMenuItem(
                    text    = { Text(task.title) },
                    onClick = { onTaskSelected(task); expanded = false }
                )
            }
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
            Text(text = iconText, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.size(10.dp))
            Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Icon(imageVector = Icons.Outlined.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// ── Componentes iguais ao original ───────────────────────────────────────────

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
fun TimeSpentRow(hours: Int, onIncrease: () -> Unit, onDecrease: () -> Unit) {
    val hourText = if (hours == 1) appText("1 hour", "1 hora") else appText("$hours hours", "$hours horas")
    Surface(
        modifier = Modifier.fillMaxWidth().height(46.dp),
        shape    = RoundedCornerShape(8.dp),
        color    = MaterialTheme.colorScheme.surface,
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CheckCircle, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.size(10.dp))
            Text(appText("Time spent", "Tempo gasto"), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.tertiary).padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text(hourText, color = MaterialTheme.colorScheme.onTertiary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.size(8.dp))
            Column {
                Text("▲", modifier = Modifier.clickable { onIncrease() }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                Text("▼", modifier = Modifier.clickable { onDecrease() }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
            }
        }
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
        appText("Remote", "Remoto")
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
            locations.forEach {
                DropdownMenuItem(text = { Text(it) }, onClick = { onValueChange(it); expanded = false })
            }
        }
    }
}

@Composable
fun PhotoAttachmentCard(selectedPhotoUri: Uri?, onPhotoSelected: (Uri) -> Unit, onRemovePhoto: () -> Unit) {
    val context = LocalContext.current
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { onPhotoSelected(it) } }
    Surface(
        modifier = Modifier.fillMaxWidth().height(164.dp),
        shape    = RoundedCornerShape(8.dp),
        color    = MaterialTheme.colorScheme.surface,
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("▧", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.size(8.dp))
                Text(appText("Photo attachment", "Anexo fotográfico"), color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(12.dp))
            if (selectedPhotoUri == null) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(92.dp).clickable { photoLauncher.launch("image/*") },
                    shape    = RoundedCornerShape(10.dp),
                    color    = MaterialTheme.colorScheme.background,
                    border   = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("▣", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f))
                        Spacer(Modifier.height(6.dp))
                        Text(appText("Add Photo", "Adicionar foto"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().height(92.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.secondaryContainer).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(getFileNameFromUri(context, selectedPhotoUri), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error).clickable { onRemovePhoto() }, contentAlignment = Alignment.Center) {
                        Text("×", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun NotesCard(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        modifier    = Modifier.fillMaxWidth().height(150.dp),
        value       = value,
        onValueChange = onValueChange,
        placeholder = { Text(appText("Here you can write about your progress and any obstacles you may have encountered.", "Aqui podes escrever sobre o progresso e eventuais obstáculos encontrados.")) },
        label       = { Row(verticalAlignment = Alignment.CenterVertically) { Text("▤"); Spacer(Modifier.size(8.dp)); Text(appText("Notes", "Notas")) } },
        shape       = RoundedCornerShape(8.dp)
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