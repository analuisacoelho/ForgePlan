package com.example.forgeplan.timeline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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

            selectedProject?.let { project ->
                taskViewModel.loadTasks(project.id)
            }
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
        modifier = Modifier.fillMaxSize()
    ) {
        ForgePlanTopBar(
            title = "ForgePlan",
            initials = "FP"
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            ForgeSearchBar(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = "Search your task"
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Your Timeline",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Check here your timeline. Check your deadlines and starting dates according to your projects and tasks.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
            )

            Spacer(modifier = Modifier.height(16.dp))

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
            }

            taskError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (filteredTasks.isEmpty()) {
                ForgeCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
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
            .width(92.dp)
            .height(34.dp)
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
            style = MaterialTheme.typography.bodySmall,
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
    pending: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(460.dp)
    ) {
        TimelineBoard(
            tasks = tasks,
            mode = mode,
            projectName = projectName
        )

        TimelineSummary(
            finished = finished,
            active = active,
            pending = pending,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .offset(y = (-42).dp)
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

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        val taskColumnWidth = 96.dp
        val gridWidth = maxWidth - taskColumnWidth
        val columnWidth = gridWidth / 5f
        val boardHeight = 390.dp
        val headerHeight = 42.dp
        val gridHeight = boardHeight - headerHeight

        ForgeCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(boardHeight)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .width(taskColumnWidth)
                        .height(boardHeight)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Tasks",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    visibleTasks.forEach { task ->
                        TimelineTaskLabel(
                            task = task,
                            projectName = projectName
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Column(
                    modifier = Modifier.width(gridWidth)
                ) {
                    Row {
                        columns.forEachIndexed { index, column ->
                            Text(
                                text = column,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .width(columnWidth)
                                    .height(headerHeight)
                                    .background(
                                        if (index == 2)
                                            MaterialTheme.colorScheme.tertiary
                                        else
                                            MaterialTheme.colorScheme.secondaryContainer
                                    )
                                    .padding(top = 5.dp, start = 1.dp, end = 1.dp),
                                color =
                                    if (index == 2)
                                        MaterialTheme.colorScheme.onTertiary
                                    else
                                        MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(gridWidth)
                            .height(gridHeight)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        TimelineGrid(
                            gridWidth = gridWidth,
                            columnWidth = columnWidth,
                            gridHeight = gridHeight
                        )

                        visibleTasks.forEachIndexed { index, task ->
                            val progress = task.completion_rate ?: 0

                            val statusText = when (task.status?.uppercase()) {
                                "DONE" -> "100% Finished"
                                "IN_PROGRESS" -> "$progress% Active"
                                else -> "$progress% Pending"
                            }

                            val barStart =
                                if (mode == "Week") {
                                    (columnWidth.value * (index % 4) + 4f).dp
                                } else {
                                    (columnWidth.value * (index % 5) + 4f).dp
                                }

                            val barWidth =
                                when {
                                    progress >= 100 -> (columnWidth.value * 2.25f).dp
                                    progress >= 70 -> (columnWidth.value * 2.1f).dp
                                    progress >= 40 -> (columnWidth.value * 1.95f).dp
                                    progress > 0 -> (columnWidth.value * 1.75f).dp
                                    else -> (columnWidth.value * 2.0f).dp
                                }

                            TimelineProgressBarPositioned(
                                text = statusText,
                                width = barWidth,
                                top = (index * 38 + 8).dp,
                                start = barStart
                            )
                        }
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
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            )
                    )
                }
            }
        }

        Column {
            repeat(8) {
                Box(
                    modifier = Modifier
                        .width(gridWidth)
                        .height(1.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                )

                Spacer(modifier = Modifier.height(39.dp))
            }
        }
    }
}

@Composable
fun TimelineProgressBarPositioned(
    text: String,
    width: Dp,
    top: Dp,
    start: Dp
) {
    Box(
        modifier = Modifier
            .padding(start = start, top = top)
            .width(width)
            .height(18.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.tertiary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiary
        )
    }
}

@Composable
fun TimelineTaskLabel(
    task: Task,
    projectName: String
) {
    Column(
        modifier = Modifier.height(32.dp)
    ) {
        Text(
            text = task.title,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )

        Text(
            text = "From $projectName",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
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
    ForgeOutlinedCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Summary of the day",
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

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
            .height(58.dp)
            .background(
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
            )
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
            else -> MaterialTheme.colorScheme.onBackground
        }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = number,
            style = MaterialTheme.typography.headlineLarge,
            color = numberColor
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}