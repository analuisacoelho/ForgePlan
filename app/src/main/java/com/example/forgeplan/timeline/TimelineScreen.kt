package com.example.forgeplan.timeline.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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

    Column(modifier = Modifier.fillMaxSize()) {
        ForgePlanTopBar(
            title = "ForgePlan",
            initials = "FP"
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp, vertical = 16.dp)
        ) {
            ForgeSearchBar(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = "Search your task"
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Timeline",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row {
                TimelineToggle(
                    text = "Week",
                    selected = selectedMode == "Week",
                    onClick = { selectedMode = "Week" }
                )

                TimelineToggle(
                    text = "Month",
                    selected = selectedMode == "Month",
                    onClick = { selectedMode = "Month" }
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

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
                            text = "Não existem tarefas para apresentar.",
                            style = MaterialTheme.typography.bodyMedium
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
                    pending = pendingCount
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
                else Color(0xFFC9C3FF)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color =
                if (selected) MaterialTheme.colorScheme.onTertiary
                else MaterialTheme.colorScheme.onBackground
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
    pending: Int
) {
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
                projectName = projectName
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

@Composable
fun TimelineBoard(
    tasks: List<Task>,
    mode: String,
    projectName: String
) {
    val visibleTasks = tasks.take(6)

    val columns =
        if (mode == "Week") {
            listOf(
                "Monday\n25th July",
                "Tuesday\n26th July",
                "Wednesday\n27th July",
                "Thursday\n28th July",
                "Friday\n29th July"
            )
        } else {
            listOf(
                "Week 1",
                "Week 2",
                "Week 3",
                "Week 4",
                "Week 5"
            )
        }

    val taskColumnWidth = 112.dp
    val columnWidth = 98.dp
    val boardWidth = taskColumnWidth + columnWidth * 5
    val boardHeight = 390.dp
    val headerHeight = 48.dp
    val gridHeight = boardHeight - headerHeight

    ForgeCard(
        modifier = Modifier.width(boardWidth)
    ) {
        Row(
            modifier = Modifier
                .width(boardWidth)
                .height(boardHeight)
                .background(Color(0xFFE9E6FF))
        ) {
            Column(
                modifier = Modifier
                    .width(taskColumnWidth)
                    .height(boardHeight)
                    .background(Color(0xFFC6BFFF))
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Tasks",
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

                    Spacer(modifier = Modifier.height(12.dp))
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
                                    else Color(0xFFE9E6FF)
                                )
                                .padding(top = 7.dp, start = 4.dp, end = 4.dp),
                            color =
                                if (index == 2) MaterialTheme.colorScheme.onTertiary
                                else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .width(columnWidth * 5)
                        .height(gridHeight)
                        .background(Color(0xFFE9E6FF))
                ) {
                    TimelineGrid(
                        gridWidth = columnWidth * 5,
                        columnWidth = columnWidth,
                        gridHeight = gridHeight
                    )

                    visibleTasks.forEachIndexed { index, task ->
                        val progress = task.completion_rate ?: 0
                        val normalizedStatus = task.status?.uppercase()

                        val statusText = when (normalizedStatus) {
                            "DONE" -> "100% Finished"
                            "IN_PROGRESS" -> "$progress% Active"
                            else -> "$progress% Pending"
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
                            top = (index * 46 + 12).dp,
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
    gridHeight: Dp
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
                            .background(Color(0xFF8C86C8).copy(alpha = 0.35f))
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
                        .background(Color(0xFF8C86C8).copy(alpha = 0.28f))
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
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1
        )

        Text(
            text = "From $projectName",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
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
                text = "Summary of the day",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimelineSummaryItem(
                    number = finished.toString(),
                    label = "Finished",
                    variant = "finished"
                )

                TimelineDivider()

                TimelineSummaryItem(
                    number = active.toString(),
                    label = "Active",
                    variant = "active"
                )

                TimelineDivider()

                TimelineSummaryItem(
                    number = pending.toString(),
                    label = "Pending",
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
            else -> Color(0xFF05051F)
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
            style = MaterialTheme.typography.bodySmall
        )
    }
}