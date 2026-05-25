package com.example.forgeplan.projects.ui

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.model.ProjectPayload
import com.example.forgeplan.core.model.ProjectUser
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.model.TaskAssignment
import com.example.forgeplan.core.model.User
import com.example.forgeplan.core.network.SupabaseApi
import com.example.forgeplan.core.repository.ProjectUserRepository
import com.example.forgeplan.core.repository.TaskRepository
import com.example.forgeplan.core.ui.components.ForgeMiniChip
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.core.ui.components.ForgeSearchBar
import com.example.forgeplan.projects.viewmodel.ProjectViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun ManagerDashboardScreen(
    onProjectClick: (Long) -> Unit,
    onCreateProjectClick: () -> Unit,
    onEditTaskClick: (Long) -> Unit,
    onTimelineClick: () -> Unit,
    onProgressClick: () -> Unit,
    onTeamClick: () -> Unit,
    viewModel: ProjectViewModel = viewModel()
) {
    val projects by viewModel.projects.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val taskRepository = remember { TaskRepository() }
    val projectUserRepository = remember { ProjectUserRepository() }

    val projectTasks = remember { mutableStateMapOf<Long, List<Task>>() }
    val projectUsers = remember { mutableStateMapOf<Long, List<ProjectUser>>() }
    val taskAssignments = remember { mutableStateMapOf<Long, List<TaskAssignment>>() }

    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var searchText by remember { mutableStateOf("") }
    var selectedProjectId by remember { mutableStateOf<Long?>(null) }
    var exportProject by remember { mutableStateOf<Project?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadProjects()

        SupabaseApi.service.getUsers().enqueue(object : Callback<List<User>> {
            override fun onResponse(call: Call<List<User>>, response: Response<List<User>>) {
                users = response.body()?.filter { it.is_active } ?: emptyList()
            }

            override fun onFailure(call: Call<List<User>>, t: Throwable) {
                users = emptyList()
            }
        })
    }

    LaunchedEffect(projects) {
        if (selectedProjectId == null && projects.isNotEmpty()) {
            selectedProjectId = projects.first().id
        }

        projects.forEach { project ->
            taskRepository.getTasksByProjectId(
                projectId = project.id,
                onSuccess = { tasks ->
                    projectTasks[project.id] = tasks

                    tasks.forEach { task ->
                        SupabaseApi.service.getTaskAssignmentsByTaskId("eq.${task.id}")
                            .enqueue(object : Callback<List<TaskAssignment>> {
                                override fun onResponse(
                                    call: Call<List<TaskAssignment>>,
                                    response: Response<List<TaskAssignment>>
                                ) {
                                    taskAssignments[task.id] = response.body() ?: emptyList()
                                }

                                override fun onFailure(
                                    call: Call<List<TaskAssignment>>,
                                    t: Throwable
                                ) {
                                    taskAssignments[task.id] = emptyList()
                                }
                            })
                    }
                },
                onError = { projectTasks[project.id] = emptyList() }
            )

            projectUserRepository.getProjectUsersByProjectId(
                projectId = project.id,
                onSuccess = { projectUsers[project.id] = it },
                onError = { projectUsers[project.id] = emptyList() }
            )
        }
    }

    val visibleProjects = projects.filter { project ->
        if (searchText.isBlank()) {
            true
        } else {
            projectTasks[project.id]?.any { task ->
                task.title.contains(searchText, ignoreCase = true) ||
                        (task.description ?: "").contains(searchText, ignoreCase = true)
            } == true
        }
    }

    val selectedProject =
        projects.firstOrNull { it.id == selectedProjectId } ?: visibleProjects.firstOrNull()

    val visibleUserIds =
        visibleProjects.flatMap { project ->
            projectUsers[project.id]?.map { it.user_id } ?: emptyList()
        }.distinct()

    val visibleMembers = users.filter { visibleUserIds.contains(it.id) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ForgePlanTopBar(title = "ForgePlan", initials = "FP")

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                ForgeSearchBar(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = "Search task"
                )

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = "Projects",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                ProjectMemberAvatars(members = visibleMembers)

                Spacer(modifier = Modifier.height(20.dp))

                when {
                    isLoading -> CircularProgressIndicator()

                    error != null -> Text(
                        text = error ?: "Erro desconhecido",
                        color = MaterialTheme.colorScheme.error
                    )

                    visibleProjects.isEmpty() -> Text(
                        text = if (searchText.isBlank()) "No projects found." else "No tasks found.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    else -> LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(22.dp),
                        modifier = Modifier.padding(bottom = 96.dp)
                    ) {
                        items(visibleProjects) { project ->
                            ProjectCard(
                                project = project,
                                tasks = projectTasks[project.id] ?: emptyList(),
                                taskAssignments = taskAssignments,
                                users = users,
                                searchText = searchText,
                                onSelectProject = {
                                    selectedProjectId = project.id
                                },
                                onReviewClick = {
                                    selectedProjectId = project.id
                                    onProjectClick(project.id)
                                },
                                onEditTaskClick = onEditTaskClick,
                                onFinishedChange = { checked ->
                                    selectedProjectId = project.id

                                    viewModel.updateProject(
                                        projectId = project.id,
                                        project = ProjectPayload(
                                            created_by_id = project.created_by_id,
                                            manager_id = project.manager_id,
                                            name = project.name,
                                            description = project.description,
                                            priority = project.priority,
                                            status = if (checked) "DONE" else "IN_PROGRESS",
                                            start_date = project.start_date,
                                            end_date = project.end_date
                                        ),
                                        onSuccess = { viewModel.loadProjects() }
                                    )
                                }
                            )
                        }
                    }
                }
            }

            ForgePlanBottomBar(
                selectedItem = "Projects",
                onTimelineClick = onTimelineClick,
                onProgressClick = onProgressClick,
                onTeamClick = onTeamClick
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = { selectedProject?.let { exportProject = it } },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(52.dp)
            ) {
                Text("↗", style = MaterialTheme.typography.titleLarge)
            }

            FloatingActionButton(
                onClick = {
                    val project = selectedProject
                    val firstTask = project?.let { projectTasks[it.id]?.firstOrNull() }

                    if (firstTask != null) {
                        onEditTaskClick(firstTask.id)
                    }
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(52.dp)
            ) {
                Text("✎", style = MaterialTheme.typography.titleLarge)
            }

            FloatingActionButton(
                onClick = onCreateProjectClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(56.dp)
            ) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }

    exportProject?.let {
        ExportDialog(
            projectName = it.name,
            onDismiss = { exportProject = null }
        )
    }
}

@Composable
fun ProjectCard(
    project: Project,
    tasks: List<Task>,
    taskAssignments: Map<Long, List<TaskAssignment>>,
    users: List<User>,
    searchText: String,
    onSelectProject: () -> Unit,
    onReviewClick: () -> Unit,
    onEditTaskClick: (Long) -> Unit,
    onFinishedChange: (Boolean) -> Unit
) {
    val isFinished = project.status?.uppercase() == "DONE"
    val pendingTasks = tasks.filter { it.status?.uppercase() != "DONE" }

    val visibleTasks =
        if (searchText.isBlank()) {
            tasks.take(6)
        } else {
            tasks.filter {
                it.title.contains(searchText, ignoreCase = true) ||
                        (it.description ?: "").contains(searchText, ignoreCase = true)
            }.take(6)
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (visibleTasks.isEmpty()) 230.dp else 292.dp)
            .clickable { onSelectProject() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(7.dp)
                                .height(28.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "To do",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 10.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = pendingTasks.size.toString().padStart(2, '0'),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                ProjectFinishedPill(
                    isFinished = isFinished,
                    onClick = {
                        onFinishedChange(!isFinished)
                    }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (visibleTasks.isEmpty()) {
                Text(
                    text = if (searchText.isBlank()) {
                        "Ainda não existem tarefas associadas a este projeto."
                    } else {
                        "Nenhuma tarefa deste projeto corresponde à pesquisa."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    visibleTasks.forEach { task ->
                        val assignedUserIds =
                            taskAssignments[task.id]?.map { it.user_id } ?: emptyList()

                        val assignedUsers =
                            users.filter { assignedUserIds.contains(it.id) }

                        ProjectTaskPreviewCard(
                            task = task,
                            assignedUsers = assignedUsers,
                            onEditClick = {
                                onEditTaskClick(task.id)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "⚑",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (project.priority?.uppercase() == "HIGH") Color(0xFFB3261E)
                    else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "☑",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.clickable { onReviewClick() }
                )
            }
        }
    }
}

@Composable
fun ProjectFinishedPill(
    isFinished: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isFinished) "Is finished" else "Mark as finished",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.width(4.dp))

        RadioButton(
            selected = isFinished,
            onClick = onClick,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun ProjectMemberAvatars(members: List<User>) {
    if (members.isEmpty()) return

    Row(verticalAlignment = Alignment.CenterVertically) {
        members.take(4).forEachIndexed { index, user ->
            Box(
                modifier = Modifier
                    .offset(x = (-8 * index).dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        listOf(
                            Color(0xFF30258A),
                            Color(0xFF8A5A2B),
                            Color(0xFF2B9B7E),
                            Color(0xFFB4546D)
                        )[index % 4]
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userInitials(user),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (members.size > 4) {
            Box(
                modifier = Modifier
                    .offset(x = (-32).dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFB4546D)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+${members.size - 4}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
fun ProjectTaskPreviewCard(
    task: Task,
    assignedUsers: List<User>,
    onEditClick: () -> Unit
) {
    val priority = task.priority ?: "LOW"
    val finished = task.status?.uppercase() == "DONE"
    val firstUser = assignedUsers.firstOrNull()

    Card(
        modifier = Modifier
            .width(150.dp)
            .height(104.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (finished) Color(0xFF777777) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(104.dp)
                    .background(
                        when (priority.uppercase()) {
                            "HIGH" -> Color(0xFFB3261E)
                            "MEDIUM" -> Color(0xFF1D1B6E)
                            else -> Color(0xFF8A6A3A)
                        }
                    )
                    .align(Alignment.CenterStart)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 18.dp, top = 10.dp, end = 10.dp, bottom = 10.dp)
            ) {
                ForgeMiniChip(
                    text = priority.lowercase().replaceFirstChar { it.uppercase() }
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (firstUser != null && finished) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SmallTaskAvatar(user = firstUser)

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "${shortName(firstUser)} finished this task",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2
                        )
                    }
                } else {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (finished) Color.White else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = if (finished) "Done ✓" else "Mark as done ○",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (finished) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "•••",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 10.dp, top = 6.dp)
                    .clickable { onEditClick() },
                color = if (finished) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SmallTaskAvatar(user: User) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color(0xFF30258A)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = userInitials(user),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ExportDialog(
    projectName: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Always") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Just Once") } },
        title = { Text("Export as", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(projectName)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    ExportOption("PDF")
                    ExportOption("X")
                    ExportOption("W")
                    ExportOption("...")
                }
            }
        }
    )
}

@Composable
fun ExportOption(text: String) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun userInitials(user: User): String {
    return user.name
        .split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()
        .ifBlank { user.username.take(2).uppercase() }
}

private fun shortName(user: User): String {
    val parts = user.name.split(" ").filter { it.isNotBlank() }

    return when {
        parts.size >= 2 -> "${parts.first()} ${parts.last().first()}."
        parts.isNotEmpty() -> parts.first()
        else -> user.username
    }
}