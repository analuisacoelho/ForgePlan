package com.example.forgeplan.timeline.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.ui.components.ForgeCard
import com.example.forgeplan.core.ui.components.ForgeOutlinedCard
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.core.ui.components.ForgeSearchBar
import com.example.forgeplan.projects.viewmodel.ProjectViewModel
import com.example.forgeplan.tasks.viewmodel.TaskViewModel

private object TimelineStateHolder {
    var selectedProjectId: Long? = null
    var selectedMode: String = "Week"
}

@Composable
fun TimelineScreen(
    onProjectsClick: () -> Unit = {},
    onProgressClick: () -> Unit = {},
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

    var searchText by remember { mutableStateOf("") }
    var selectedProject by remember { mutableStateOf<Project?>(null) }
    var selectedMode by remember { mutableStateOf(TimelineStateHolder.selectedMode) }

    LaunchedEffect(Unit) {
        projectViewModel.loadProjects()
    }

    LaunchedEffect(projects) {
        if (projects.isNotEmpty()) {
            selectedProject =
                projects.firstOrNull {
                    it.id == TimelineStateHolder.selectedProjectId &&
                            !it.name.equals("a", ignoreCase = true)
                }
                    ?: projects.firstOrNull {
                        it.name.contains("ForgePlan", ignoreCase = true)
                    }
                            ?: projects.first()

            TimelineStateHolder.selectedProjectId = selectedProject?.id
            selectedProject?.let { taskViewModel.loadTasks(it.id) }
        }
    }

    LaunchedEffect(selectedMode) {
        TimelineStateHolder.selectedMode = selectedMode
    }

    val filteredTasks = tasks.filter {
        it.title.contains(searchText, ignoreCase = true) ||
                (it.description ?: "").contains(searchText, ignoreCase = true)
    }

    val finishedCount = filteredTasks.count { it.status?.uppercase() == "DONE" }
    val activeCount = filteredTasks.count { it.status?.uppercase() == "IN_PROGRESS" }
    val pendingCount = filteredTasks.count {
        it.status?.uppercase() != "DONE" &&
                it.status?.uppercase() != "IN_PROGRESS"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ForgePlanTopBar(
            title = "ForgePlan",
            initials = "FP"
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    horizontal = if (isLandscape) 28.dp else 14.dp,
                    vertical = if (isLandscape) 12.dp else 16.dp
                )
        ) {
            ForgeSearchBar(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = appText(
                    en = "Search your task",
                    pt = "Pesquisar tarefa"
                )
            )

            Spacer(modifier = Modifier.height(if (isLandscape) 14.dp else 24.dp))

            Text(
                text = appText(
                    en = "Timeline",
                    pt = "Cronologia"
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(if (isLandscape) 12.dp else 18.dp))

            Row {
                TimelineToggle(
                    text = appText(en = "Week", pt = "Semana"),
                    selected = selectedMode == "Week",
                    onClick = { selectedMode = "Week" }
                )

                TimelineToggle(
                    text = appText(en = "Month", pt = "Mês"),
                    selected = selectedMode == "Month",
                    onClick = { selectedMode = "Month" }
                )
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 14.dp else 22.dp))

            projectError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            taskError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (filteredTasks.isEmpty()) {
                ForgeCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = appText(
                                en = "There are no tasks to show.",
                                pt = "Não existem tarefas para apresentar."
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                TimelineBoardWithSummary(
                    tasks = filteredTasks,
                    mode = selectedMode,
                    projectName = selectedProject?.name ?: "project",
                    finished = finishedCount,
                    active = activeCount,
                    pending = pendingCount,
                    isLandscape = isLandscape
                )
            }
        }

        ForgePlanBottomBar(
            selectedItem = "Timeline",
            onProjectsClick = onProjectsClick,
            onProgressClick = onProgressClick,
            onTeamClick = onTeamClick
        )
    }
}

@Composable
fun TimelineToggle(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(88.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.secondaryContainer
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color =
                if (selected) MaterialTheme.colorScheme.onTertiary
                else MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
fun TimelineBoardWithSummary(
    tasks: List<Task>,
    mode: String,
    projectName: String,
    finished: Int,
    active: Int,
    pending: Int,
    isLandscape: Boolean
) {
    if (isLandscape) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(330.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                TimelineBoard(
                    tasks = tasks,
                    mode = mode,
                    projectName = projectName,
                    isLandscape = true
                )
            }

            TimelineSummary(
                finished = finished,
                active = active,
                pending = pending,
                modifier = Modifier.width(230.dp)
            )
        }
    } else {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(390.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                TimelineBoard(
                    tasks = tasks,
                    mode = mode,
                    projectName = projectName,
                    isLandscape = false
                )
            }

            TimelineSummary(
                finished = finished,
                active = active,
                pending = pending,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun TimelineBoard(
    tasks: List<Task>,
    mode: String,
    projectName: String,
    isLandscape: Boolean
) {
    val visibleTasks = tasks.take(if (isLandscape) 5 else 6)

    val columns =
        if (mode == "Week") {
            listOf(
                appText(en = "Monday\n25th July", pt = "Segunda\n25 Jul."),
                appText(en = "Tuesday\n26th July", pt = "Terça\n26 Jul."),
                appText(en = "Wednesday\n27th July", pt = "Quarta\n27 Jul."),
                appText(en = "Thursday\n28th July", pt = "Quinta\n28 Jul."),
                appText(en = "Friday\n29th July", pt = "Sexta\n29 Jul.")
            )
        } else {
            listOf(
                appText(en = "Week 1", pt = "Semana 1"),
                appText(en = "Week 2", pt = "Semana 2"),
                appText(en = "Week 3", pt = "Semana 3"),
                appText(en = "Week 4", pt = "Semana 4"),
                appText(en = "Week 5", pt = "Semana 5")
            )
        }

    val taskColumnWidth = if (isLandscape) 128.dp else 112.dp
    val columnWidth = if (isLandscape) 112.dp else 98.dp
    val boardWidth = taskColumnWidth + columnWidth * 5
    val boardHeight = if (isLandscape) 330.dp else 390.dp
    val headerHeight = 48.dp
    val gridHeight = boardHeight - headerHeight

    val boardColor = MaterialTheme.colorScheme.secondaryContainer
    val taskColumnColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
    val gridLineColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.28f)

    ForgeCard(
        modifier = Modifier.width(boardWidth)
    ) {
        Row(
            modifier = Modifier
                .width(boardWidth)
                .height(boardHeight)
                .background(boardColor)
        ) {
            Column(
                modifier = Modifier
                    .width(taskColumnWidth)
                    .height(boardHeight)
                    .background(taskColumnColor)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Text(
                    text = appText(en = "Tasks", pt = "Tarefas"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(18.dp))

                visibleTasks.forEach { task ->
                    TimelineTaskLabel(
                        task = task,
                        projectName = projectName
                    )

                    Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 12.dp))
                }
            }

            Column(
                modifier = Modifier.width(columnWidth * 5)
            ) {
                Row {
                    columns.forEachIndexed { index, column ->
                        Text(
                            text = column,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .width(columnWidth)
                                .height(headerHeight)
                                .background(
                                    if (index == 2) MaterialTheme.colorScheme.tertiary
                                    else boardColor
                                )
                                .padding(top = 7.dp, start = 4.dp, end = 4.dp),
                            color =
                                if (index == 2) MaterialTheme.colorScheme.onTertiary
                                else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .width(columnWidth * 5)
                        .height(gridHeight)
                        .background(boardColor)
                ) {
                    TimelineGrid(
                        gridWidth = columnWidth * 5,
                        columnWidth = columnWidth,
                        gridHeight = gridHeight,
                        gridLineColor = gridLineColor
                    )

                    visibleTasks.forEachIndexed { index, task ->
                        val progress = task.completion_rate ?: 0
                        val normalizedStatus = task.status?.uppercase()

                        val statusText = when (normalizedStatus) {
                            "DONE" -> appText(en = "100% Finished", pt = "100% Concluída")
                            "IN_PROGRESS" -> appText(
                                en = "$progress% Active",
                                pt = "$progress% Ativa"
                            )
                            else -> appText(
                                en = "$progress% Pending",
                                pt = "$progress% Pendente"
                            )
                        }

                        val barStart =
                            if (mode == "Week") {
                                (columnWidth.value * (index % 4) + 12f).dp
                            } else {
                                (columnWidth.value * (index % 5) + 12f).dp
                            }

                        TimelineProgressBarPositioned(
                            text = statusText,
                            width = when {
                                progress >= 100 -> 190.dp
                                progress >= 70 -> 180.dp
                                progress >= 40 -> 170.dp
                                progress > 0 -> 160.dp
                                else -> 160.dp
                            },
                            top = (index * if (isLandscape) 40 else 46 + 12).dp,
                            start = barStart,
                            pending = normalizedStatus != "DONE" &&
                                    normalizedStatus != "IN_PROGRESS"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineGrid(
    gridWidth: Dp,
    columnWidth: Dp,
    gridHeight: Dp,
    gridLineColor: androidx.compose.ui.graphics.Color
) {
    Box(
        modifier = Modifier
            .width(gridWidth)
            .height(gridHeight)
    ) {
        Row {
            repeat(5) {
                Box(
                    modifier = Modifier
                        .width(columnWidth)
                        .height(gridHeight)
                ) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(gridHeight)
                            .background(gridLineColor)
                    )
                }
            }
        }

        Column {
            repeat(7) {
                Box(
                    modifier = Modifier
                        .width(gridWidth)
                        .height(1.dp)
                        .background(gridLineColor)
                )

                Spacer(modifier = Modifier.height(45.dp))
            }
        }
    }
}

@Composable
fun TimelineProgressBarPositioned(
    text: String,
    width: Dp,
    top: Dp,
    start: Dp,
    pending: Boolean
) {
    val barColor =
        if (pending) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier
            .padding(start = start, top = top)
            .width(width)
            .height(22.dp)
            .clip(RoundedCornerShape(50))
            .background(barColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color =
                if (pending) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onTertiary
        )
    }
}

@Composable
fun TimelineTaskLabel(
    task: Task,
    projectName: String
) {
    Column(
        modifier = Modifier.height(38.dp)
    ) {
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1
        )

        Text(
            text = appText(
                en = "From $projectName",
                pt = "De $projectName"
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f),
            maxLines = 1
        )
    }
}

@Composable
fun TimelineSummary(
    finished: Int,
    active: Int,
    pending: Int,
    modifier: Modifier = Modifier
) {
    ForgeOutlinedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Text(
                text = appText(
                    en = "Summary of the day",
                    pt = "Resumo do dia"
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimelineSummaryItem(
                    number = finished.toString(),
                    label = appText(en = "Finished", pt = "Concluídas"),
                    variant = "finished"
                )

                TimelineDivider()

                TimelineSummaryItem(
                    number = active.toString(),
                    label = appText(en = "Active", pt = "Ativas"),
                    variant = "active"
                )

                TimelineDivider()

                TimelineSummaryItem(
                    number = pending.toString(),
                    label = appText(en = "Pending", pt = "Pendentes"),
                    variant = "pending"
                )
            }
        }
    }
}

@Composable
fun TimelineDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(52.dp)
            .background(MaterialTheme.colorScheme.tertiary)
    )
}

@Composable
fun TimelineSummaryItem(
    number: String,
    label: String,
    variant: String
) {
    val numberColor =
        when (variant) {
            "finished" -> MaterialTheme.colorScheme.secondary
            "active" -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface
        }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = number,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = numberColor
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}