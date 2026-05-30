package com.example.forgeplan.reports.ui

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
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

@Composable
fun ReportsScreen(
    onProjectsClick: () -> Unit = {},
    onTimelineClick: () -> Unit = {},
    onTeamClick: () -> Unit = {},
    viewModel: ProjectViewModel = viewModel()
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val projects by viewModel.projects.collectAsState()
    val error by viewModel.error.collectAsState()

    val taskRepository = remember { TaskRepository() }
    val projectTasks = remember { mutableStateMapOf<Long, List<Task>>() }

    var selectedType by remember { mutableStateOf("PROJECT") }
    var searchText by remember { mutableStateOf("") }
    var exportMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadProjects()
    }

    LaunchedEffect(projects) {
        projects.forEach { project ->
            taskRepository.getTasksByProjectId(
                projectId = project.id,
                onSuccess = { tasks -> projectTasks[project.id] = tasks },
                onError = { projectTasks[project.id] = emptyList() }
            )
        }
    }

    val allTasks = projectTasks.values.flatten()
    val doneTasks = allTasks.count { it.status?.uppercase() == "DONE" }
    val activeTasks = allTasks.count { it.status?.uppercase() == "IN_PROGRESS" }
    val pendingTasks = allTasks.size - doneTasks - activeTasks

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
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ReportsSummaryCard(
                        title = appText(en = "Total Projects", pt = "Total de Projetos"),
                        value = projects.size.toString(),
                        modifier = Modifier.weight(1f)
                    )

                    ReportsSummaryCard(
                        title = appText(en = "Completed Tasks", pt = "Tarefas Concluídas"),
                        value = doneTasks.toString(),
                        modifier = Modifier.weight(1f)
                    )

                    ReportsSummaryCard(
                        title = appText(en = "Active Tasks", pt = "Tarefas Ativas"),
                        value = activeTasks.toString(),
                        modifier = Modifier.weight(1f)
                    )

                    ReportsSummaryCard(
                        title = appText(en = "Pending Tasks", pt = "Tarefas Pendentes"),
                        value = pendingTasks.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReportsSummaryCard(
                        title = appText(en = "Projects", pt = "Projetos"),
                        value = projects.size.toString(),
                        modifier = Modifier.weight(1f)
                    )

                    ReportsSummaryCard(
                        title = appText(en = "Tasks", pt = "Tarefas"),
                        value = allTasks.size.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            when (selectedType) {
                "USER" -> UserReportSection(
                    tasks = allTasks,
                    exportMessage = exportMessage,
                    onExport = { format ->
                        exportMessage = appText(
                            en = "User report exported as $format.",
                            pt = "Relatório de utilizador exportado em $format."
                        )
                    }
                )

                "TASK" -> TaskReportSection(
                    tasks = allTasks.filter {
                        searchText.isBlank() || it.title.contains(searchText, ignoreCase = true)
                    },
                    exportMessage = exportMessage,
                    onExport = { format ->
                        exportMessage = appText(
                            en = "Task report exported as $format.",
                            pt = "Relatório de tarefas exportado em $format."
                        )
                    }
                )

                else -> ProjectReportSection(
                    projects = projects.filter {
                        searchText.isBlank() || it.name.contains(searchText, ignoreCase = true)
                    },
                    projectTasks = projectTasks,
                    exportMessage = exportMessage,
                    onExport = { format ->
                        exportMessage = appText(
                            en = "Project report exported as $format.",
                            pt = "Relatório de projeto exportado em $format."
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
fun ReportsTypeSelector(
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = appText(en = "Select report type", pt = "Selecionar tipo de relatório"),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportTypeChip(
                    text = appText(en = "By Project", pt = "Por Projeto"),
                    selected = selectedType == "PROJECT",
                    onClick = { onTypeSelected("PROJECT") },
                    modifier = Modifier.weight(1f)
                )

                ReportTypeChip(
                    text = appText(en = "By User", pt = "Por Utilizador"),
                    selected = selectedType == "USER",
                    onClick = { onTypeSelected("USER") },
                    modifier = Modifier.weight(1f)
                )

                ReportTypeChip(
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
fun ReportTypeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        modifier = modifier,
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall
            )
        }
    )
}

@Composable
fun ReportsSummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(82.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
fun ProjectReportSection(
    projects: List<Project>,
    projectTasks: Map<Long, List<Task>>,
    exportMessage: String?,
    onExport: (String) -> Unit
) {
    ReportSectionCard(
        title = appText(en = "Project performance", pt = "Desempenho por projeto")
    ) {
        if (projects.isEmpty()) {
            Text(
                text = appText(en = "No projects found.", pt = "Nenhum projeto encontrado."),
                color = MaterialTheme.colorScheme.onSurface
            )
        } else {
            projects.forEach { project ->
                val tasks = projectTasks[project.id] ?: emptyList()
                val done = tasks.count { it.status?.uppercase() == "DONE" }
                val progress = if (tasks.isEmpty()) 0 else ((done.toFloat() / tasks.size) * 100).toInt()

                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row {
                    ForgeMiniChip(
                        text = appText(
                            en = "${tasks.size} tasks",
                            pt = "${tasks.size} tarefas"
                        )
                    )

                    Spacer(modifier = Modifier.padding(4.dp))

                    ForgeMiniChip(text = "$progress%")
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50)),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.secondaryContainer
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        ReportExportButtons(onExport = onExport)

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

@Composable
fun UserReportSection(
    tasks: List<Task>,
    exportMessage: String?,
    onExport: (String) -> Unit
) {
    val totalHours = tasks.size * 3

    ReportSectionCard(
        title = appText(en = "User report", pt = "Relatório por utilizador")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ReportMetric(
                value = tasks.size.toString(),
                label = appText(en = "Total tasks", pt = "Total de tarefas")
            )

            ReportMetric(
                value = tasks.count { it.status?.uppercase() == "DONE" }.toString(),
                label = appText(en = "Completed", pt = "Concluídas")
            )

            ReportMetric(
                value = "${totalHours}h",
                label = appText(en = "Estimated hours", pt = "Horas estimadas")
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        ReportExportButtons(onExport = onExport)

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

@Composable
fun TaskReportSection(
    tasks: List<Task>,
    exportMessage: String?,
    onExport: (String) -> Unit
) {
    val done = tasks.count { it.status?.uppercase() == "DONE" }
    val active = tasks.count { it.status?.uppercase() == "IN_PROGRESS" }
    val pending = tasks.size - done - active

    ReportSectionCard(
        title = appText(en = "Task status distribution", pt = "Distribuição de tarefas")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ReportMetric(
                value = pending.toString(),
                label = appText(en = "To do", pt = "Por fazer")
            )

            ReportMetric(
                value = active.toString(),
                label = appText(en = "Active", pt = "Ativas")
            )

            ReportMetric(
                value = done.toString(),
                label = appText(en = "Done", pt = "Feitas")
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        tasks.take(5).forEach { task ->
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        ReportExportButtons(onExport = onExport)

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
            modifier = Modifier.padding(16.dp)
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
fun ReportMetric(
    value: String,
    label: String
) {
    Column {
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
fun ReportExportButtons(
    onExport: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
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
                    text = appText(en = "Export PDF", pt = "Exportar PDF"),
                    color = MaterialTheme.colorScheme.error
                )
            }

            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { onExport("CSV") },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = appText(en = "Export CSV", pt = "Exportar CSV"),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onExport("TXT") },
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
        ) {
            Text(
                text = appText(en = "Export TXT", pt = "Exportar TXT"),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
