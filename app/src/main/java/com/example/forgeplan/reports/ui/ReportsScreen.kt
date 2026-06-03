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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.model.ProjectUser
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.model.User
import com.example.forgeplan.core.repository.ProjectUserRepository
import com.example.forgeplan.core.repository.TaskAssignmentRepository
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

data class ReportTaskStats(
    val total: Int,
    val todo: Int,
    val active: Int,
    val done: Int
)

data class PendingPdfExport(
    val type: String,
    val fileName: String
)

data class PendingCsvExport(
    val type: String,
    val fileName: String
)

@Composable
fun ReportsScreen(
    onProjectsClick: () -> Unit = {},
    onTimelineClick: () -> Unit = {},
    onTeamClick: () -> Unit = {},
    projectViewModel: ProjectViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val projects by projectViewModel.projects.collectAsState()
    val users by userViewModel.users.collectAsState()
    val projectError by projectViewModel.error.collectAsState()
    val userError by userViewModel.error.collectAsState()

    val taskRepository = remember { TaskRepository() }
    val projectUserRepository = remember { ProjectUserRepository() }
    val taskAssignmentRepository = remember { TaskAssignmentRepository() }

    val projectTasks = remember { mutableStateMapOf<Long, List<Task>>() }
    val projectUsers = remember { mutableStateMapOf<Long, List<ProjectUser>>() }
    val selectedUserTaskIds = remember { mutableStateListOf<Long>() }

    var selectedType by remember { mutableStateOf("PROJECT") }
    var selectedProject by remember { mutableStateOf<Project?>(null) }
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }

    var pendingPdfExport by remember { mutableStateOf<PendingPdfExport?>(null) }
    var pendingCsvExport by remember { mutableStateOf<PendingCsvExport?>(null) }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val export = pendingPdfExport
        if (uri != null && export != null) {
            writeReportPdf(
                context = context,
                uri = uri,
                exportType = export.type,
                selectedProject = selectedProject,
                selectedUser = selectedUser,
                selectedTask = selectedTask,
                projects = projects,
                projectTasks = projectTasks,
                projectUsers = projectUsers,
                selectedUserTaskIds = selectedUserTaskIds
            )
        }

        pendingPdfExport = null
    }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val export = pendingCsvExport
        if (uri != null && export != null) {
            writeReportCsv(
                context = context,
                uri = uri,
                exportType = export.type,
                selectedProject = selectedProject,
                selectedUser = selectedUser,
                selectedTask = selectedTask,
                projects = projects,
                projectTasks = projectTasks,
                projectUsers = projectUsers,
                selectedUserTaskIds = selectedUserTaskIds
            )
        }

        pendingCsvExport = null
    }

    LaunchedEffect(Unit) {
        projectViewModel.loadProjects()
        userViewModel.loadUsers()
    }

    LaunchedEffect(projects) {
        if (projects.isNotEmpty() && selectedProject == null) {
            selectedProject = projects.first()
        }

        projects.forEach { project ->
            taskRepository.getTasksByProjectId(
                projectId = project.id,
                onSuccess = { tasks -> projectTasks[project.id] = tasks },
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
        if (users.isNotEmpty() && selectedUser == null) {
            selectedUser = users.first()
        }
    }

    val allTasks = projectTasks.values.flatten()

    LaunchedEffect(allTasks.size) {
        if (allTasks.isNotEmpty() && selectedTask == null) {
            selectedTask = allTasks.first()
        }
    }

    LaunchedEffect(selectedUser?.id) {
        selectedUserTaskIds.clear()

        selectedUser?.let { user ->
            taskAssignmentRepository.getTaskIdsByUserId(
                userId = user.id,
                onSuccess = { ids ->
                    selectedUserTaskIds.clear()
                    selectedUserTaskIds.addAll(ids)
                },
                onError = {
                    selectedUserTaskIds.clear()
                }
            )
        }
    }

    val selectedProjectTasks =
        selectedProject?.let { projectTasks[it.id] ?: emptyList() } ?: emptyList()

    val selectedUserTasks =
        allTasks.filter { task -> selectedUserTaskIds.contains(task.id) }

    val selectedTaskList =
        selectedTask?.let { listOf(it) } ?: emptyList()

    val displayedStats =
        when (selectedType) {
            "USER" -> calculateReportTaskStats(selectedUserTasks)
            "TASK" -> calculateReportTaskStats(selectedTaskList)
            else -> calculateReportTaskStats(selectedProjectTasks)
        }

    val selectedProjectTeamCount =
        selectedProject?.let { projectUsers[it.id]?.size ?: 0 } ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ForgePlanTopBar(
            title = appText(en = "Reports", pt = "Relatórios"),
            initials = "FP"
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (isLandscape) 42.dp else 18.dp,
                    vertical = if (isLandscape) 20.dp else 18.dp
                )
                .padding(bottom = 96.dp)
        ) {
            Text(
                text = appText(en = "Reports & Statistics", pt = "Relatórios e Estatísticas"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = appText(
                    en = "Export data by project, user, or task",
                    pt = "Exporta dados por projeto, utilizador ou tarefa"
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
            )

            Spacer(modifier = Modifier.height(18.dp))

            ReportsTypeSelector(
                selectedType = selectedType,
                onTypeSelected = {
                    selectedType = it
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedType) {
                "USER" -> ReportsDropdownCard(
                    title = appText(en = "Select user", pt = "Selecionar utilizador"),
                    selectedText = selectedUser?.let { "${it.name} - ${formatRole(it.role)}" }
                        ?: appText(en = "No user selected", pt = "Nenhum utilizador selecionado"),
                    items = users,
                    itemText = { "${it.name} - ${formatRole(it.role)}" },
                    onItemSelected = {
                        selectedUser = it
                    }
                )

                "TASK" -> ReportsDropdownCard(
                    title = appText(en = "Select task", pt = "Selecionar tarefa"),
                    selectedText = selectedTask?.let { task ->
                        "${task.title} (${projectNameById(projects, task.project_id)})"
                    } ?: appText(en = "No task selected", pt = "Nenhuma tarefa selecionada"),
                    items = allTasks,
                    itemText = { task ->
                        "${task.title} (${projectNameById(projects, task.project_id)})"
                    },
                    onItemSelected = {
                        selectedTask = it
                    }
                )

                else -> ReportsDropdownCard(
                    title = appText(en = "Select project", pt = "Selecionar projeto"),
                    selectedText = selectedProject?.name
                        ?: appText(en = "No project selected", pt = "Nenhum projeto selecionado"),
                    items = projects,
                    itemText = { it.name },
                    onItemSelected = {
                        selectedProject = it
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            projectError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(10.dp))
            }

            userError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(10.dp))
            }

            when (selectedType) {
                "USER" -> UserReportContent(
                    selectedUser = selectedUser,
                    userTasks = selectedUserTasks,
                    projects = projects,
                    projectCount = selectedUserTasks.map { it.project_id }.distinct().size,
                    stats = displayedStats,
                    isLandscape = isLandscape,
                    onExportPdf = {
                        val name = selectedUser?.name?.safeFileName() ?: "user"
                        pendingPdfExport = PendingPdfExport(
                            type = "USER",
                            fileName = "forgeplan_user_report_$name.pdf"
                        )
                        pdfLauncher.launch(pendingPdfExport!!.fileName)
                    },
                    onExportCsv = {
                        val name = selectedUser?.name?.safeFileName() ?: "user"
                        pendingCsvExport = PendingCsvExport(
                            type = "USER",
                            fileName = "forgeplan_user_report_$name.csv"
                        )
                        csvLauncher.launch(pendingCsvExport!!.fileName)
                    }
                )

                "TASK" -> TaskReportContent(
                    selectedTask = selectedTask,
                    projectName = selectedTask?.let { projectNameById(projects, it.project_id) }.orEmpty(),
                    onExportPdf = {
                        val name = selectedTask?.title?.safeFileName() ?: "task"
                        pendingPdfExport = PendingPdfExport(
                            type = "TASK",
                            fileName = "forgeplan_task_report_$name.pdf"
                        )
                        pdfLauncher.launch(pendingPdfExport!!.fileName)
                    },
                    onExportCsv = {
                        val name = selectedTask?.title?.safeFileName() ?: "task"
                        pendingCsvExport = PendingCsvExport(
                            type = "TASK",
                            fileName = "forgeplan_task_report_$name.csv"
                        )
                        csvLauncher.launch(pendingCsvExport!!.fileName)
                    }
                )

                else -> ProjectReportContent(
                    selectedProject = selectedProject,
                    tasks = selectedProjectTasks,
                    teamCount = selectedProjectTeamCount,
                    stats = displayedStats,
                    isLandscape = isLandscape,
                    onExportPdf = {
                        val name = selectedProject?.name?.safeFileName() ?: "project"
                        pendingPdfExport = PendingPdfExport(
                            type = "PROJECT",
                            fileName = "forgeplan_project_report_$name.pdf"
                        )
                        pdfLauncher.launch(pendingPdfExport!!.fileName)
                    },
                    onExportCsv = {
                        val name = selectedProject?.name?.safeFileName() ?: "project"
                        pendingCsvExport = PendingCsvExport(
                            type = "PROJECT",
                            fileName = "forgeplan_project_report_$name.csv"
                        )
                        csvLauncher.launch(pendingCsvExport!!.fileName)
                    }
                )
            }
        }

        ForgePlanBottomBar(
            selectedItem = "Reports",
            onProjectsClick = onProjectsClick,
            onTimelineClick = onTimelineClick,
            onProgressClick = {},
            onTeamClick = onTeamClick
        )
    }
}

@Composable
fun ReportsTypeSelector(
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = appText(en = "Select report type", pt = "Selecionar tipo de relatório"),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ReportTypeButton(
                    text = appText(en = "By Project", pt = "Por Projeto"),
                    selected = selectedType == "PROJECT",
                    onClick = { onTypeSelected("PROJECT") },
                    modifier = Modifier.weight(1f)
                )

                ReportTypeButton(
                    text = appText(en = "By User", pt = "Por Utilizador"),
                    selected = selectedType == "USER",
                    onClick = { onTypeSelected("USER") },
                    modifier = Modifier.weight(1f)
                )

                ReportTypeButton(
                    text = appText(en = "By Task", pt = "Por Tarefa"),
                    selected = selectedType == "TASK",
                    onClick = { onTypeSelected("TASK") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ReportTypeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        modifier = modifier.height(44.dp),
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.background
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onBackground
            }
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun <T> ReportsDropdownCard(
    title: String,
    selectedText: String,
    items: List<T>,
    itemText: (T) -> String,
    onItemSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            Box {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { expanded = true },
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )

                        Text(
                            text = "⌄",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    items.forEach { item ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = itemText(item),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            onClick = {
                                onItemSelected(item)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectReportContent(
    selectedProject: Project?,
    tasks: List<Task>,
    teamCount: Int,
    stats: ReportTaskStats,
    isLandscape: Boolean,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit
) {
    val totalHours = estimateHours(tasks)

    ReportMetricsCard(
        title = selectedProject?.let { "Projeto: ${it.name}" }
            ?: appText(en = "Project", pt = "Projeto"),
        metrics = listOf(
            appText(en = "Total Tasks", pt = "Total de Tarefas") to stats.total.toString(),
            appText(en = "Completed", pt = "Concluídas") to stats.done.toString(),
            appText(en = "Total Hours", pt = "Horas Totais") to "${totalHours}h",
            appText(en = "Team Members", pt = "Membros") to teamCount.toString()
        )
    )

    Spacer(modifier = Modifier.height(16.dp))

    ReportSectionCard(
        title = appText(en = "Task Status Distribution", pt = "Distribuição do Estado das Tarefas")
    ) {
        TaskStatusDonutChart(
            stats = stats,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isLandscape) 320.dp else 400.dp)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    ReportSectionCard(
        title = appText(en = "Project Tasks", pt = "Tarefas do Projeto")
    ) {
        if (tasks.isEmpty()) {
            Text(
                text = appText(en = "No tasks found.", pt = "Não existem tarefas."),
                color = MaterialTheme.colorScheme.onSurface
            )
        } else {
            tasks.forEach { task ->
                TaskReportRow(task = task)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    ExportReportCard(
        title = appText(en = "Export Project Report", pt = "Exportar Relatório do Projeto"),
        onExportPdf = onExportPdf,
        onExportCsv = onExportCsv
    )
}

@Composable
fun UserReportContent(
    selectedUser: User?,
    userTasks: List<Task>,
    projects: List<Project>,
    projectCount: Int,
    stats: ReportTaskStats,
    isLandscape: Boolean,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit
) {
    val totalHours = estimateHours(userTasks)

    ReportMetricsCard(
        title = selectedUser?.let { "Utilizador: ${it.name}" }
            ?: appText(en = "User", pt = "Utilizador"),
        subtitle = selectedUser?.let { "Role: ${formatRole(it.role)}" },
        metrics = listOf(
            appText(en = "Total Tasks", pt = "Total de Tarefas") to stats.total.toString(),
            appText(en = "Completed", pt = "Concluídas") to stats.done.toString(),
            appText(en = "Total Hours", pt = "Horas Totais") to "${totalHours}h",
            appText(en = "Projects", pt = "Projetos") to projectCount.toString()
        )
    )

    Spacer(modifier = Modifier.height(16.dp))

    ReportSectionCard(
        title = appText(en = "Tasks & Hours by Project", pt = "Tarefas e Horas por Projeto")
    ) {
        UserProjectsDesktopTable(
            tasks = userTasks,
            projects = projects
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    ReportSectionCard(
        title = appText(en = "Performance Trend", pt = "Tendência de Performance")
    ) {
        PerformanceLineChart(
            tasks = userTasks,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isLandscape) 250.dp else 280.dp)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    ExportReportCard(
        title = appText(en = "Export User Report", pt = "Exportar Relatório do Utilizador"),
        onExportPdf = onExportPdf,
        onExportCsv = onExportCsv
    )
}

@Composable
fun UserProjectsDesktopTable(
    tasks: List<Task>,
    projects: List<Project>
) {
    val grouped = tasks.groupBy { it.project_id }

    if (grouped.isEmpty()) {
        Text(
            text = appText(
                en = "No tasks assigned to this user.",
                pt = "Não existem tarefas atribuídas a este utilizador."
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        return
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            TableHeader(
                text = appText(en = "Project", pt = "Projeto"),
                modifier = Modifier.weight(2f)
            )

            TableHeader(
                text = appText(en = "Tasks", pt = "Tarefas"),
                modifier = Modifier.weight(1f)
            )

            TableHeader(
                text = appText(en = "Hours", pt = "Horas"),
                modifier = Modifier.weight(1f)
            )

            TableHeader(
                text = appText(en = "Avg.", pt = "Média"),
                modifier = Modifier.weight(1f)
            )
        }

        grouped.forEach { entry ->
            val projectName = projectNameById(projects, entry.key)
            val taskCount = entry.value.size
            val hours = estimateHours(entry.value)
            val average =
                if (taskCount == 0) 0f
                else hours.toFloat() / taskCount.toFloat()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                TableCell(
                    text = projectName,
                    modifier = Modifier.weight(2f)
                )

                TableCell(
                    text = taskCount.toString(),
                    modifier = Modifier.weight(1f)
                )

                TableCell(
                    text = "${hours}h",
                    modifier = Modifier.weight(1f)
                )

                TableCell(
                    text = String.format(Locale.US, "%.1fh", average),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun TableHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
        maxLines = 1
    )
}

@Composable
fun TableCell(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1
    )
}

@Composable
fun TaskReportContent(
    selectedTask: Task?,
    projectName: String,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit
) {
    val task = selectedTask

    if (task == null) {
        ReportSectionCard(
            title = appText(en = "Task details", pt = "Detalhes da tarefa")
        ) {
            Text(
                text = appText(en = "No task selected.", pt = "Nenhuma tarefa selecionada."),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        return
    }

    ReportMetricsCard(
        title = task.title,
        subtitle = appText(en = "Project: $projectName", pt = "Projeto: $projectName"),
        metrics = listOf(
            appText(en = "Status", pt = "Estado") to formatStatus(task.status),
            appText(en = "Progress", pt = "Progresso") to "${task.completion_rate ?: 0}%",
            appText(en = "Priority", pt = "Prioridade") to formatPriority(task.priority),
            appText(en = "Group", pt = "Grupo") to (task.task_group ?: "General")
        )
    )

    Spacer(modifier = Modifier.height(16.dp))

    ReportSectionCard(
        title = appText(en = "Task details", pt = "Detalhes da tarefa")
    ) {
        Text(
            text = appText(en = "Description", pt = "Descrição"),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = task.description ?: appText(en = "No description.", pt = "Sem descrição."),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )

        Spacer(modifier = Modifier.height(14.dp))

        LinearProgressIndicator(
            progress = { ((task.completion_rate ?: 0).coerceIn(0, 100)) / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50)),
            color = if ((task.completion_rate ?: 0) >= 100 || task.status?.uppercase() == "DONE") {
                strongDoneColor()
            } else {
                strongActiveColor()
            },
            trackColor = MaterialTheme.colorScheme.secondaryContainer
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ForgeMiniChip(
                text = appText(
                    en = "Start: ${task.start_date ?: "-"}",
                    pt = "Início: ${task.start_date ?: "-"}"
                )
            )

            ForgeMiniChip(
                text = appText(
                    en = "End: ${task.end_date ?: "-"}",
                    pt = "Fim: ${task.end_date ?: "-"}"
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    ExportReportCard(
        title = appText(en = "Export Task Report", pt = "Exportar Relatório da Tarefa"),
        onExportPdf = onExportPdf,
        onExportCsv = onExportCsv
    )
}

@Composable
fun ReportMetricsCard(
    title: String,
    subtitle: String? = null,
    metrics: List<Pair<String, String>>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            subtitle?.let {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                metrics.forEach { metric ->
                    MetricColumn(
                        metric = metric,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricColumn(
    metric: Pair<String, String>,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = metric.second,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )

        Text(
            text = metric.first,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            maxLines = 1
        )
    }
}

@Composable
fun ReportSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            content()
        }
    }
}

@Composable
fun TaskStatusDonutChart(
    stats: ReportTaskStats,
    modifier: Modifier = Modifier
) {
    val total = stats.total.coerceAtLeast(0)
    val todoColor = strongTodoColor()
    val activeColor = strongActiveColor()
    val doneColor = strongDoneColor()
    val emptyColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(210.dp)) {
                val stroke = Stroke(
                    width = 26.dp.toPx(),
                    cap = StrokeCap.Butt
                )

                if (total == 0) {
                    drawArc(
                        color = emptyColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = stroke,
                        size = Size(size.width, size.height)
                    )
                } else {
                    var startAngle = -90f

                    val todoSweep = 360f * stats.todo / total
                    val activeSweep = 360f * stats.active / total
                    val doneSweep = 360f * stats.done / total

                    if (stats.todo > 0) {
                        drawArc(
                            color = todoColor,
                            startAngle = startAngle,
                            sweepAngle = todoSweep,
                            useCenter = false,
                            style = stroke,
                            size = Size(size.width, size.height)
                        )
                        startAngle += todoSweep
                    }

                    if (stats.active > 0) {
                        drawArc(
                            color = activeColor,
                            startAngle = startAngle,
                            sweepAngle = activeSweep,
                            useCenter = false,
                            style = stroke,
                            size = Size(size.width, size.height)
                        )
                        startAngle += activeSweep
                    }

                    if (stats.done > 0) {
                        drawArc(
                            color = doneColor,
                            startAngle = startAngle,
                            sweepAngle = doneSweep,
                            useCenter = false,
                            style = stroke,
                            size = Size(size.width, size.height)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = total.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = appText(en = "Total", pt = "Total"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        ChartLegendRow(
            color = todoColor,
            label = appText(en = "To do", pt = "Por fazer"),
            value = "${stats.todo} • ${percent(stats.todo, total)}%"
        )

        ChartLegendRow(
            color = activeColor,
            label = appText(en = "Active", pt = "Ativas"),
            value = "${stats.active} • ${percent(stats.active, total)}%"
        )

        ChartLegendRow(
            color = doneColor,
            label = appText(en = "Done", pt = "Feitas"),
            value = "${stats.done} • ${percent(stats.done, total)}%"
        )
    }
}

@Composable
fun ChartLegendRow(
    color: Color,
    label: String,
    value: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun PerformanceLineChart(
    tasks: List<Task>,
    modifier: Modifier = Modifier
) {
    val done = tasks.count { it.status?.uppercase() == "DONE" }
    val active = tasks.count {
        it.status?.uppercase() == "IN_PROGRESS" || it.status?.uppercase() == "ACTIVE"
    }

    val total = tasks.size.coerceAtLeast(1)

    val activeColor = strongActiveColor()
    val doneColor = strongDoneColor()

    val pointsDone = listOf(
        0.20f,
        0.28f,
        0.24f,
        (done.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    )

    val pointsActive = listOf(
        0.45f,
        0.52f,
        0.48f,
        (active.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    )

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            val paddingLeft = 50.dp.toPx()
            val paddingRight = 20.dp.toPx()
            val paddingTop = 18.dp.toPx()
            val paddingBottom = 36.dp.toPx()

            val chartWidth = size.width - paddingLeft - paddingRight
            val chartHeight = size.height - paddingTop - paddingBottom

            val gridColor = Color.LightGray.copy(alpha = 0.45f)
            val axisColor = Color.Gray

            repeat(5) { index ->
                val y = paddingTop + chartHeight * index / 4f

                drawLine(
                    color = gridColor,
                    start = androidx.compose.ui.geometry.Offset(paddingLeft, y),
                    end = androidx.compose.ui.geometry.Offset(size.width - paddingRight, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            drawLine(
                color = axisColor,
                start = androidx.compose.ui.geometry.Offset(paddingLeft, paddingTop),
                end = androidx.compose.ui.geometry.Offset(paddingLeft, paddingTop + chartHeight),
                strokeWidth = 1.5.dp.toPx()
            )

            drawLine(
                color = axisColor,
                start = androidx.compose.ui.geometry.Offset(paddingLeft, paddingTop + chartHeight),
                end = androidx.compose.ui.geometry.Offset(size.width - paddingRight, paddingTop + chartHeight),
                strokeWidth = 1.5.dp.toPx()
            )

            fun drawSeries(values: List<Float>, color: Color) {
                val step = chartWidth / (values.size - 1)

                values.zipWithNext().forEachIndexed { index, pair ->
                    val x1 = paddingLeft + step * index
                    val y1 = paddingTop + chartHeight * (1f - pair.first)
                    val x2 = paddingLeft + step * (index + 1)
                    val y2 = paddingTop + chartHeight * (1f - pair.second)

                    drawLine(
                        color = color,
                        start = androidx.compose.ui.geometry.Offset(x1, y1),
                        end = androidx.compose.ui.geometry.Offset(x2, y2),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            drawSeries(pointsActive, activeColor)
            drawSeries(pointsDone, doneColor)

            drawContext.canvas.nativeCanvas.apply {
                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.DKGRAY
                    textSize = 28f
                    textAlign = android.graphics.Paint.Align.RIGHT
                }

                val labelsY = listOf("80", "60", "40", "20", "0")

                labelsY.forEachIndexed { index, label ->
                    val y = paddingTop + chartHeight * index / 4f + 8f
                    drawText(label, paddingLeft - 10f, y, textPaint)
                }

                textPaint.textAlign = android.graphics.Paint.Align.CENTER

                val months = listOf("Jan", "Feb", "Mar", "Apr")

                months.forEachIndexed { index, month ->
                    val x = paddingLeft + chartWidth * index / 3f
                    drawText(month, x, paddingTop + chartHeight + 28f, textPaint)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ForgeMiniChip(
                text = appText(en = "Active tasks", pt = "Tarefas ativas"),
                containerColor = activeColor,
                contentColor = Color.White
            )

            ForgeMiniChip(
                text = appText(en = "Completed tasks", pt = "Tarefas concluídas"),
                containerColor = doneColor,
                contentColor = Color.White
            )
        }
    }
}

@Composable
fun TaskReportRow(
    task: Task
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = task.description ?: appText(en = "No description", pt = "Sem descrição"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    maxLines = 1
                )
            }

            ForgeMiniChip(
                text = formatStatus(task.status),
                containerColor = statusChipColor(task.status),
                contentColor = Color.White
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { ((task.completion_rate ?: 0).coerceIn(0, 100)) / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(50)),
            color = if ((task.completion_rate ?: 0) >= 100 || task.status?.uppercase() == "DONE") {
                strongDoneColor()
            } else {
                strongActiveColor()
            },
            trackColor = MaterialTheme.colorScheme.secondaryContainer
        )
    }
}

@Composable
fun ExportReportCard(
    title: String,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onExportPdf,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text(
                        text = appText(en = "Export PDF", pt = "Exportar PDF"),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onExportCsv,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, strongDoneColor())
                ) {
                    Text(
                        text = appText(en = "Export CSV", pt = "Exportar CSV"),
                        color = strongDoneColor()
                    )
                }
            }
        }
    }
}

private fun calculateReportTaskStats(tasks: List<Task>): ReportTaskStats {
    val done = tasks.count { it.status?.uppercase() == "DONE" }
    val active = tasks.count {
        it.status?.uppercase() == "IN_PROGRESS" || it.status?.uppercase() == "ACTIVE"
    }

    val todo = tasks.size - done - active

    return ReportTaskStats(
        total = tasks.size,
        todo = todo.coerceAtLeast(0),
        active = active,
        done = done
    )
}

private fun percent(
    value: Int,
    total: Int
): Int {
    if (total <= 0) return 0
    return ((value.toFloat() / total.toFloat()) * 100f).roundToInt()
}

private fun estimateHours(tasks: List<Task>): Int {
    return tasks.size * 3
}

private fun projectNameById(
    projects: List<Project>,
    projectId: Long
): String {
    return projects.firstOrNull { it.id == projectId }?.name ?: "Project"
}

private fun formatStatus(status: String?): String {
    return when (status?.uppercase()) {
        "DONE" -> "Done"
        "IN_PROGRESS" -> "Active"
        "ACTIVE" -> "Active"
        "PENDING" -> "To do"
        "TODO" -> "To do"
        else -> status ?: "To do"
    }
}

private fun formatPriority(priority: String?): String {
    return when (priority?.uppercase()) {
        "HIGH" -> "High"
        "MEDIUM" -> "Medium"
        "LOW" -> "Low"
        else -> priority ?: "-"
    }
}

private fun formatRole(role: String?): String {
    return when (role?.uppercase()) {
        "ADMIN" -> "Admin"
        "PROJECT_MANAGER" -> "Manager"
        "MANAGER" -> "Manager"
        "USER" -> "Worker"
        else -> role ?: "User"
    }
}

@Composable
private fun statusChipColor(status: String?): Color {
    return when (status?.uppercase()) {
        "DONE" -> strongDoneColor()
        "IN_PROGRESS", "ACTIVE" -> strongActiveColor()
        else -> strongTodoColor()
    }
}

@Composable
private fun strongTodoColor(): Color {
    return Color(0xFF94A3B8)
}

@Composable
private fun strongActiveColor(): Color {
    return Color(0xFF1D4ED8)
}

@Composable
private fun strongDoneColor(): Color {
    return Color(0xFF16A34A)
}

private fun String.safeFileName(): String {
    return lowercase()
        .replace(" ", "_")
        .replace(Regex("[^a-z0-9_\\-]"), "")
        .ifBlank { "report" }
}

private fun resolveReportTasks(
    exportType: String,
    selectedProject: Project?,
    selectedUser: User?,
    selectedTask: Task?,
    projectTasks: Map<Long, List<Task>>,
    selectedUserTaskIds: List<Long>
): List<Task> {
    val allTasks = projectTasks.values.flatten()

    return when (exportType) {
        "USER" -> allTasks.filter { selectedUserTaskIds.contains(it.id) }
        "TASK" -> selectedTask?.let { listOf(it) } ?: emptyList()
        else -> selectedProject?.let { projectTasks[it.id] ?: emptyList() } ?: emptyList()
    }
}

private fun writeReportCsv(
    context: Context,
    uri: Uri,
    exportType: String,
    selectedProject: Project?,
    selectedUser: User?,
    selectedTask: Task?,
    projects: List<Project>,
    projectTasks: Map<Long, List<Task>>,
    projectUsers: Map<Long, List<ProjectUser>>,
    selectedUserTaskIds: List<Long>
): Boolean {
    return try {
        val tasks = resolveReportTasks(
            exportType = exportType,
            selectedProject = selectedProject,
            selectedUser = selectedUser,
            selectedTask = selectedTask,
            projectTasks = projectTasks,
            selectedUserTaskIds = selectedUserTaskIds
        )

        val stats = calculateReportTaskStats(tasks)

        context.contentResolver.openOutputStream(uri)?.use { output ->
            OutputStreamWriter(output).use { writer ->
                writer.appendLine("ForgePlan Report")
                writer.appendLine("Type,${csv(exportType)}")
                writer.appendLine("Generated,${csv(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()))}")

                when (exportType) {
                    "USER" -> {
                        writer.appendLine("User,${csv(selectedUser?.name ?: "-")}")
                        writer.appendLine("Role,${csv(formatRole(selectedUser?.role))}")
                    }

                    "TASK" -> {
                        writer.appendLine("Task,${csv(selectedTask?.title ?: "-")}")
                        writer.appendLine("Project,${csv(selectedTask?.let { projectNameById(projects, it.project_id) } ?: "-")}")
                    }

                    else -> {
                        writer.appendLine("Project,${csv(selectedProject?.name ?: "-")}")
                        writer.appendLine("Team members,${selectedProject?.let { projectUsers[it.id]?.size ?: 0 } ?: 0}")
                    }
                }

                writer.appendLine()
                writer.appendLine("Total tasks,Completed,Active,To do,Estimated hours")
                writer.appendLine("${stats.total},${stats.done},${stats.active},${stats.todo},${estimateHours(tasks)}h")

                writer.appendLine()
                writer.appendLine("Task,Project,Status,Progress,Priority,Group,Start date,End date,Description")

                tasks.forEach { task ->
                    writer.appendLine(
                        listOf(
                            task.title,
                            projectNameById(projects, task.project_id),
                            formatStatus(task.status),
                            "${task.completion_rate ?: 0}%",
                            formatPriority(task.priority),
                            task.task_group ?: "General",
                            task.start_date ?: "-",
                            task.end_date ?: "-",
                            task.description ?: ""
                        ).joinToString(",") { csv(it) }
                    )
                }
            }
        }

        true
    } catch (_: Exception) {
        false
    }
}

private fun csv(value: String): String {
    val escaped = value.replace("\"", "\"\"")
    return "\"$escaped\""
}

private fun writeReportPdf(
    context: Context,
    uri: Uri,
    exportType: String,
    selectedProject: Project?,
    selectedUser: User?,
    selectedTask: Task?,
    projects: List<Project>,
    projectTasks: Map<Long, List<Task>>,
    projectUsers: Map<Long, List<ProjectUser>>,
    selectedUserTaskIds: List<Long>
): Boolean {
    return try {
        val tasks = resolveReportTasks(
            exportType = exportType,
            selectedProject = selectedProject,
            selectedUser = selectedUser,
            selectedTask = selectedTask,
            projectTasks = projectTasks,
            selectedUserTaskIds = selectedUserTaskIds
        )

        val title: String
        val subtitle: String
        val extraLine: String

        when (exportType) {
            "USER" -> {
                title = "ForgePlan - User Report"
                subtitle = selectedUser?.let { "${it.name} • ${formatRole(it.role)}" } ?: "User"
                extraLine = "Projects: ${tasks.map { it.project_id }.distinct().size}"
            }

            "TASK" -> {
                title = "ForgePlan - Task Report"
                subtitle = selectedTask?.title ?: "Task"
                extraLine = "Project: ${selectedTask?.let { projectNameById(projects, it.project_id) } ?: "-"}"
            }

            else -> {
                title = "ForgePlan - Project Report"
                subtitle = selectedProject?.name ?: "Project"
                extraLine = "Team members: ${selectedProject?.let { projectUsers[it.id]?.size ?: 0 } ?: 0}"
            }
        }

        val stats = calculateReportTaskStats(tasks)
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val navy = AndroidColor.rgb(28, 30, 89)
        val peach = AndroidColor.rgb(242, 198, 148)
        val green = AndroidColor.rgb(22, 163, 74)
        val blue = AndroidColor.rgb(29, 78, 216)
        val grey = AndroidColor.rgb(148, 163, 184)
        val light = AndroidColor.rgb(245, 247, 252)
        val border = AndroidColor.rgb(220, 226, 238)

        paint.style = Paint.Style.FILL
        paint.color = navy
        canvas.drawRect(0f, 0f, 595f, 78f, paint)

        paint.color = AndroidColor.WHITE
        paint.textSize = 22f
        paint.isFakeBoldText = true
        canvas.drawText(title, 42f, 38f, paint)

        paint.textSize = 12f
        paint.isFakeBoldText = false
        canvas.drawText(
            "Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}",
            42f,
            58f,
            paint
        )

        paint.color = AndroidColor.BLACK
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText(subtitle, 42f, 115f, paint)

        paint.textSize = 12f
        paint.isFakeBoldText = false
        paint.color = AndroidColor.rgb(75, 85, 99)
        canvas.drawText(extraLine, 42f, 135f, paint)

        drawPdfCard(canvas, paint, 42f, 160f, 511f, 95f, light, border)

        drawPdfMetric(canvas, paint, "Total Tasks", stats.total.toString(), 75f, 205f, navy)
        drawPdfMetric(canvas, paint, "Completed", stats.done.toString(), 205f, 205f, green)
        drawPdfMetric(canvas, paint, "Active", stats.active.toString(), 335f, 205f, blue)
        drawPdfMetric(canvas, paint, "To Do", stats.todo.toString(), 465f, 205f, grey)

        when {
            exportType == "TASK" && selectedTask != null -> {
                drawPdfCard(canvas, paint, 42f, 280f, 511f, 300f, AndroidColor.WHITE, border)

                paint.color = AndroidColor.BLACK
                paint.textSize = 16f
                paint.isFakeBoldText = true
                canvas.drawText("Task Details", 62f, 315f, paint)

                paint.textSize = 11f
                paint.isFakeBoldText = true
                canvas.drawText("Description", 62f, 350f, paint)

                paint.isFakeBoldText = false
                paint.color = AndroidColor.rgb(75, 85, 99)
                canvas.drawText((selectedTask.description ?: "No description.").take(80), 62f, 370f, paint)

                val progress = (selectedTask.completion_rate ?: 0).coerceIn(0, 100)

                paint.color = border
                paint.style = Paint.Style.FILL
                canvas.drawRoundRect(62f, 398f, 532f, 410f, 8f, 8f, paint)

                paint.color =
                    if (progress >= 100 || selectedTask.status?.uppercase() == "DONE") green else blue
                canvas.drawRoundRect(62f, 398f, 62f + (470f * progress / 100f), 410f, 8f, 8f, paint)

                paint.color = AndroidColor.rgb(31, 41, 55)
                paint.textSize = 10f
                paint.isFakeBoldText = true
                canvas.drawText("Status", 62f, 455f, paint)
                canvas.drawText("Progress", 175f, 455f, paint)
                canvas.drawText("Priority", 300f, 455f, paint)
                canvas.drawText("Group", 420f, 455f, paint)

                paint.isFakeBoldText = false
                canvas.drawText(formatStatus(selectedTask.status), 62f, 475f, paint)
                canvas.drawText("${selectedTask.completion_rate ?: 0}%", 175f, 475f, paint)
                canvas.drawText(formatPriority(selectedTask.priority), 300f, 475f, paint)
                canvas.drawText((selectedTask.task_group ?: "General").take(18), 420f, 475f, paint)

                paint.isFakeBoldText = true
                canvas.drawText("Start date", 62f, 525f, paint)
                canvas.drawText("End date", 205f, 525f, paint)

                paint.isFakeBoldText = false
                canvas.drawText(selectedTask.start_date ?: "-", 62f, 545f, paint)
                canvas.drawText(selectedTask.end_date ?: "-", 205f, 545f, paint)
            }

            exportType == "USER" -> {
                drawPdfCard(canvas, paint, 42f, 280f, 511f, 235f, light, border)

                paint.color = AndroidColor.BLACK
                paint.textSize = 16f
                paint.isFakeBoldText = true
                canvas.drawText("Performance Trend", 62f, 315f, paint)

                drawPdfLineChart(
                    canvas = canvas,
                    paint = paint,
                    tasks = tasks,
                    left = 78f,
                    top = 340f,
                    width = 430f,
                    height = 135f,
                    activeColor = blue,
                    doneColor = green,
                    gridColor = border
                )

                drawPdfLegend(canvas, paint, "Active tasks", stats.active, stats.total, blue, 210f, 495f)
                drawPdfLegend(canvas, paint, "Completed tasks", stats.done, stats.total, green, 330f, 495f)

                drawPdfCard(canvas, paint, 42f, 545f, 511f, 195f, AndroidColor.WHITE, border)

                paint.color = AndroidColor.BLACK
                paint.textSize = 16f
                paint.isFakeBoldText = true
                canvas.drawText("Tasks and Hours by Project", 62f, 580f, paint)

                paint.textSize = 10f
                paint.isFakeBoldText = true
                canvas.drawText("Project", 62f, 610f, paint)
                canvas.drawText("Tasks", 315f, 610f, paint)
                canvas.drawText("Hours", 405f, 610f, paint)
                canvas.drawText("Avg.", 475f, 610f, paint)

                paint.isFakeBoldText = false
                paint.color = AndroidColor.rgb(55, 65, 81)

                var y = 635f

                tasks.groupBy { it.project_id }.entries.take(5).forEach { entry ->
                    val projectName = projectNameById(projects, entry.key)
                    val taskCount = entry.value.size
                    val hours = estimateHours(entry.value)
                    val average = if (taskCount == 0) 0f else hours.toFloat() / taskCount.toFloat()

                    canvas.drawText(projectName.take(28), 62f, y, paint)
                    canvas.drawText(taskCount.toString(), 315f, y, paint)
                    canvas.drawText("${hours}h", 405f, y, paint)
                    canvas.drawText(String.format(Locale.US, "%.1fh", average), 475f, y, paint)

                    paint.color = border
                    canvas.drawLine(62f, y + 12f, 532f, y + 12f, paint)
                    paint.color = AndroidColor.rgb(55, 65, 81)

                    y += 24f
                }
            }

            else -> {
                drawPdfCard(canvas, paint, 42f, 280f, 511f, 210f, light, border)

                paint.color = AndroidColor.BLACK
                paint.textSize = 16f
                paint.isFakeBoldText = true
                canvas.drawText("Task Status Distribution", 62f, 315f, paint)

                drawPdfDonut(
                    canvas = canvas,
                    paint = paint,
                    stats = stats,
                    centerX = 298f,
                    centerY = 390f,
                    radius = 62f,
                    stroke = 24f,
                    todoColor = grey,
                    activeColor = blue,
                    doneColor = green
                )

                drawPdfLegend(canvas, paint, "To do", stats.todo, stats.total, grey, 410f, 350f)
                drawPdfLegend(canvas, paint, "Active", stats.active, stats.total, blue, 410f, 380f)
                drawPdfLegend(canvas, paint, "Done", stats.done, stats.total, green, 410f, 410f)

                drawPdfCard(canvas, paint, 42f, 520f, 511f, 220f, AndroidColor.WHITE, border)

                paint.color = AndroidColor.BLACK
                paint.textSize = 16f
                paint.isFakeBoldText = true
                canvas.drawText("Task Details", 62f, 555f, paint)

                paint.textSize = 10f
                paint.isFakeBoldText = true
                canvas.drawText("Task", 62f, 585f, paint)
                canvas.drawText("Status", 315f, 585f, paint)
                canvas.drawText("Progress", 405f, 585f, paint)
                canvas.drawText("Group", 475f, 585f, paint)

                paint.isFakeBoldText = false
                paint.color = AndroidColor.rgb(55, 65, 81)

                var y = 610f

                tasks.take(8).forEach { task ->
                    canvas.drawText(task.title.take(32), 62f, y, paint)
                    canvas.drawText(formatStatus(task.status), 315f, y, paint)
                    canvas.drawText("${task.completion_rate ?: 0}%", 405f, y, paint)
                    canvas.drawText((task.task_group ?: "General").take(16), 475f, y, paint)

                    paint.color = border
                    canvas.drawLine(62f, y + 12f, 532f, y + 12f, paint)
                    paint.color = AndroidColor.rgb(55, 65, 81)

                    y += 24f
                }
            }
        }

        paint.color = peach
        canvas.drawRect(0f, 800f, 595f, 842f, paint)

        paint.color = navy
        paint.textSize = 11f
        paint.isFakeBoldText = true
        canvas.drawText("ForgePlan", 42f, 824f, paint)

        pdf.finishPage(page)

        context.contentResolver.openOutputStream(uri)?.use { output ->
            pdf.writeTo(output)
        }

        pdf.close()

        true
    } catch (_: Exception) {
        false
    }
}

private fun drawPdfCard(
    canvas: android.graphics.Canvas,
    paint: Paint,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    fillColor: Int,
    borderColor: Int
) {
    paint.style = Paint.Style.FILL
    paint.color = fillColor
    canvas.drawRoundRect(left, top, left + width, top + height, 18f, 18f, paint)

    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1f
    paint.color = borderColor
    canvas.drawRoundRect(left, top, left + width, top + height, 18f, 18f, paint)

    paint.style = Paint.Style.FILL
}

private fun drawPdfMetric(
    canvas: android.graphics.Canvas,
    paint: Paint,
    label: String,
    value: String,
    x: Float,
    y: Float,
    color: Int
) {
    paint.color = color
    paint.textSize = 22f
    paint.isFakeBoldText = true
    canvas.drawText(value, x, y, paint)

    paint.color = AndroidColor.rgb(75, 85, 99)
    paint.textSize = 10f
    paint.isFakeBoldText = false
    canvas.drawText(label, x - 14f, y + 18f, paint)
}

private fun drawPdfDonut(
    canvas: android.graphics.Canvas,
    paint: Paint,
    stats: ReportTaskStats,
    centerX: Float,
    centerY: Float,
    radius: Float,
    stroke: Float,
    todoColor: Int,
    activeColor: Int,
    doneColor: Int
) {
    val total = stats.total

    paint.style = Paint.Style.STROKE
    paint.strokeWidth = stroke
    paint.strokeCap = Paint.Cap.BUTT

    val rect = android.graphics.RectF(
        centerX - radius,
        centerY - radius,
        centerX + radius,
        centerY + radius
    )

    if (total == 0) {
        paint.color = AndroidColor.LTGRAY
        canvas.drawArc(rect, -90f, 360f, false, paint)
    } else {
        var start = -90f

        val todoSweep = 360f * stats.todo / total
        val activeSweep = 360f * stats.active / total
        val doneSweep = 360f * stats.done / total

        if (stats.todo > 0) {
            paint.color = todoColor
            canvas.drawArc(rect, start, todoSweep, false, paint)
            start += todoSweep
        }

        if (stats.active > 0) {
            paint.color = activeColor
            canvas.drawArc(rect, start, activeSweep, false, paint)
            start += activeSweep
        }

        if (stats.done > 0) {
            paint.color = doneColor
            canvas.drawArc(rect, start, doneSweep, false, paint)
        }
    }

    paint.style = Paint.Style.FILL
    paint.color = AndroidColor.rgb(28, 30, 89)
    paint.textSize = 20f
    paint.isFakeBoldText = true
    canvas.drawText(total.toString(), centerX - 8f, centerY + 6f, paint)

    paint.textSize = 10f
    paint.isFakeBoldText = false
    canvas.drawText("Total", centerX - 13f, centerY + 23f, paint)
}

private fun drawPdfLineChart(
    canvas: android.graphics.Canvas,
    paint: Paint,
    tasks: List<Task>,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    activeColor: Int,
    doneColor: Int,
    gridColor: Int
) {
    val done = tasks.count { it.status?.uppercase() == "DONE" }
    val active = tasks.count {
        it.status?.uppercase() == "IN_PROGRESS" || it.status?.uppercase() == "ACTIVE"
    }

    val total = tasks.size.coerceAtLeast(1)

    val donePoints = listOf(
        0.20f,
        0.28f,
        0.24f,
        (done.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    )

    val activePoints = listOf(
        0.45f,
        0.52f,
        0.48f,
        (active.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    )

    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1f
    paint.color = gridColor

    repeat(5) { index ->
        val y = top + height * index / 4f
        canvas.drawLine(left, y, left + width, y, paint)
    }

    paint.color = AndroidColor.rgb(120, 130, 150)
    paint.strokeWidth = 1.5f
    canvas.drawLine(left, top, left, top + height, paint)
    canvas.drawLine(left, top + height, left + width, top + height, paint)

    fun drawSeries(values: List<Float>, color: Int) {
        paint.color = color
        paint.strokeWidth = 3f
        paint.style = Paint.Style.STROKE

        val step = width / (values.size - 1)

        values.zipWithNext().forEachIndexed { index, pair ->
            val x1 = left + step * index
            val y1 = top + height * (1f - pair.first)
            val x2 = left + step * (index + 1)
            val y2 = top + height * (1f - pair.second)

            canvas.drawLine(x1, y1, x2, y2, paint)
        }
    }

    drawSeries(activePoints, activeColor)
    drawSeries(donePoints, doneColor)

    paint.style = Paint.Style.FILL
    paint.textSize = 9f
    paint.isFakeBoldText = false
    paint.color = AndroidColor.rgb(75, 85, 99)

    val labelsY = listOf("80", "60", "40", "20", "0")
    labelsY.forEachIndexed { index, label ->
        val y = top + height * index / 4f + 3f
        canvas.drawText(label, left - 22f, y, paint)
    }

    val months = listOf("Jan", "Feb", "Mar", "Apr")
    months.forEachIndexed { index, month ->
        val x = left + width * index / 3f - 6f
        canvas.drawText(month, x, top + height + 15f, paint)
    }
}

private fun drawPdfLegend(
    canvas: android.graphics.Canvas,
    paint: Paint,
    label: String,
    value: Int,
    total: Int,
    color: Int,
    x: Float,
    y: Float
) {
    paint.style = Paint.Style.FILL
    paint.color = color
    canvas.drawRoundRect(x, y - 10f, x + 14f, y + 4f, 4f, 4f, paint)

    paint.color = AndroidColor.rgb(31, 41, 55)
    paint.textSize = 11f
    paint.isFakeBoldText = false
    canvas.drawText("$label: $value • ${percent(value, total)}%", x + 22f, y + 1f, paint)
}
