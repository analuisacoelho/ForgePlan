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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.repository.TaskRepository
import com.example.forgeplan.core.ui.components.ForgeMiniChip
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.core.ui.components.ForgeSearchBar
import com.example.forgeplan.projects.viewmodel.ProjectViewModel

data class PendingExport(
    val fileName: String,
    val content: String
)

data class FakeUserReport(
    val name: String,
    val role: String,
    val totalTasks: Int,
    val completed: Int,
    val hours: Float,
    val projects: Int
)

@Composable
fun ReportsScreen(
    onProjectsClick: () -> Unit = {},
    onTimelineClick: () -> Unit = {},
    onTeamClick: () -> Unit = {},
    viewModel: ProjectViewModel = viewModel()
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current

    val projects by viewModel.projects.collectAsState()
    val error by viewModel.error.collectAsState()

    val taskRepository = remember { TaskRepository() }
    val projectTasks = remember { mutableStateMapOf<Long, List<Task>>() }

    var selectedType by remember { mutableStateOf("PROJECT") }
    var searchText by remember { mutableStateOf("") }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    var pendingExport by remember { mutableStateOf<PendingExport?>(null) }

    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            pendingExport?.let { export ->
                writeReportTextFile(context, it, export.content)
                exportMessage = appText(
                    en = "CSV exported successfully.",
                    pt = "CSV exportado com sucesso."
                )
            }
        }
        pendingExport = null
    }

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let {
            pendingExport?.let { export ->
                writeReportPdfFile(context, it, export.content)
                exportMessage = appText(
                    en = "PDF exported successfully.",
                    pt = "PDF exportado com sucesso."
                )
            }
        }
        pendingExport = null
    }

    fun exportReport(format: String, content: String) {
        val extension = format.lowercase()
        val fileName = "forgeplan_${selectedType.lowercase()}_report.$extension"

        pendingExport = PendingExport(
            fileName = fileName,
            content = content
        )

        if (format == "PDF") {
            createPdfLauncher.launch(fileName)
        } else {
            createCsvLauncher.launch(fileName)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadProjects()
    }

    LaunchedEffect(projects) {
        projects.forEach { project ->
            taskRepository.getTasksByProjectId(
                projectId = project.id,
                onSuccess = { tasks ->
                    projectTasks[project.id] = tasks
                },
                onError = {
                    projectTasks[project.id] = emptyList()
                }
            )
        }
    }

    val allTasks = projectTasks.values.flatten()
    val doneTasks = allTasks.count { it.status?.uppercase() == "DONE" }
    val activeTasks = allTasks.count { it.status?.uppercase() == "IN_PROGRESS" }
    val pendingTasks = allTasks.size - doneTasks - activeTasks

    val fakeUsers = remember {
        listOf(
            FakeUserReport("Ana Coelho", "Manager", 12, 9, 42.5f, 3),
            FakeUserReport("Tiago Araújo", "Worker", 8, 5, 26f, 2),
            FakeUserReport("Diana Matos", "Worker", 7, 4, 21.5f, 2),
            FakeUserReport("Gestor Projeto", "Manager", 10, 7, 35f, 4)
        )
    }

    val filteredProjects = projects.filter {
        searchText.isBlank() ||
                it.name.contains(searchText, ignoreCase = true) ||
                (it.description ?: "").contains(searchText, ignoreCase = true)
    }

    val selectedProject = filteredProjects.firstOrNull()

    val filteredTasks = allTasks.filter {
        searchText.isBlank() ||
                it.title.contains(searchText, ignoreCase = true) ||
                (it.description ?: "").contains(searchText, ignoreCase = true)
    }

    val filteredUsers = fakeUsers.filter {
        searchText.isBlank() ||
                it.name.contains(searchText, ignoreCase = true) ||
                it.role.contains(searchText, ignoreCase = true)
    }

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
                    horizontal = if (isLandscape) 34.dp else 18.dp,
                    vertical = if (isLandscape) 14.dp else 18.dp
                )
                .padding(bottom = 96.dp)
        ) {
            Text(
                text = appText(en = "Reports & Statistics", pt = "Relatórios e Estatísticas"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = appText(
                    en = "Export data by project, user, or task",
                    pt = "Exporta dados por projeto, utilizador ou tarefa"
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
            )

            Spacer(modifier = Modifier.height(18.dp))

            ReportsSegmentedSelector(
                selectedType = selectedType,
                onTypeSelected = {
                    selectedType = it
                    searchText = ""
                    exportMessage = null
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ForgeSearchBar(
                value = searchText,
                onValueChange = {
                    searchText = it
                    exportMessage = null
                },
                placeholder = when (selectedType) {
                    "USER" -> appText(en = "Search user", pt = "Pesquisar utilizador")
                    "TASK" -> appText(en = "Search task", pt = "Pesquisar tarefa")
                    else -> appText(en = "Search project", pt = "Pesquisar projeto")
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            when (selectedType) {
                "PROJECT" -> ProjectStatisticsSection(
                    project = selectedProject,
                    projects = filteredProjects,
                    projectTasks = projectTasks,
                    totalTasks = allTasks.size,
                    done = doneTasks,
                    active = activeTasks,
                    pending = pendingTasks,
                    exportMessage = exportMessage,
                    onExport = { format ->
                        exportReport(
                            format = format,
                            content = buildProjectReport(filteredProjects, projectTasks, format)
                        )
                    }
                )

                "USER" -> UserStatisticsSection(
                    users = filteredUsers,
                    exportMessage = exportMessage,
                    onExport = { format ->
                        exportReport(
                            format = format,
                            content = buildUserReport(filteredUsers, format)
                        )
                    }
                )

                "TASK" -> TaskStatisticsSection(
                    tasks = filteredTasks,
                    done = filteredTasks.count { it.status?.uppercase() == "DONE" },
                    active = filteredTasks.count { it.status?.uppercase() == "IN_PROGRESS" },
                    pending = filteredTasks.count {
                        it.status?.uppercase() != "DONE" &&
                                it.status?.uppercase() != "IN_PROGRESS"
                    },
                    exportMessage = exportMessage,
                    onExport = { format ->
                        exportReport(
                            format = format,
                            content = buildTaskReport(filteredTasks, format)
                        )
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
fun ReportsSegmentedSelector(
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    ReportSectionCard(
        title = appText(en = "Select report type", pt = "Selecionar tipo de relatório")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReportsSegmentButton(
                text = appText(en = "By Project", pt = "Por Projeto"),
                selected = selectedType == "PROJECT",
                onClick = { onTypeSelected("PROJECT") },
                modifier = Modifier.weight(1f)
            )

            ReportsSegmentButton(
                text = appText(en = "By User", pt = "Por Utilizador"),
                selected = selectedType == "USER",
                onClick = { onTypeSelected("USER") },
                modifier = Modifier.weight(1f)
            )

            ReportsSegmentButton(
                text = appText(en = "By Task", pt = "Por Tarefa"),
                selected = selectedType == "TASK",
                onClick = { onTypeSelected("TASK") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ReportsSegmentButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.background
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color =
                if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun ProjectStatisticsSection(
    project: Project?,
    projects: List<Project>,
    projectTasks: Map<Long, List<Task>>,
    totalTasks: Int,
    done: Int,
    active: Int,
    pending: Int,
    exportMessage: String?,
    onExport: (String) -> Unit
) {
    ReportSectionCard(
        title = project?.let {
            appText(en = "Project: ${it.name}", pt = "Projeto: ${it.name}")
        } ?: appText(en = "Project overview", pt = "Visão geral dos projetos")
    ) {
        ReportsMetricRow(
            firstValue = totalTasks.toString(),
            firstLabel = appText(en = "Total Tasks", pt = "Total de Tarefas"),
            secondValue = done.toString(),
            secondLabel = appText(en = "Completed", pt = "Concluídas"),
            thirdValue = "${totalTasks * 3}h",
            thirdLabel = appText(en = "Total Hours", pt = "Horas Totais"),
            fourthValue = projects.size.toString(),
            fourthLabel = appText(en = "Projects", pt = "Projetos")
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    ReportSectionCard(
        title = appText(en = "Task Status Distribution", pt = "Distribuição do Estado das Tarefas")
    ) {
        ReportsDonutChart(
            todo = pending,
            active = active,
            done = done
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    ReportSectionCard(
        title = appText(en = "Project performance", pt = "Desempenho por projeto")
    ) {
        projects.forEach { currentProject ->
            val tasks = projectTasks[currentProject.id] ?: emptyList()
            val currentDone = tasks.count { it.status?.uppercase() == "DONE" }
            val currentProgress =
                if (tasks.isEmpty()) 0
                else ((currentDone.toFloat() / tasks.size.toFloat()) * 100).toInt()

            Text(
                text = currentProject.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(5.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ForgeMiniChip(
                    text = appText(en = "${tasks.size} tasks", pt = "${tasks.size} tarefas")
                )

                ForgeMiniChip(text = "$currentProgress%")
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { currentProgress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (currentProgress >= 100) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.tertiary
                },
                trackColor = MaterialTheme.colorScheme.secondaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    ReportExportCard(
        title = appText(en = "Export Project Report", pt = "Exportar Relatório de Projeto"),
        exportMessage = exportMessage,
        onExport = onExport
    )
}

@Composable
fun UserStatisticsSection(
    users: List<FakeUserReport>,
    exportMessage: String?,
    onExport: (String) -> Unit
) {
    val totalTasks = users.sumOf { it.totalTasks }
    val completed = users.sumOf { it.completed }
    val hours = users.sumOf { it.hours.toDouble() }.toFloat()

    ReportSectionCard(
        title = users.firstOrNull()?.let {
            appText(en = "User: ${it.name}", pt = "Utilizador: ${it.name}")
        } ?: appText(en = "User report", pt = "Relatório por utilizador")
    ) {
        ReportsMetricRow(
            firstValue = totalTasks.toString(),
            firstLabel = appText(en = "Total Tasks", pt = "Total de Tarefas"),
            secondValue = completed.toString(),
            secondLabel = appText(en = "Completed", pt = "Concluídas"),
            thirdValue = "${hours}h",
            thirdLabel = appText(en = "Total Hours", pt = "Horas Totais"),
            fourthValue = users.sumOf { it.projects }.toString(),
            fourthLabel = appText(en = "Projects", pt = "Projetos")
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    ReportSectionCard(
        title = appText(en = "Tasks & Hours by Project", pt = "Tarefas e Horas por Projeto")
    ) {
        ReportsTableHeader(
            c1 = appText(en = "User", pt = "Utilizador"),
            c2 = appText(en = "Tasks", pt = "Tarefas"),
            c3 = appText(en = "Hours", pt = "Horas"),
            c4 = appText(en = "Avg.", pt = "Média")
        )

        users.forEach { user ->
            ReportsTableRow(
                c1 = user.name,
                c2 = user.completed.toString(),
                c3 = "${user.hours}h",
                c4 = "${if (user.completed == 0) 0f else user.hours / user.completed}h"
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    ReportSectionCard(
        title = appText(en = "Performance Trend", pt = "Tendência de Performance")
    ) {
        ReportsTrendChart()
    }

    Spacer(modifier = Modifier.height(16.dp))

    ReportExportCard(
        title = appText(en = "Export User Report", pt = "Exportar Relatório de Utilizador"),
        exportMessage = exportMessage,
        onExport = onExport
    )
}

@Composable
fun TaskStatisticsSection(
    tasks: List<Task>,
    done: Int,
    active: Int,
    pending: Int,
    exportMessage: String?,
    onExport: (String) -> Unit
) {
    ReportSectionCard(
        title = appText(en = "Task Status Distribution", pt = "Distribuição de tarefas")
    ) {
        ReportsDonutChart(
            todo = pending,
            active = active,
            done = done
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    ReportSectionCard(
        title = appText(en = "Task details", pt = "Detalhes das tarefas")
    ) {
        if (tasks.isEmpty()) {
            Text(
                text = appText(en = "No tasks found.", pt = "Nenhuma tarefa encontrada."),
                color = MaterialTheme.colorScheme.onSurface
            )
        } else {
            tasks.take(8).forEach { task ->
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "${readableReportTaskStatus(task.status)} • ${task.completion_rate ?: 0}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    ReportExportCard(
        title = appText(en = "Export Task Report", pt = "Exportar Relatório de Tarefas"),
        exportMessage = exportMessage,
        onExport = onExport
    )
}

@Composable
fun ReportsMetricRow(
    firstValue: String,
    firstLabel: String,
    secondValue: String,
    secondLabel: String,
    thirdValue: String,
    thirdLabel: String,
    fourthValue: String,
    fourthLabel: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ReportMetric(firstValue, firstLabel)
        ReportMetric(secondValue, secondLabel)
        ReportMetric(thirdValue, thirdLabel)
        ReportMetric(fourthValue, fourthLabel)
    }
}

@Composable
fun ReportsDonutChart(
    todo: Int,
    active: Int,
    done: Int
) {
    val total = todo + active + done

    val todoColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f)
    val activeColor = MaterialTheme.colorScheme.primary
    val doneColor = MaterialTheme.colorScheme.secondary

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .width(190.dp)
                .height(190.dp)
        ) {
            val chartTotal = total.coerceAtLeast(1)
            val todoSweep = todo / chartTotal.toFloat() * 360f
            val activeSweep = active / chartTotal.toFloat() * 360f
            val doneSweep = done / chartTotal.toFloat() * 360f

            val stroke = Stroke(width = 42f, cap = StrokeCap.Butt)
            val chartSize = Size(size.minDimension, size.minDimension)
            val topLeft = Offset(
                x = (size.width - chartSize.width) / 2,
                y = (size.height - chartSize.height) / 2
            )

            if (total == 0) {
                drawArc(
                    color = todoColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = chartSize,
                    style = stroke
                )
            } else {
                var start = -90f

                if (todo > 0) {
                    drawArc(
                        color = todoColor,
                        startAngle = start,
                        sweepAngle = todoSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = chartSize,
                        style = stroke
                    )
                    start += todoSweep
                }

                if (active > 0) {
                    drawArc(
                        color = activeColor,
                        startAngle = start,
                        sweepAngle = activeSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = chartSize,
                        style = stroke
                    )
                    start += activeSweep
                }

                if (done > 0) {
                    drawArc(
                        color = doneColor,
                        startAngle = start,
                        sweepAngle = doneSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = chartSize,
                        style = stroke
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        ReportsChartLegend(
            todo = todo,
            active = active,
            done = done
        )
    }
}

@Composable
fun ReportsChartLegend(
    todo: Int,
    active: Int,
    done: Int
) {
    val todoColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f)
    val activeColor = MaterialTheme.colorScheme.primary
    val doneColor = MaterialTheme.colorScheme.secondary

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ReportsLegendItem(
            color = todoColor,
            text = appText(en = "To Do: $todo", pt = "Por fazer: $todo")
        )

        ReportsLegendItem(
            color = activeColor,
            text = appText(en = "Active: $active", pt = "Ativas: $active")
        )

        ReportsLegendItem(
            color = doneColor,
            text = appText(en = "Done: $done", pt = "Feitas: $done")
        )
    }
}

@Composable
fun ReportsLegendItem(
    color: androidx.compose.ui.graphics.Color,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ReportsTrendChart() {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
    ) {
        val left = 32f
        val bottom = size.height - 28f
        val top = 18f
        val right = size.width - 18f

        repeat(4) { index ->
            val y = top + index * ((bottom - top) / 3f)
            drawLine(
                color = gridColor,
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = 2f
            )
        }

        val taskPoints = listOf(8f, 12f, 10f, 16f)
        val hourPoints = listOf(35f, 52f, 45f, 77f)

        fun point(index: Int, value: Float): Offset {
            val x = left + index * ((right - left) / 3f)
            val y = bottom - (value / 80f) * (bottom - top)
            return Offset(x, y)
        }

        for (i in 0..2) {
            drawLine(
                color = primary,
                start = point(i, taskPoints[i]),
                end = point(i + 1, taskPoints[i + 1]),
                strokeWidth = 4f
            )

            drawLine(
                color = secondary,
                start = point(i, hourPoints[i]),
                end = point(i + 1, hourPoints[i + 1]),
                strokeWidth = 4f
            )
        }

        taskPoints.forEachIndexed { index, value ->
            drawCircle(color = primary, radius = 6f, center = point(index, value))
        }

        hourPoints.forEachIndexed { index, value ->
            drawCircle(color = secondary, radius = 6f, center = point(index, value))
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ForgeMiniChip(text = appText(en = "Tasks Completed", pt = "Tarefas concluídas"))
        ForgeMiniChip(text = appText(en = "Hours Worked", pt = "Horas trabalhadas"))
    }
}

@Composable
fun ReportsTableHeader(
    c1: String,
    c2: String,
    c3: String,
    c4: String
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(c1, modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold)
        Text(c2, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
        Text(c3, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
        Text(c4, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
    }

    Spacer(modifier = Modifier.height(10.dp))
}

@Composable
fun ReportsTableRow(
    c1: String,
    c2: String,
    c3: String,
    c4: String
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(c1, modifier = Modifier.weight(1.5f))
        Text(c2, modifier = Modifier.weight(1f))
        Text(c3, modifier = Modifier.weight(1f))
        Text(c4, modifier = Modifier.weight(1f))
    }

    Spacer(modifier = Modifier.height(10.dp))
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
        Column(modifier = Modifier.padding(16.dp)) {
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
fun ReportMetric(
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
    }
}

@Composable
fun ReportExportCard(
    title: String,
    exportMessage: String?,
    onExport: (String) -> Unit
) {
    ReportSectionCard(title = title) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { onExport("PDF") },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Text(
                    text = appText(en = "Export to PDF", pt = "Exportar PDF"),
                    color = MaterialTheme.colorScheme.error
                )
            }

            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { onExport("CSV") },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
            ) {
                Text(
                    text = appText(en = "Export to CSV", pt = "Exportar CSV"),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        exportMessage?.let {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun buildProjectReport(
    projects: List<Project>,
    projectTasks: Map<Long, List<Task>>,
    format: String
): String {
    return if (format == "CSV") {
        buildString {
            appendLine("Project,Status,Priority,Total Tasks,Completed Tasks,Progress")
            projects.forEach { project ->
                val tasks = projectTasks[project.id] ?: emptyList()
                val done = tasks.count { it.status?.uppercase() == "DONE" }
                val progress = if (tasks.isEmpty()) 0 else ((done.toFloat() / tasks.size) * 100).toInt()

                appendLine(
                    listOf(
                        project.name,
                        project.status ?: "",
                        project.priority ?: "",
                        tasks.size.toString(),
                        done.toString(),
                        "$progress%"
                    ).joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
                )
            }
        }
    } else {
        buildString {
            appendLine("ForgePlan - Project Report")
            appendLine()
            projects.forEach { project ->
                val tasks = projectTasks[project.id] ?: emptyList()
                val done = tasks.count { it.status?.uppercase() == "DONE" }
                val progress = if (tasks.isEmpty()) 0 else ((done.toFloat() / tasks.size) * 100).toInt()

                appendLine("Project: ${project.name}")
                appendLine("Status: ${project.status ?: "-"}")
                appendLine("Priority: ${project.priority ?: "-"}")
                appendLine("Tasks: ${tasks.size}")
                appendLine("Completed: $done")
                appendLine("Progress: $progress%")
                appendLine()
            }
        }
    }
}

private fun buildTaskReport(
    tasks: List<Task>,
    format: String
): String {
    return if (format == "CSV") {
        buildString {
            appendLine("Task,Status,Priority,Completion,Start,End")
            tasks.forEach { task ->
                appendLine(
                    listOf(
                        task.title,
                        task.status ?: "",
                        task.priority ?: "",
                        "${task.completion_rate ?: 0}%",
                        task.start_date ?: "",
                        task.end_date ?: ""
                    ).joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
                )
            }
        }
    } else {
        buildString {
            appendLine("ForgePlan - Task Report")
            appendLine()
            tasks.forEach { task ->
                appendLine("Task: ${task.title}")
                appendLine("Status: ${task.status ?: "-"}")
                appendLine("Priority: ${task.priority ?: "-"}")
                appendLine("Completion: ${task.completion_rate ?: 0}%")
                appendLine("Start: ${task.start_date ?: "-"}")
                appendLine("End: ${task.end_date ?: "-"}")
                appendLine()
            }
        }
    }
}

private fun buildUserReport(
    users: List<FakeUserReport>,
    format: String
): String {
    return if (format == "CSV") {
        buildString {
            appendLine("User,Role,Total Tasks,Completed,Hours,Projects")
            users.forEach { user ->
                appendLine(
                    listOf(
                        user.name,
                        user.role,
                        user.totalTasks.toString(),
                        user.completed.toString(),
                        "${user.hours}h",
                        user.projects.toString()
                    ).joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
                )
            }
        }
    } else {
        buildString {
            appendLine("ForgePlan - User Report")
            appendLine()
            users.forEach { user ->
                appendLine("User: ${user.name}")
                appendLine("Role: ${user.role}")
                appendLine("Total tasks: ${user.totalTasks}")
                appendLine("Completed: ${user.completed}")
                appendLine("Hours: ${user.hours}h")
                appendLine("Projects: ${user.projects}")
                appendLine()
            }
        }
    }
}

private fun readableReportTaskStatus(status: String?): String {
    return when (status?.uppercase()) {
        "DONE" -> appText(en = "Done", pt = "Feita")
        "IN_PROGRESS" -> appText(en = "In Progress", pt = "Em progresso")
        "PENDING" -> appText(en = "To Do", pt = "Por fazer")
        else -> appText(en = "To Do", pt = "Por fazer")
    }
}

private fun writeReportTextFile(
    context: Context,
    uri: Uri,
    content: String
) {
    context.contentResolver.openOutputStream(uri)?.use { output ->
        output.write(content.toByteArray())
    }
}

private fun writeReportPdfFile(
    context: Context,
    uri: Uri,
    content: String
) {
    val document = PdfDocument()
    val paint = Paint().apply {
        textSize = 12f
        color = android.graphics.Color.BLACK
    }

    val pageWidth = 595
    val pageHeight = 842
    val margin = 40
    val lineHeight = 18

    var pageNumber = 1
    var page = document.startPage(
        PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    )
    var canvas = page.canvas
    var y = margin

    content.lines().forEach { line ->
        if (y > pageHeight - margin) {
            document.finishPage(page)
            pageNumber++
            page = document.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            )
            canvas = page.canvas
            y = margin
        }

        canvas.drawText(line.take(90), margin.toFloat(), y.toFloat(), paint)
        y += lineHeight
    }

    document.finishPage(page)

    context.contentResolver.openOutputStream(uri)?.use { output ->
        document.writeTo(output)
    }

    document.close()
}