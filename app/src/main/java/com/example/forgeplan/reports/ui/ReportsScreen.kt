package com.example.forgeplan.reports.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalConfiguration
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.model.Comment
import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.model.ProjectUser
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.model.TaskAttachment
import com.example.forgeplan.core.model.TaskLog
import com.example.forgeplan.core.model.TaskPhoto
import com.example.forgeplan.core.model.User
import com.example.forgeplan.core.repository.CommentRepository
import com.example.forgeplan.core.repository.ProjectUserRepository
import com.example.forgeplan.core.repository.TaskAssignmentRepository
import com.example.forgeplan.core.repository.TaskAttachmentRepository
import com.example.forgeplan.core.repository.TaskLogRepository
import com.example.forgeplan.core.repository.TaskRepository
import com.example.forgeplan.core.ui.components.ForgeMiniChip
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.projects.viewmodel.ProjectViewModel
import com.example.forgeplan.tasks.viewmodel.UserViewModel
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import android.graphics.Color as AndroidColor

data class ReportTaskStats(val total: Int, val todo: Int, val active: Int, val done: Int)
data class PendingPdfExport(val type: String, val fileName: String)
data class PendingCsvExport(val type: String, val fileName: String)

@Composable
fun ReportsScreen(
    showScaffold: Boolean = true,
    onProjectsClick: () -> Unit = {},
    onTimelineClick: () -> Unit = {},
    onTeamClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    projectViewModel: ProjectViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {
    val context = LocalContext.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val projects by projectViewModel.projects.collectAsState()
    val users by userViewModel.users.collectAsState()
    val projectError by projectViewModel.error.collectAsState()
    val userError by userViewModel.error.collectAsState()

    val taskRepository = remember { TaskRepository() }
    val projectUserRepository = remember { ProjectUserRepository() }
    val taskAssignmentRepository = remember { TaskAssignmentRepository() }
    val taskLogRepository = remember { TaskLogRepository() }
    val commentRepository = remember { CommentRepository() }
    val taskAttachmentRepository = remember { TaskAttachmentRepository() }

    val projectTasks = remember { mutableStateMapOf<Long, List<Task>>() }
    val projectUsers = remember { mutableStateMapOf<Long, List<ProjectUser>>() }
    val selectedUserTaskIds = remember { mutableStateListOf<Long>() }

    // Horas reais: task_id → lista de logs
    val taskLogs = remember { mutableStateMapOf<Long, List<TaskLog>>() }

    // log_id → fotos desse log
    val logPhotos = remember { mutableStateMapOf<Long, List<TaskPhoto>>() }

    // Attachments da tarefa selecionada
    val taskAttachments = remember { mutableStateListOf<TaskAttachment>() }

    // Comentários da tarefa selecionada
    val taskComments = remember { mutableStateListOf<Comment>() }

    var selectedType by remember { mutableStateOf("PROJECT") }
    var selectedProject by remember { mutableStateOf<Project?>(null) }
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }

    var pendingPdfExport by remember { mutableStateOf<PendingPdfExport?>(null) }
    var pendingCsvExport by remember { mutableStateOf<PendingCsvExport?>(null) }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val export = pendingPdfExport
        if (uri != null && export != null) {
            writeReportPdf(context, uri, export.type, selectedProject, selectedUser, selectedTask, projects, projectTasks, projectUsers, selectedUserTaskIds, taskLogs)
        }
        pendingPdfExport = null
    }

    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val export = pendingCsvExport
        if (uri != null && export != null) {
            writeReportCsv(context, uri, export.type, selectedProject, selectedUser, selectedTask, projects, projectTasks, projectUsers, selectedUserTaskIds, taskLogs)
        }
        pendingCsvExport = null
    }

    LaunchedEffect(Unit) {
        projectViewModel.loadProjects()
        userViewModel.loadUsers()
    }

    LaunchedEffect(projects) {
        if (projects.isNotEmpty() && selectedProject == null) selectedProject = projects.first()
        projects.forEach { project ->
            taskRepository.getTasksByProjectId(
                projectId = project.id,
                onSuccess = { tasks ->
                    projectTasks[project.id] = tasks
                    // Para cada tarefa, vai buscar os logs reais
                    tasks.forEach { task ->
                        taskLogRepository.getLogsByTaskId(
                            taskId = task.id,
                            onSuccess = { logs -> taskLogs[task.id] = logs },
                            onError = { taskLogs[task.id] = emptyList() }
                        )
                    }
                },
                onError = { projectTasks[project.id] = emptyList() }
            )
            projectUserRepository.getProjectUsersByProjectId(
                projectId = project.id,
                onSuccess = { members -> projectUsers[project.id] = members },
                onError = { projectUsers[project.id] = emptyList() }
            )
        }
    }

    LaunchedEffect(users) {
        if (users.isNotEmpty() && selectedUser == null) selectedUser = users.first()
    }

    val allTasks = projectTasks.values.flatten()

    LaunchedEffect(allTasks.size) {
        if (allTasks.isNotEmpty() && selectedTask == null) selectedTask = allTasks.first()
    }

    LaunchedEffect(selectedUser?.id) {
        selectedUserTaskIds.clear()
        selectedUser?.let { user ->
            taskAssignmentRepository.getTaskIdsByUserId(
                userId = user.id,
                onSuccess = { ids -> selectedUserTaskIds.clear(); selectedUserTaskIds.addAll(ids) },
                onError = { selectedUserTaskIds.clear() }
            )
        }
    }

    // Quando muda a tarefa selecionada, vai buscar comentários e attachments
    LaunchedEffect(selectedTask?.id) {
        taskComments.clear()
        taskAttachments.clear()
        selectedTask?.let { task ->
            commentRepository.getCommentsByTaskId(
                taskId = task.id,
                onSuccess = { comments -> taskComments.clear(); taskComments.addAll(comments) },
                onError = { taskComments.clear() }
            )
            taskAttachmentRepository.getAttachmentsByTaskId(
                taskId = task.id,
                onSuccess = { attachments -> taskAttachments.clear(); taskAttachments.addAll(attachments) },
                onError = { taskAttachments.clear() }
            )
            // Carrega logs da tarefa selecionada e as fotos de cada log
            taskLogRepository.getLogsByTaskId(
                taskId = task.id,
                onSuccess = { logs ->
                    taskLogs[task.id] = logs
                    logs.forEach { log ->
                        taskLogRepository.getPhotosByLogId(
                            taskLogId = log.id,
                            onSuccess = { photos -> logPhotos[log.id] = photos },
                            onError = { logPhotos[log.id] = emptyList() }
                        )
                    }
                },
                onError = {}
            )
        }
    }

    val selectedProjectTasks = selectedProject?.let { projectTasks[it.id] ?: emptyList() } ?: emptyList()
    val selectedUserTasks = allTasks.filter { task -> selectedUserTaskIds.contains(task.id) }
    val selectedTaskList = selectedTask?.let { listOf(it) } ?: emptyList()

    val displayedStats = when (selectedType) {
        "USER" -> calculateReportTaskStats(selectedUserTasks)
        "TASK" -> calculateReportTaskStats(selectedTaskList)
        else -> calculateReportTaskStats(selectedProjectTasks)
    }

    val selectedProjectTeamCount = selectedProject?.let { projectUsers[it.id]?.size ?: 0 } ?: 0

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        if (showScaffold) {
            ForgePlanTopBar(title = appText(en = "Reports", pt = "Relatórios"), initials = "FP")
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isLandscape) 42.dp else 18.dp, vertical = if (isLandscape) 20.dp else 18.dp)
                .padding(bottom = if (showScaffold) 96.dp else 16.dp)
        ) {
            Text(text = appText(en = "Reports & Statistics", pt = "Relatórios e Estatísticas"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = appText(en = "Export data by project, user, or task", pt = "Exporta dados por projeto, utilizador ou tarefa"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f))
            Spacer(modifier = Modifier.height(18.dp))

            ReportsTypeSelector(selectedType = selectedType, onTypeSelected = { selectedType = it })
            Spacer(modifier = Modifier.height(16.dp))

            when (selectedType) {
                "USER" -> ReportsDropdownCard(
                    title = appText(en = "Select user", pt = "Selecionar utilizador"),
                    selectedText = selectedUser?.let { "${it.name} - ${formatRole(it.role)}" } ?: appText(en = "No user selected", pt = "Nenhum utilizador selecionado"),
                    items = users, itemText = { "${it.name} - ${formatRole(it.role)}" }, onItemSelected = { selectedUser = it }
                )
                "TASK" -> ReportsDropdownCard(
                    title = appText(en = "Select task", pt = "Selecionar tarefa"),
                    selectedText = selectedTask?.let { task -> "${task.title} (${projectNameById(projects, task.project_id)})" } ?: appText(en = "No task selected", pt = "Nenhuma tarefa selecionada"),
                    items = allTasks, itemText = { task -> "${task.title} (${projectNameById(projects, task.project_id)})" }, onItemSelected = { selectedTask = it }
                )
                else -> ReportsDropdownCard(
                    title = appText(en = "Select project", pt = "Selecionar projeto"),
                    selectedText = selectedProject?.name ?: appText(en = "No project selected", pt = "Nenhum projeto selecionado"),
                    items = projects, itemText = { it.name }, onItemSelected = { selectedProject = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            projectError?.let { Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium); Spacer(modifier = Modifier.height(10.dp)) }
            userError?.let { Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium); Spacer(modifier = Modifier.height(10.dp)) }

            when (selectedType) {
                "USER" -> UserReportContent(
                    selectedUser = selectedUser, userTasks = selectedUserTasks, projects = projects,
                    projectCount = selectedUserTasks.map { it.project_id }.distinct().size,
                    stats = displayedStats, isLandscape = isLandscape,
                    taskLogs = taskLogs, users = users,
                    onExportPdf = { val name = selectedUser?.name?.safeFileName() ?: "user"; pendingPdfExport = PendingPdfExport("USER", "forgeplan_user_report_$name.pdf"); pdfLauncher.launch(pendingPdfExport!!.fileName) },
                    onExportCsv = { val name = selectedUser?.name?.safeFileName() ?: "user"; pendingCsvExport = PendingCsvExport("USER", "forgeplan_user_report_$name.csv"); csvLauncher.launch(pendingCsvExport!!.fileName) }
                )
                "TASK" -> TaskReportContent(
                    selectedTask = selectedTask,
                    projectName = selectedTask?.let { projectNameById(projects, it.project_id) }.orEmpty(),
                    comments = taskComments,
                    taskLogs = selectedTask?.let { taskLogs[it.id] } ?: emptyList(),
                    taskAttachments = taskAttachments,
                    logPhotos = logPhotos,
                    users = users,
                    onExportPdf = { val name = selectedTask?.title?.safeFileName() ?: "task"; pendingPdfExport = PendingPdfExport("TASK", "forgeplan_task_report_$name.pdf"); pdfLauncher.launch(pendingPdfExport!!.fileName) },
                    onExportCsv = { val name = selectedTask?.title?.safeFileName() ?: "task"; pendingCsvExport = PendingCsvExport("TASK", "forgeplan_task_report_$name.csv"); csvLauncher.launch(pendingCsvExport!!.fileName) }
                )
                else -> ProjectReportContent(
                    selectedProject = selectedProject, tasks = selectedProjectTasks,
                    teamCount = selectedProjectTeamCount, stats = displayedStats, isLandscape = isLandscape,
                    taskLogs = taskLogs,
                    onExportPdf = { val name = selectedProject?.name?.safeFileName() ?: "project"; pendingPdfExport = PendingPdfExport("PROJECT", "forgeplan_project_report_$name.pdf"); pdfLauncher.launch(pendingPdfExport!!.fileName) },
                    onExportCsv = { val name = selectedProject?.name?.safeFileName() ?: "project"; pendingCsvExport = PendingCsvExport("PROJECT", "forgeplan_project_report_$name.csv"); csvLauncher.launch(pendingCsvExport!!.fileName) }
                )
            }
        }

        if (showScaffold) {
            ForgePlanBottomBar(selectedItem = "Reports", onProjectsClick = onProjectsClick, onTimelineClick = onTimelineClick, onProgressClick = {}, onTeamClick = onTeamClick, onProfileClick = onProfileClick)
        }
    }
}

// ─────────────────────────────────────────────────────────
// Horas reais a partir dos task_logs
// ─────────────────────────────────────────────────────────

private fun realHoursForTasks(taskIds: List<Long>, taskLogs: Map<Long, List<TaskLog>>): Int {
    val totalMinutes = taskIds.sumOf { taskId ->
        taskLogs[taskId]?.sumOf { it.minutes_spent ?: 0 } ?: 0
    }
    return totalMinutes / 60
}

private fun realHoursForUser(userId: Long, taskIds: List<Long>, taskLogs: Map<Long, List<TaskLog>>): Int {
    val totalMinutes = taskIds.sumOf { taskId ->
        taskLogs[taskId]?.filter { it.user_id == userId }?.sumOf { it.minutes_spent ?: 0 } ?: 0
    }
    return totalMinutes / 60
}

// ─────────────────────────────────────────────────────────
// Conteúdo By Project
// ─────────────────────────────────────────────────────────

@Composable
fun ProjectReportContent(
    selectedProject: Project?, tasks: List<Task>, teamCount: Int,
    stats: ReportTaskStats, isLandscape: Boolean,
    taskLogs: Map<Long, List<TaskLog>>,
    onExportPdf: () -> Unit, onExportCsv: () -> Unit
) {
    val totalHours = realHoursForTasks(tasks.map { it.id }, taskLogs)

    ReportMetricsCard(
        title = selectedProject?.let { "Projeto: ${it.name}" } ?: appText(en = "Project", pt = "Projeto"),
        metrics = listOf(
            appText(en = "Total Tasks", pt = "Total de Tarefas") to stats.total.toString(),
            appText(en = "Completed", pt = "Concluídas") to stats.done.toString(),
            appText(en = "Total Hours", pt = "Horas Totais") to "${totalHours}h",
            appText(en = "Team Members", pt = "Membros") to teamCount.toString()
        )
    )
    Spacer(modifier = Modifier.height(16.dp))
    ReportSectionCard(title = appText(en = "Task Status Distribution", pt = "Distribuição do Estado das Tarefas")) {
        TaskStatusDonutChart(stats = stats, modifier = Modifier.fillMaxWidth().height(if (isLandscape) 320.dp else 400.dp))
    }
    Spacer(modifier = Modifier.height(16.dp))
    ReportSectionCard(title = appText(en = "Project Tasks", pt = "Tarefas do Projeto")) {
        if (tasks.isEmpty()) {
            Text(text = appText(en = "No tasks found.", pt = "Não existem tarefas."), color = MaterialTheme.colorScheme.onSurface)
        } else {
            tasks.forEach { task -> TaskReportRow(task = task); Spacer(modifier = Modifier.height(10.dp)) }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    ExportReportCard(title = appText(en = "Export Project Report", pt = "Exportar Relatório do Projeto"), onExportPdf = onExportPdf, onExportCsv = onExportCsv)
}

// ─────────────────────────────────────────────────────────
// Conteúdo By User
// ─────────────────────────────────────────────────────────

@Composable
fun UserReportContent(
    selectedUser: User?, userTasks: List<Task>, projects: List<Project>,
    projectCount: Int, stats: ReportTaskStats, isLandscape: Boolean,
    taskLogs: Map<Long, List<TaskLog>>, users: List<User>,
    onExportPdf: () -> Unit, onExportCsv: () -> Unit
) {
    val totalHours = selectedUser?.let {
        realHoursForUser(it.id, userTasks.map { t -> t.id }, taskLogs)
    } ?: 0

    ReportMetricsCard(
        title = selectedUser?.let { "Utilizador: ${it.name}" } ?: appText(en = "User", pt = "Utilizador"),
        subtitle = selectedUser?.let { "Role: ${formatRole(it.role)}" },
        metrics = listOf(
            appText(en = "Total Tasks", pt = "Total de Tarefas") to stats.total.toString(),
            appText(en = "Completed", pt = "Concluídas") to stats.done.toString(),
            appText(en = "Total Hours", pt = "Horas Totais") to "${totalHours}h",
            appText(en = "Projects", pt = "Projetos") to projectCount.toString()
        )
    )
    Spacer(modifier = Modifier.height(16.dp))
    ReportSectionCard(title = appText(en = "Tasks & Hours by Project", pt = "Tarefas e Horas por Projeto")) {
        UserProjectsTable(tasks = userTasks, projects = projects, userId = selectedUser?.id, taskLogs = taskLogs)
    }
    Spacer(modifier = Modifier.height(16.dp))
    ReportSectionCard(title = appText(en = "Performance Trend", pt = "Tendência de Performance")) {
        PerformanceLineChart(tasks = userTasks, modifier = Modifier.fillMaxWidth().height(if (isLandscape) 250.dp else 280.dp))
    }
    Spacer(modifier = Modifier.height(16.dp))
    ExportReportCard(title = appText(en = "Export User Report", pt = "Exportar Relatório do Utilizador"), onExportPdf = onExportPdf, onExportCsv = onExportCsv)
}

@Composable
fun UserProjectsTable(tasks: List<Task>, projects: List<Project>, userId: Long?, taskLogs: Map<Long, List<TaskLog>>) {
    val grouped = tasks.groupBy { it.project_id }
    if (grouped.isEmpty()) {
        Text(text = appText(en = "No tasks assigned to this user.", pt = "Não existem tarefas atribuídas a este utilizador."), color = MaterialTheme.colorScheme.onSurface)
        return
    }
    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
            TableHeader(text = appText(en = "Project", pt = "Projeto"), modifier = Modifier.weight(2f))
            TableHeader(text = appText(en = "Tasks", pt = "Tarefas"), modifier = Modifier.weight(1f))
            TableHeader(text = appText(en = "Hours", pt = "Horas"), modifier = Modifier.weight(1f))
            TableHeader(text = appText(en = "Avg.", pt = "Média"), modifier = Modifier.weight(1f))
        }
        grouped.forEach { entry ->
            val projectName = projectNameById(projects, entry.key)
            val taskCount = entry.value.size
            val hours = if (userId != null)
                realHoursForUser(userId, entry.value.map { it.id }, taskLogs)
            else
                realHoursForTasks(entry.value.map { it.id }, taskLogs)
            val average = if (taskCount == 0) 0f else hours.toFloat() / taskCount.toFloat()
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                TableCell(text = projectName, modifier = Modifier.weight(2f))
                TableCell(text = taskCount.toString(), modifier = Modifier.weight(1f))
                TableCell(text = "${hours}h", modifier = Modifier.weight(1f))
                TableCell(text = String.format(Locale.US, "%.1fh", average), modifier = Modifier.weight(1f))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Conteúdo By Task — com comentários e logs/evidências
// ─────────────────────────────────────────────────────────

@Composable
fun TaskReportContent(
    selectedTask: Task?, projectName: String,
    comments: List<Comment>, taskLogs: List<TaskLog>,
    taskAttachments: List<TaskAttachment>, logPhotos: Map<Long, List<TaskPhoto>>,
    users: List<User>,
    onExportPdf: () -> Unit, onExportCsv: () -> Unit
) {
    val task = selectedTask
    if (task == null) {
        ReportSectionCard(title = appText(en = "Task details", pt = "Detalhes da tarefa")) {
            Text(text = appText(en = "No task selected.", pt = "Nenhuma tarefa selecionada."), color = MaterialTheme.colorScheme.onSurface)
        }
        return
    }

    val totalMinutes = taskLogs.sumOf { it.minutes_spent ?: 0 }
    val totalHours = totalMinutes / 60
    val totalMinutesRem = totalMinutes % 60

    ReportMetricsCard(
        title = task.title,
        subtitle = appText(en = "Project: $projectName", pt = "Projeto: $projectName"),
        metrics = listOf(
            appText(en = "Status", pt = "Estado") to formatStatus(task.status),
            appText(en = "Progress", pt = "Progresso") to "${task.completion_rate ?: 0}%",
            appText(en = "Priority", pt = "Prioridade") to formatPriority(task.priority),
            appText(en = "Hours", pt = "Horas") to "${totalHours}h${if (totalMinutesRem > 0) " ${totalMinutesRem}m" else ""}"
        )
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Detalhes básicos
    ReportSectionCard(title = appText(en = "Task details", pt = "Detalhes da tarefa")) {
        Text(text = appText(en = "Description", pt = "Descrição"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = task.description ?: appText(en = "No description.", pt = "Sem descrição."), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
        Spacer(modifier = Modifier.height(14.dp))
        LinearProgressIndicator(
            progress = { ((task.completion_rate ?: 0).coerceIn(0, 100)) / 100f },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
            color = if ((task.completion_rate ?: 0) >= 100 || task.status?.uppercase() == "DONE") strongDoneColor() else strongActiveColor(),
            trackColor = MaterialTheme.colorScheme.secondaryContainer
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ForgeMiniChip(text = appText(en = "Start: ${task.start_date ?: "-"}", pt = "Início: ${task.start_date ?: "-"}"))
            ForgeMiniChip(text = appText(en = "End: ${task.end_date ?: "-"}", pt = "Fim: ${task.end_date ?: "-"}"))
        }
    }

    // Logs de trabalho
    if (taskLogs.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        ReportSectionCard(title = appText(en = "Work Logs", pt = "Registos de Trabalho")) {
            taskLogs.forEachIndexed { index, log ->
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val userName = users.firstOrNull { it.id == log.user_id }?.name ?: "User #${log.user_id}"
                        Text(text = userName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = log.log_date ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        log.minutes_spent?.let { mins ->
                            ForgeMiniChip(text = "${mins / 60}h ${mins % 60}m")
                        }
                        log.completion_rate?.let { rate ->
                            ForgeMiniChip(text = "$rate%")
                        }
                        log.location?.let { loc ->
                            ForgeMiniChip(text = loc)
                        }
                    }
                    log.notes?.let { notes ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
                if (index < taskLogs.size - 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }

    // Comentários
    if (comments.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        ReportSectionCard(title = appText(en = "Comments", pt = "Comentários")) {
            comments.forEachIndexed { index, comment ->
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val userName = users.firstOrNull { it.id == comment.user_id }?.name ?: "User #${comment.user_id}"
                        Text(text = userName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = formatCommentDate(comment.created_at), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = comment.content ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
                }
                if (index < comments.size - 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }

    // Ficheiros anexados à tarefa
    if (taskAttachments.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        ReportSectionCard(title = appText(en = "Attachments", pt = "Ficheiros Anexados")) {
            val uriHandler = LocalUriHandler.current
            taskAttachments.forEachIndexed { index, attachment ->
                val url = attachment.file_url
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (url != null) Modifier.clickable { uriHandler.openUri(url) } else Modifier)
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = attachment.file_name ?: appText(en = "Unnamed file", pt = "Ficheiro sem nome"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (url != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        attachment.file_type?.let { type ->
                            Text(text = type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        attachment.uploaded_at?.let { date ->
                            Text(text = formatCommentDate(date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                    ForgeMiniChip(
                        text = appText(en = "Open", pt = "Abrir"),
                        containerColor = if (url != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (url != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (index < taskAttachments.size - 1) {
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
                }
            }
        }
    }

    // Fotos dos logs de trabalho
    val logsWithPhotos = taskLogs.filter { log -> (logPhotos[log.id]?.isNotEmpty() == true) }
    if (logsWithPhotos.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        ReportSectionCard(title = appText(en = "Evidence Photos", pt = "Fotos de Evidência")) {
            logsWithPhotos.forEachIndexed { logIndex, log ->
                val photos = logPhotos[log.id] ?: emptyList()
                val userName = users.firstOrNull { it.id == log.user_id }?.name ?: "User #${log.user_id}"
                Text(
                    text = "$userName — ${log.log_date ?: ""}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                val uriHandler = LocalUriHandler.current
                photos.forEachIndexed { photoIndex, photo ->
                    val photoUrl = photo.photo_url
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (photoUrl != null) Modifier.clickable { uriHandler.openUri(photoUrl) } else Modifier)
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📷 ${photo.photo_url ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        photo.captured_at?.let { date ->
                            Text(text = formatCommentDate(date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                }
                if (logIndex < logsWithPhotos.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    ExportReportCard(title = appText(en = "Export Task Report", pt = "Exportar Relatório da Tarefa"), onExportPdf = onExportPdf, onExportCsv = onExportCsv)
}

private fun formatCommentDate(dateString: String?): String {
    if (dateString == null) return ""
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val date = inputFormat.parse(dateString.substringBefore("."))
        outputFormat.format(date ?: return dateString)
    } catch (e: Exception) { dateString }
}

// ─────────────────────────────────────────────────────────
// Componentes partilhados (iguais ao original)
// ─────────────────────────────────────────────────────────

@Composable
fun ReportsTypeSelector(selectedType: String, onTypeSelected: (String) -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = appText(en = "Select report type", pt = "Selecionar tipo de relatório"), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ReportTypeButton(text = appText(en = "By Project", pt = "Por Projeto"), selected = selectedType == "PROJECT", onClick = { onTypeSelected("PROJECT") }, modifier = Modifier.weight(1f))
                ReportTypeButton(text = appText(en = "By User", pt = "Por Utilizador"), selected = selectedType == "USER", onClick = { onTypeSelected("USER") }, modifier = Modifier.weight(1f))
                ReportTypeButton(text = appText(en = "By Task", pt = "Por Tarefa"), selected = selectedType == "TASK", onClick = { onTypeSelected("TASK") }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ReportTypeButton(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(modifier = modifier.height(44.dp), onClick = onClick, shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
        )
    ) { Text(text = text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
}

@Composable
fun <T> ReportsDropdownCard(title: String, selectedText: String, items: List<T>, itemText: (T) -> String, onItemSelected: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(10.dp))
            Box {
                Surface(modifier = Modifier.fillMaxWidth().height(48.dp).clickable { expanded = true }, color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f))) {
                    Row(modifier = Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = selectedText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f), maxLines = 1)
                        Text(text = "⌄", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    items.forEach { item -> DropdownMenuItem(text = { Text(text = itemText(item), style = MaterialTheme.typography.bodyMedium) }, onClick = { onItemSelected(item); expanded = false }) }
                }
            }
        }
    }
}

@Composable
fun ReportMetricsCard(title: String, subtitle: String? = null, metrics: List<Pair<String, String>>) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            subtitle?.let { Spacer(modifier = Modifier.height(4.dp)); Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f), maxLines = 1) }
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                metrics.forEach { metric -> MetricColumn(metric = metric, modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MetricColumn(metric: Pair<String, String>, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(text = metric.second, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        Text(text = metric.first, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f), maxLines = 1)
    }
}

@Composable
fun ReportSectionCard(title: String, content: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
fun TableHeader(text: String, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f), maxLines = 1)
}

@Composable
fun TableCell(text: String, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
}

@Composable
fun TaskReportRow(task: Task) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = task.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = task.description ?: appText(en = "No description", pt = "Sem descrição"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f), maxLines = 1)
            }
            ForgeMiniChip(text = formatStatus(task.status), containerColor = statusChipColor(task.status), contentColor = Color.White)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { ((task.completion_rate ?: 0).coerceIn(0, 100)) / 100f },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50)),
            color = if ((task.completion_rate ?: 0) >= 100 || task.status?.uppercase() == "DONE") strongDoneColor() else strongActiveColor(),
            trackColor = MaterialTheme.colorScheme.secondaryContainer
        )
    }
}

@Composable
fun ExportReportCard(title: String, onExportPdf: () -> Unit, onExportCsv: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(modifier = Modifier.weight(1f), onClick = onExportPdf, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)) {
                    Text(text = appText(en = "Export PDF", pt = "Exportar PDF"), color = MaterialTheme.colorScheme.error)
                }
                OutlinedButton(modifier = Modifier.weight(1f), onClick = onExportCsv, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, strongDoneColor())) {
                    Text(text = appText(en = "Export CSV", pt = "Exportar CSV"), color = strongDoneColor())
                }
            }
        }
    }
}

@Composable
fun TaskStatusDonutChart(stats: ReportTaskStats, modifier: Modifier = Modifier) {
    val total = stats.total.coerceAtLeast(0)
    val todoColor = strongTodoColor(); val activeColor = strongActiveColor(); val doneColor = strongDoneColor()
    val emptyColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(210.dp)) {
                val stroke = Stroke(width = 26.dp.toPx(), cap = StrokeCap.Butt)
                if (total == 0) {
                    drawArc(color = emptyColor, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = stroke, size = Size(size.width, size.height))
                } else {
                    var startAngle = -90f
                    if (stats.todo > 0) { drawArc(color = todoColor, startAngle = startAngle, sweepAngle = 360f * stats.todo / total, useCenter = false, style = stroke, size = Size(size.width, size.height)); startAngle += 360f * stats.todo / total }
                    if (stats.active > 0) { drawArc(color = activeColor, startAngle = startAngle, sweepAngle = 360f * stats.active / total, useCenter = false, style = stroke, size = Size(size.width, size.height)); startAngle += 360f * stats.active / total }
                    if (stats.done > 0) { drawArc(color = doneColor, startAngle = startAngle, sweepAngle = 360f * stats.done / total, useCenter = false, style = stroke, size = Size(size.width, size.height)) }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = total.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = appText(en = "Total", pt = "Total"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        ChartLegendRow(color = todoColor, label = appText(en = "To do", pt = "Por fazer"), value = "${stats.todo} • ${percent(stats.todo, total)}%")
        ChartLegendRow(color = activeColor, label = appText(en = "Active", pt = "Ativas"), value = "${stats.active} • ${percent(stats.active, total)}%")
        ChartLegendRow(color = doneColor, label = appText(en = "Done", pt = "Feitas"), value = "${stats.done} • ${percent(stats.done, total)}%")
    }
}

@Composable
fun ChartLegendRow(color: Color, label: String, value: String) {
    Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(8.dp)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(color))
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
fun PerformanceLineChart(tasks: List<Task>, modifier: Modifier = Modifier) {
    val done = tasks.count { it.status?.uppercase() == "DONE" }
    val active = tasks.count { it.status?.uppercase() == "IN_PROGRESS" || it.status?.uppercase() == "ACTIVE" }
    val total = tasks.size.coerceAtLeast(1)
    val activeColor = strongActiveColor(); val doneColor = strongDoneColor()
    val pointsDone = listOf(0.20f, 0.28f, 0.24f, (done.toFloat() / total.toFloat()).coerceIn(0f, 1f))
    val pointsActive = listOf(0.45f, 0.52f, 0.48f, (active.toFloat() / total.toFloat()).coerceIn(0f, 1f))
    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            val pL = 50.dp.toPx(); val pR = 20.dp.toPx(); val pT = 18.dp.toPx(); val pB = 36.dp.toPx()
            val cW = size.width - pL - pR; val cH = size.height - pT - pB
            val gridColor = Color.LightGray.copy(alpha = 0.45f); val axisColor = Color.Gray
            repeat(5) { i -> val y = pT + cH * i / 4f; drawLine(color = gridColor, start = androidx.compose.ui.geometry.Offset(pL, y), end = androidx.compose.ui.geometry.Offset(size.width - pR, y), strokeWidth = 1.dp.toPx()) }
            drawLine(color = axisColor, start = androidx.compose.ui.geometry.Offset(pL, pT), end = androidx.compose.ui.geometry.Offset(pL, pT + cH), strokeWidth = 1.5.dp.toPx())
            drawLine(color = axisColor, start = androidx.compose.ui.geometry.Offset(pL, pT + cH), end = androidx.compose.ui.geometry.Offset(size.width - pR, pT + cH), strokeWidth = 1.5.dp.toPx())
            fun drawSeries(values: List<Float>, color: Color) {
                val step = cW / (values.size - 1)
                values.zipWithNext().forEachIndexed { index, pair ->
                    drawLine(color = color, start = androidx.compose.ui.geometry.Offset(pL + step * index, pT + cH * (1f - pair.first)), end = androidx.compose.ui.geometry.Offset(pL + step * (index + 1), pT + cH * (1f - pair.second)), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                }
            }
            drawSeries(pointsActive, activeColor); drawSeries(pointsDone, doneColor)
            drawContext.canvas.nativeCanvas.apply {
                val tp = android.graphics.Paint().apply { color = android.graphics.Color.DKGRAY; textSize = 28f; textAlign = android.graphics.Paint.Align.RIGHT }
                listOf("80", "60", "40", "20", "0").forEachIndexed { i, label -> drawText(label, pL - 10f, pT + cH * i / 4f + 8f, tp) }
                tp.textAlign = android.graphics.Paint.Align.CENTER
                listOf("Jan", "Feb", "Mar", "Apr").forEachIndexed { i, month -> drawText(month, pL + cW * i / 3f, pT + cH + 28f, tp) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ForgeMiniChip(text = appText(en = "Active tasks", pt = "Tarefas ativas"), containerColor = activeColor, contentColor = Color.White)
            ForgeMiniChip(text = appText(en = "Completed tasks", pt = "Tarefas concluídas"), containerColor = doneColor, contentColor = Color.White)
        }
    }
}

// ─────────────────────────────────────────────────────────
// Funções privadas utilitárias
// ─────────────────────────────────────────────────────────

private fun calculateReportTaskStats(tasks: List<Task>): ReportTaskStats {
    val done = tasks.count { it.status?.uppercase() == "DONE" }
    val active = tasks.count { it.status?.uppercase() == "IN_PROGRESS" || it.status?.uppercase() == "ACTIVE" }
    val todo = tasks.size - done - active
    return ReportTaskStats(total = tasks.size, todo = todo.coerceAtLeast(0), active = active, done = done)
}

private fun percent(value: Int, total: Int): Int {
    if (total <= 0) return 0
    return ((value.toFloat() / total.toFloat()) * 100f).roundToInt()
}

private fun projectNameById(projects: List<Project>, projectId: Long): String =
    projects.firstOrNull { it.id == projectId }?.name ?: "Project"

private fun formatStatus(status: String?): String = when (status?.uppercase()) {
    "DONE" -> "Done"; "IN_PROGRESS" -> "Active"; "ACTIVE" -> "Active"; "PENDING" -> "To do"; "TODO" -> "To do"; else -> status ?: "To do"
}

private fun formatPriority(priority: String?): String = when (priority?.uppercase()) {
    "HIGH" -> "High"; "MEDIUM" -> "Medium"; "LOW" -> "Low"; else -> priority ?: "-"
}

private fun formatRole(role: String?): String = when (role?.uppercase()) {
    "ADMIN" -> "Admin"; "PROJECT_MANAGER" -> "Manager"; "MANAGER" -> "Manager"; "USER" -> "Worker"; else -> role ?: "User"
}

@Composable
private fun statusChipColor(status: String?): Color = when (status?.uppercase()) {
    "DONE" -> strongDoneColor(); "IN_PROGRESS", "ACTIVE" -> strongActiveColor(); else -> strongTodoColor()
}

@Composable private fun strongTodoColor(): Color = Color(0xFF94A3B8)
@Composable private fun strongActiveColor(): Color = Color(0xFF1D4ED8)
@Composable private fun strongDoneColor(): Color = Color(0xFF16A34A)

private fun String.safeFileName(): String =
    lowercase().replace(" ", "_").replace(Regex("[^a-z0-9_\\-]"), "").ifBlank { "report" }

private fun resolveReportTasks(exportType: String, selectedProject: Project?, selectedUser: User?, selectedTask: Task?, projectTasks: Map<Long, List<Task>>, selectedUserTaskIds: List<Long>): List<Task> {
    val allTasks = projectTasks.values.flatten()
    return when (exportType) {
        "USER" -> allTasks.filter { selectedUserTaskIds.contains(it.id) }
        "TASK" -> selectedTask?.let { listOf(it) } ?: emptyList()
        else -> selectedProject?.let { projectTasks[it.id] ?: emptyList() } ?: emptyList()
    }
}

private fun writeReportCsv(context: Context, uri: Uri, exportType: String, selectedProject: Project?, selectedUser: User?, selectedTask: Task?, projects: List<Project>, projectTasks: Map<Long, List<Task>>, projectUsers: Map<Long, List<ProjectUser>>, selectedUserTaskIds: List<Long>, taskLogs: Map<Long, List<TaskLog>>): Boolean {
    return try {
        val tasks = resolveReportTasks(exportType, selectedProject, selectedUser, selectedTask, projectTasks, selectedUserTaskIds)
        val stats = calculateReportTaskStats(tasks)
        val totalHours = when (exportType) {
            "USER" -> selectedUser?.let { realHoursForUser(it.id, tasks.map { t -> t.id }, taskLogs) } ?: 0
            else -> realHoursForTasks(tasks.map { it.id }, taskLogs)
        }
        context.contentResolver.openOutputStream(uri)?.use { output ->
            OutputStreamWriter(output).use { writer ->
                writer.appendLine("ForgePlan Report")
                writer.appendLine("Type,${csv(exportType)}")
                writer.appendLine("Generated,${csv(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()))}")
                when (exportType) {
                    "USER" -> { writer.appendLine("User,${csv(selectedUser?.name ?: "-")}"); writer.appendLine("Role,${csv(formatRole(selectedUser?.role))}") }
                    "TASK" -> { writer.appendLine("Task,${csv(selectedTask?.title ?: "-")}"); writer.appendLine("Project,${csv(selectedTask?.let { projectNameById(projects, it.project_id) } ?: "-")}") }
                    else -> { writer.appendLine("Project,${csv(selectedProject?.name ?: "-")}"); writer.appendLine("Team members,${selectedProject?.let { projectUsers[it.id]?.size ?: 0 } ?: 0}") }
                }
                writer.appendLine()
                writer.appendLine("Total tasks,Completed,Active,To do,Real hours")
                writer.appendLine("${stats.total},${stats.done},${stats.active},${stats.todo},${totalHours}h")
                writer.appendLine()
                writer.appendLine("Task,Project,Status,Progress,Priority,Group,Start date,End date,Real hours,Description")
                tasks.forEach { task ->
                    val taskHours = realHoursForTasks(listOf(task.id), taskLogs)
                    writer.appendLine(listOf(task.title, projectNameById(projects, task.project_id), formatStatus(task.status), "${task.completion_rate ?: 0}%", formatPriority(task.priority), task.task_group ?: "General", task.start_date ?: "-", task.end_date ?: "-", "${taskHours}h", task.description ?: "").joinToString(",") { csv(it) })
                }
            }
        }
        true
    } catch (_: Exception) { false }
}

private fun csv(value: String): String = "\"${value.replace("\"", "\"\"")}\""

private fun writeReportPdf(context: Context, uri: Uri, exportType: String, selectedProject: Project?, selectedUser: User?, selectedTask: Task?, projects: List<Project>, projectTasks: Map<Long, List<Task>>, projectUsers: Map<Long, List<ProjectUser>>, selectedUserTaskIds: List<Long>, taskLogs: Map<Long, List<TaskLog>>): Boolean {
    return try {
        val tasks = resolveReportTasks(exportType, selectedProject, selectedUser, selectedTask, projectTasks, selectedUserTaskIds)
        val title: String; val subtitle: String; val extraLine: String
        when (exportType) {
            "USER" -> { title = "ForgePlan - User Report"; subtitle = selectedUser?.let { "${it.name} • ${formatRole(it.role)}" } ?: "User"; extraLine = "Projects: ${tasks.map { it.project_id }.distinct().size}" }
            "TASK" -> { title = "ForgePlan - Task Report"; subtitle = selectedTask?.title ?: "Task"; extraLine = "Project: ${selectedTask?.let { projectNameById(projects, it.project_id) } ?: "-"}" }
            else -> { title = "ForgePlan - Project Report"; subtitle = selectedProject?.name ?: "Project"; extraLine = "Team members: ${selectedProject?.let { projectUsers[it.id]?.size ?: 0 } ?: 0}" }
        }
        val stats = calculateReportTaskStats(tasks)
        val totalHours = when (exportType) {
            "USER" -> selectedUser?.let { realHoursForUser(it.id, tasks.map { t -> t.id }, taskLogs) } ?: 0
            else -> realHoursForTasks(tasks.map { it.id }, taskLogs)
        }
        val pdf = PdfDocument(); val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create(); val page = pdf.startPage(pageInfo); val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val navy = AndroidColor.rgb(28, 30, 89); val peach = AndroidColor.rgb(242, 198, 148); val green = AndroidColor.rgb(22, 163, 74); val blue = AndroidColor.rgb(29, 78, 216); val grey = AndroidColor.rgb(148, 163, 184); val light = AndroidColor.rgb(245, 247, 252); val border = AndroidColor.rgb(220, 226, 238)
        paint.style = Paint.Style.FILL; paint.color = navy; canvas.drawRect(0f, 0f, 595f, 78f, paint)
        paint.color = AndroidColor.WHITE; paint.textSize = 22f; paint.isFakeBoldText = true; canvas.drawText(title, 42f, 38f, paint)
        paint.textSize = 12f; paint.isFakeBoldText = false; canvas.drawText("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}", 42f, 58f, paint)
        paint.color = AndroidColor.BLACK; paint.textSize = 18f; paint.isFakeBoldText = true; canvas.drawText(subtitle, 42f, 115f, paint)
        paint.textSize = 12f; paint.isFakeBoldText = false; paint.color = AndroidColor.rgb(75, 85, 99); canvas.drawText(extraLine, 42f, 135f, paint)
        drawPdfCard(canvas, paint, 42f, 160f, 511f, 95f, light, border)
        drawPdfMetric(canvas, paint, "Total Tasks", stats.total.toString(), 75f, 205f, navy)
        drawPdfMetric(canvas, paint, "Completed", stats.done.toString(), 205f, 205f, green)
        drawPdfMetric(canvas, paint, "Active", stats.active.toString(), 335f, 205f, blue)
        drawPdfMetric(canvas, paint, "Real Hours", "${totalHours}h", 465f, 205f, grey)
        paint.color = peach; canvas.drawRect(0f, 800f, 595f, 842f, paint)
        paint.color = navy; paint.textSize = 11f; paint.isFakeBoldText = true; canvas.drawText("ForgePlan", 42f, 824f, paint)
        pdf.finishPage(page)
        context.contentResolver.openOutputStream(uri)?.use { output -> pdf.writeTo(output) }
        pdf.close(); true
    } catch (_: Exception) { false }
}

private fun drawPdfCard(canvas: android.graphics.Canvas, paint: Paint, left: Float, top: Float, width: Float, height: Float, fillColor: Int, borderColor: Int) {
    paint.style = Paint.Style.FILL; paint.color = fillColor; canvas.drawRoundRect(left, top, left + width, top + height, 18f, 18f, paint)
    paint.style = Paint.Style.STROKE; paint.strokeWidth = 1f; paint.color = borderColor; canvas.drawRoundRect(left, top, left + width, top + height, 18f, 18f, paint)
    paint.style = Paint.Style.FILL
}

private fun drawPdfMetric(canvas: android.graphics.Canvas, paint: Paint, label: String, value: String, x: Float, y: Float, color: Int) {
    paint.color = color; paint.textSize = 22f; paint.isFakeBoldText = true; canvas.drawText(value, x, y, paint)
    paint.color = AndroidColor.rgb(75, 85, 99); paint.textSize = 10f; paint.isFakeBoldText = false; canvas.drawText(label, x - 14f, y + 18f, paint)
}