package com.example.forgeplan.tasks.ui

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.session.SessionManager
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.tasks.viewmodel.ProjectTasksViewModel
import androidx.compose.ui.Alignment
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar

// ─────────────────────────────────────────────
// STRINGS (PT / EN)
// ─────────────────────────────────────────────

private object ProjectTasksStrings {
    val title get() = appText("Project Tasks", "Tarefas do Projeto")
    val noTasks get() = appText("No tasks available", "Sem tarefas disponíveis")
    val progress get() = appText("Progress", "Progresso")
    val done get() = appText("Done", "Feita")
    val pending get() = appText("Pending", "Pendente")
}

// ─────────────────────────────────────────────
// SCREEN
// ─────────────────────────────────────────────

@Composable
fun ProjectTasksScreen(
    projectId: Long,
    onBack: () -> Unit,
    onTaskClick: (Long) -> Unit = {},
    onTimelineClick: () -> Unit = {},
    onProgressClick: () -> Unit = {},
    onTeamClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val vm: ProjectTasksViewModel = viewModel()

    val tasks by vm.tasks.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()

    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(projectId) {
        vm.loadProjectTasks(projectId)
    }

    Scaffold(
        topBar = {
            ForgePlanTopBar(
                title = ProjectTasksStrings.title,
                initials = SessionManager.userInitials
            )
        },
        bottomBar = {
            ForgePlanBottomBar(
                selectedItem = "Tasks",
                onTimelineClick = onTimelineClick,
                onProgressClick = onProgressClick,
                onTeamClick = onTeamClick,
                onProfileClick = onProfileClick
            )
        }
    ) { padding ->

        TaskList(
            padding = padding,
            tasks = tasks,
            isLoading = isLoading,
            error = error,
            onTaskClick = onTaskClick,
            onMarkDone = { task ->
                vm.updateTask(
                    task.copy(
                        status = "DONE",
                        completion_rate = 100
                    )
                )
            }
        )
    }
}

// ─────────────────────────────────────────────
// LISTA
// ─────────────────────────────────────────────

@Composable
private fun TaskList(
    padding: PaddingValues,
    tasks: List<Task>,
    isLoading: Boolean,
    error: String?,
    onTaskClick: (Long) -> Unit,
    onMarkDone: (Task) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    val activeTasks = tasks.filter {
        it.status?.uppercase() != "DONE"
    }

    val completedTasks = tasks.filter {
        it.status?.uppercase() == "DONE"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {

        TabRow(selectedTabIndex = selectedTab) {

            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        appText(
                            "Active (${activeTasks.size})",
                            "Ativas (${activeTasks.size})"
                        )
                    )
                }
            )

            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        appText(
                            "Completed (${completedTasks.size})",
                            "Concluídas (${completedTasks.size})"
                        )
                    )
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            if (isLoading) {
                item { LoadingBox() }
            }

            error?.let {
                item { ErrorBox(it) }
            }

            val displayedTasks =
                if (selectedTab == 0)
                    activeTasks
                else
                    completedTasks

            if (!isLoading && displayedTasks.isEmpty()) {
                item {
                    Text(
                        text = if (selectedTab == 0)
                            appText(
                                "No active tasks",
                                "Sem tarefas ativas"
                            )
                        else
                            appText(
                                "No completed tasks",
                                "Sem tarefas concluídas"
                            ),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            items(displayedTasks) { task ->
                TaskCard(
                    task = task,
                    onClick = {
                        onTaskClick(task.id)
                    },
                    onMarkDone = {
                        onMarkDone(task)
                    }
                )
            }
        }
    }
}
// ─────────────────────────────────────────────
// TASK CARD (MESMO ESTILO DOS PROJECTS)
// ─────────────────────────────────────────────

@Composable
fun TaskCard(
    task: Task,
    onClick: () -> Unit,
    onMarkDone: () -> Unit
) {
    val isDone = task.status?.uppercase() == "DONE"
    val progress = task.completion_rate ?: 0

    val containerColor = if (isDone)
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    else
        MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Box(Modifier.padding(18.dp)) {

            Column {

                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = if (isDone) ProjectTasksStrings.done else ProjectTasksStrings.pending,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(ProjectTasksStrings.progress)
                    Text("$progress%", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                )
            }

            // 🔥 BOTÃO QUICK DONE
            if (!isDone) {
                IconButton(
                    onClick = onMarkDone,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Mark done",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// STATES
// ─────────────────────────────────────────────

@Composable
private fun LoadingBox() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorBox(msg: String) {
    Text(
        text = msg,
        color = MaterialTheme.colorScheme.error
    )
}

@Composable
private fun EmptyBox() {
    Text(ProjectTasksStrings.noTasks)
}