package com.example.forgeplan.progress.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.ui.components.ForgeBigProgressCard
import com.example.forgeplan.core.ui.components.ForgeInfoRow
import com.example.forgeplan.core.ui.components.ForgeMiniChip
import com.example.forgeplan.core.ui.components.ForgeOutlinedCard
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.core.ui.components.ForgePrimaryLargeButton
import com.example.forgeplan.core.ui.components.ForgeSectionTitle
import com.example.forgeplan.projects.viewmodel.ProjectViewModel
import com.example.forgeplan.tasks.viewmodel.TaskViewModel

@Composable
fun ProgressScreen(
    onProjectsClick: () -> Unit = {},
    onTimelineClick: () -> Unit = {},
    onTeamClick: () -> Unit = {},
    projectViewModel: ProjectViewModel = viewModel(),
    taskViewModel: TaskViewModel = viewModel()
) {
    val projects by projectViewModel.projects.collectAsState()
    val projectError by projectViewModel.error.collectAsState()

    val tasks by taskViewModel.tasks.collectAsState()
    val taskError by taskViewModel.error.collectAsState()

    var selectedProject by remember { mutableStateOf<Project?>(null) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    var progressValue by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }

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
            selectedTask = null
            taskViewModel.loadTasks(project.id)
        }
    }

    LaunchedEffect(tasks) {
        if (selectedTask == null && tasks.isNotEmpty()) {
            selectedTask = tasks.first()
            progressValue = tasks.first().completion_rate ?: 0
        }
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
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            ForgeSectionTitle(text = "Progress")

            Spacer(modifier = Modifier.height(18.dp))

            ProjectSelector(
                selectedProject = selectedProject,
                projects = projects,
                onProjectSelected = { project ->
                    selectedProject = project
                    selectedTask = null
                    message = null
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            TaskSelector(
                selectedTask = selectedTask,
                tasks = tasks,
                onTaskSelected = { task ->
                    selectedTask = task
                    progressValue = task.completion_rate ?: 0
                    message = null
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

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

            message?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            ForgeBigProgressCard(
                progress = progressValue
            )

            Spacer(modifier = Modifier.height(14.dp))

            ProgressQuickActions(
                selectedProgress = progressValue,
                onProgressSelected = { value ->
                    progressValue = value
                    message = null
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ForgeInfoRow(
                title = "Time spent",
                value = "3 hours",
                icon = Icons.Outlined.CheckCircle
            )

            Spacer(modifier = Modifier.height(10.dp))

            ForgeInfoRow(
                title = "Location",
                value = "Workshop A",
                icon = Icons.Outlined.AccountCircle
            )

            Spacer(modifier = Modifier.height(10.dp))

            AttachmentCard()

            Spacer(modifier = Modifier.height(10.dp))

            NotesCard()

            Spacer(modifier = Modifier.height(12.dp))

            ForgePrimaryLargeButton(
                text = "Save progress",
                onClick = {
                    val task = selectedTask

                    if (task == null) {
                        message = "Seleciona uma tarefa antes de guardar."
                    } else {
                        val newStatus = when {
                            progressValue >= 100 -> "DONE"
                            progressValue > 0 -> "IN_PROGRESS"
                            else -> "PENDING"
                        }

                        val updatedTask = task.copy(
                            completion_rate = progressValue,
                            status = newStatus
                        )

                        taskViewModel.updateTask(
                            task = updatedTask,
                            onSuccess = {
                                selectedTask = updatedTask
                                message = "Progresso guardado com sucesso."
                            }
                        )
                    }
                }
            )
        }

        ForgePlanBottomBar(
            selectedItem = "Progress",
            onProjectsClick = onProjectsClick,
            onTimelineClick = onTimelineClick,
            onTeamClick = onTeamClick
        )
    }
}

@Composable
fun ProjectSelector(
    selectedProject: Project?,
    projects: List<Project>,
    onProjectSelected: (Project) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        SelectorCard(
            text = selectedProject?.name ?: "Select your project",
            icon = Icons.Outlined.AccountCircle,
            onClick = { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
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
fun TaskSelector(
    selectedTask: Task?,
    tasks: List<Task>,
    onTaskSelected: (Task) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        SelectorCard(
            text = selectedTask?.title ?: "Select your task",
            icon = Icons.Outlined.CheckCircle,
            onClick = { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            tasks.forEach { task ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onClick = {
                        onTaskSelected(task)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SelectorCard(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    ForgeOutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
fun ProgressQuickActions(
    selectedProgress: Int,
    onProgressSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(0, 25, 50, 75, 100).forEach { value ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        onProgressSelected(value)
                    },
                contentAlignment = Alignment.Center
            ) {
                ForgeMiniChip(
                    text = "$value%",
                    containerColor =
                        if (selectedProgress == value) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                    contentColor =
                        if (selectedProgress == value) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }
                )
            }
        }
    }
}

@Composable
fun AttachmentCard() {
    ForgeOutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.size(8.dp))

                Text(
                    text = "Photo attachment",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                UploadBox(
                    modifier = Modifier.weight(1f)
                )

                PreviewBox(
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun UploadBox(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(82.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Add Photo",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
fun PreviewBox(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(82.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(10.dp)
    ) {
        Text(
            text = "Preview",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.align(Alignment.Center)
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(20.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.error),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "×",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun NotesCard() {
    ForgeOutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .height(132.dp)
                .padding(14.dp)
        ) {
            Text(
                text = "Notes",
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Here you can write about your progress and any obstacles you may have encountered.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
        }
    }
}