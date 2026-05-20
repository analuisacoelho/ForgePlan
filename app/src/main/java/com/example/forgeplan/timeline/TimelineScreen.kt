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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.ui.components.ForgeCard
import com.example.forgeplan.core.ui.components.ForgeOutlinedCard
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.core.ui.components.ForgeSearchBar
import com.example.forgeplan.core.ui.components.ForgeSectionTitle
import com.example.forgeplan.projects.viewmodel.ProjectViewModel
import com.example.forgeplan.tasks.viewmodel.TaskViewModel

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

    LaunchedEffect(Unit) {
        projectViewModel.loadProjects()
    }

    LaunchedEffect(projects) {
        if (selectedProject == null && projects.isNotEmpty()) {
            selectedProject = projects.first()
        }
    }

    LaunchedEffect(selectedProject?.id) {
        selectedProject?.let { project ->
            taskViewModel.loadTasks(project.id)
        }
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

            Spacer(modifier = Modifier.height(12.dp))

            TimelineProjectSelector(
                selectedProject = selectedProject,
                projects = projects,
                onProjectSelected = {
                    selectedProject = it
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            ForgeSectionTitle(text = "Timeline")

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimelineToggle(text = "Week", selected = true)
                TimelineToggle(text = "Month", selected = false)
            }

            Spacer(modifier = Modifier.height(10.dp))

            projectError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(6.dp))
            }

            taskError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(6.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                TimelineBoard(
                    tasks = filteredTasks
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            TimelineSummary(
                finished = finishedCount,
                active = activeCount,
                pending = pendingCount
            )
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
fun TimelineProjectSelector(
    selectedProject: Project?,
    projects: List<Project>,
    onProjectSelected: (Project) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TimelineSelectorCard(
            text = selectedProject?.name ?: "Select your project",
            icon = Icons.Outlined.AccountCircle,
            onClick = {
                expanded = true
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            projects.forEach { project ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
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
fun TimelineSelectorCard(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ForgeOutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(19.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.size(8.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "⌄",
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

@Composable
fun TimelineToggle(
    text: String,
    selected: Boolean
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface
            )
            .padding(horizontal = 18.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color =
                if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun TimelineBoard(
    tasks: List<Task>
) {
    val visibleTasks = tasks.take(6)

    ForgeCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(285.dp)
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.width(115.dp)
            ) {
                Text(
                    text = "Tasks",
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.height(26.dp))

                visibleTasks.forEach { task ->
                    TimelineTaskLabel(task = task)
                }
            }

            TimelineDateColumn(
                title = "Start",
                tasks = visibleTasks,
                useEndDate = false
            )

            TimelineDateColumn(
                title = "End",
                tasks = visibleTasks,
                useEndDate = true
            )

            TimelineStatusColumn(
                title = "Status",
                tasks = visibleTasks
            )

            TimelineProgressColumn(
                title = "Progress",
                tasks = visibleTasks
            )
        }
    }
}

@Composable
fun TimelineDateColumn(
    title: String,
    tasks: List<Task>,
    useEndDate: Boolean
) {
    Column(
        modifier = Modifier
            .width(118.dp)
            .padding(horizontal = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall
        )

        Spacer(modifier = Modifier.height(38.dp))

        tasks.forEach { task ->
            val date =
                if (useEndDate) {
                    task.end_date ?: "Sem data"
                } else {
                    task.start_date ?: "Sem data"
                }

            TimelineCellText(text = date)
        }
    }
}

@Composable
fun TimelineStatusColumn(
    title: String,
    tasks: List<Task>
) {
    Column(
        modifier = Modifier
            .width(132.dp)
            .padding(horizontal = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall
        )

        Spacer(modifier = Modifier.height(38.dp))

        tasks.forEach { task ->
            TimelineProgressBar(
                text = task.status ?: "PENDING",
                width = 110
            )

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
fun TimelineProgressColumn(
    title: String,
    tasks: List<Task>
) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .padding(horizontal = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall
        )

        Spacer(modifier = Modifier.height(38.dp))

        tasks.forEach { task ->
            val progress = task.completion_rate ?: 0

            TimelineProgressBar(
                text = "$progress%",
                width = (70 + progress).coerceAtMost(145)
            )

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
fun TimelineProgressBar(
    text: String,
    width: Int
) {
    Box(
        modifier = Modifier
            .width(width.dp)
            .height(24.dp)
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
    task: Task
) {
    Column(
        modifier = Modifier.height(42.dp)
    ) {
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1
        )

        Text(
            text = task.priority ?: "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun TimelineCellText(
    text: String
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.height(42.dp)
    )
}

@Composable
fun TimelineSummary(
    finished: Int,
    active: Int,
    pending: Int
) {
    ForgeCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = "Summary of the day",
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                TimelineSummaryItem(number = finished.toString(), label = "Finished")
                TimelineSummaryItem(number = active.toString(), label = "Active")
                TimelineSummaryItem(number = pending.toString(), label = "Pending")
            }
        }
    }
}

@Composable
fun TimelineSummaryItem(
    number: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = number,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}