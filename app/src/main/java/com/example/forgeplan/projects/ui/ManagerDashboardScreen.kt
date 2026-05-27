package com.example.forgeplan.projects.ui

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.language.appText
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
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

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

    val context = LocalContext.current

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ForgePlanTopBar(title = "ForgePlan", initials = "FP")

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        horizontal = if (isLandscape) 30.dp else 18.dp,
                        vertical = if (isLandscape) 12.dp else 16.dp
                    )
            ) {
                ForgeSearchBar(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = appText(en = "Search task", pt = "Pesquisar tarefa")
                )

                Spacer(modifier = Modifier.height(if (isLandscape) 14.dp else 22.dp))

                Text(
                    text = appText(en = "Projects", pt = "Projetos"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(14.dp))

                ProjectMemberAvatars(members = visibleMembers)

                Spacer(modifier = Modifier.height(if (isLandscape) 14.dp else 20.dp))

                when {
                    isLoading -> CircularProgressIndicator()

                    error != null -> Text(
                        text = error ?: appText(en = "Unknown error", pt = "Erro desconhecido"),
                        color = MaterialTheme.colorScheme.error
                    )

                    visibleProjects.isEmpty() -> Text(
                        text = if (searchText.isBlank()) {
                            appText(en = "No projects found.", pt = "Nenhum projeto encontrado.")
                        } else {
                            appText(en = "No tasks found.", pt = "Nenhuma tarefa encontrada.")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    else -> LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(if (isLandscape) 16.dp else 22.dp),
                        modifier = Modifier.padding(bottom = 96.dp)
                    ) {
                        items(visibleProjects) { project ->
                            ProjectCard(
                                project = project,
                                tasks = projectTasks[project.id] ?: emptyList(),
                                taskAssignments = taskAssignments,
                                users = users,
                                searchText = searchText,
                                isLandscape = isLandscape,
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
                .padding(
                    end = if (isLandscape) 30.dp else 18.dp,
                    bottom = if (isLandscape) 88.dp else 104.dp
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = { selectedProject?.let { exportProject = it } },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(52.dp)
            ) {
                Text("↗", style = MaterialTheme.typography.titleLarge)
            }

            FloatingActionButton(
                onClick = {
                    val allTasks = projectTasks.values.flatten()

                    if (allTasks.isNotEmpty()) {
                        onEditTaskClick(allTasks.first().id)
                    }
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(52.dp)
            ) {
                Text(
                    text = "✎",
                    style = MaterialTheme.typography.titleLarge
                )
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

    exportProject?.let { project ->
        ExportDialog(
            projectName = project.name,
            onExport = { format ->
                val tasks = projectTasks[project.id] ?: emptyList()

                exportProjectFile(
                    context = context,
                    project = project,
                    tasks = tasks,
                    format = format
                )

                exportProject = null
            },
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
    isLandscape: Boolean,
    onSelectProject: () -> Unit,
    onReviewClick: () -> Unit,
    onEditTaskClick: (Long) -> Unit,
    onFinishedChange: (Boolean) -> Unit
) {
    val isFinished = project.status?.uppercase() == "DONE"
    val pendingTasks = tasks.filter { it.status?.uppercase() != "DONE" }

    val visibleTasks =
        if (searchText.isBlank()) {
            tasks.take(if (isLandscape) 8 else 6)
        } else {
            tasks.filter {
                it.title.contains(searchText, ignoreCase = true) ||
                        (it.description ?: "").contains(searchText, ignoreCase = true)
            }.take(if (isLandscape) 8 else 6)
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(
                when {
                    isLandscape -> 248.dp
                    visibleTasks.isEmpty() -> 230.dp
                    else -> 292.dp
                }
            )
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
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
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
                            text = appText(en = "To do", pt = "Por fazer"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
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
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                ProjectFinishedPill(
                    isFinished = isFinished,
                    onClick = { onFinishedChange(!isFinished) }
                )
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 12.dp else 18.dp))

            if (visibleTasks.isEmpty()) {
                Text(
                    text = if (searchText.isBlank()) {
                        appText(
                            en = "There are no tasks associated with this project yet.",
                            pt = "Ainda não existem tarefas associadas a este projeto."
                        )
                    } else {
                        appText(
                            en = "No task in this project matches the search.",
                            pt = "Nenhuma tarefa deste projeto corresponde à pesquisa."
                        )
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
                            onEditClick = { onEditTaskClick(task.id) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "⚑",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (project.priority?.uppercase() == "HIGH") {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "☑",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
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
            .background(MaterialTheme.colorScheme.background)
            .clickable { onClick() }
            .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isFinished) {
                appText(en = "Is finished", pt = "Concluído")
            } else {
                appText(en = "Mark as finished", pt = "Marcar como concluído")
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground
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
            containerColor =
                if (finished) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.background
                }
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
                            "HIGH" -> MaterialTheme.colorScheme.error
                            "MEDIUM" -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.secondary
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
                            text = appText(
                                en = "${shortName(firstUser)} finished this task",
                                pt = "${shortName(firstUser)} concluiu esta tarefa"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 2
                        )
                    }
                } else {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color =
                            if (finished) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onBackground
                            },
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = if (finished) {
                        appText(en = "Done ✓", pt = "Feita ✓")
                    } else {
                        appText(en = "Mark as done ○", pt = "Marcar feita ○")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (finished) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        }
                )
            }

            Text(
                text = "•••",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 10.dp, top = 6.dp)
                    .clickable { onEditClick() },
                color =
                    if (finished) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    }
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
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = userInitials(user),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ExportDialog(
    projectName: String,
    onExport: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 70.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.tertiary
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = appText(en = "Export as", pt = "Exportar como"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExportOption("PDF", Color(0xFFE53935)) { onExport("PDF") }
                        ExportOption("X", Color(0xFF168A45)) { onExport("X") }
                        ExportOption("W", Color(0xFF1E5AA8)) { onExport("W") }
                        ExportOption("•••", MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)) {
                            onExport("TXT")
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(26.dp)
                    ) {
                        TextButton(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            onClick = { onExport("PDF") }
                        ) {
                            Text(
                                text = appText(en = "Just Once", pt = "Só uma vez"),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TextButton(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary),
                            onClick = { onExport("PDF") }
                        ) {
                            Text(
                                text = appText(en = "Always", pt = "Sempre"),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExportOption(
    text: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when (text) {
            "PDF" -> FileExportIcon(label = "PDF", color = Color(0xFFE53935))
            "X" -> OfficeExportIcon(label = "X", color = Color(0xFF168A45))
            "W" -> OfficeExportIcon(label = "W", color = Color(0xFF1E5AA8))
            else -> MoreExportIcon()
        }
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

@Composable
fun FileExportIcon(
    label: String,
    color: Color
) {
    Box(
        modifier = Modifier.size(62.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(58.dp)) {
            val w = size.width
            val h = size.height

            drawRoundRect(
                color = color,
                topLeft = Offset(w * 0.22f, h * 0.08f),
                size = Size(w * 0.56f, h * 0.72f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.055f)
            )

            drawLine(
                color = color,
                start = Offset(w * 0.58f, h * 0.08f),
                end = Offset(w * 0.78f, h * 0.26f),
                strokeWidth = w * 0.055f
            )

            drawRect(
                color = color,
                topLeft = Offset(w * 0.14f, h * 0.54f),
                size = Size(w * 0.66f, h * 0.28f)
            )
        }

        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 22.dp)
        )
    }
}

@Composable
fun OfficeExportIcon(
    label: String,
    color: Color
) {
    Box(
        modifier = Modifier.size(62.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(58.dp)) {
            val w = size.width
            val h = size.height

            drawRect(
                color = color,
                topLeft = Offset(w * 0.18f, h * 0.18f),
                size = Size(w * 0.42f, h * 0.62f)
            )

            drawRect(
                color = color.copy(alpha = 0.35f),
                topLeft = Offset(w * 0.52f, h * 0.24f),
                size = Size(w * 0.28f, h * 0.50f)
            )

            repeat(3) { i ->
                val y = h * (0.34f + i * 0.13f)
                drawLine(
                    color = Color.White,
                    start = Offset(w * 0.58f, y),
                    end = Offset(w * 0.75f, y),
                    strokeWidth = w * 0.035f
                )
            }
        }

        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.headlineSmall
        )
    }
}

@Composable
fun MoreExportIcon() {
    Box(
        modifier = Modifier
            .size(62.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "•••",
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.headlineMedium
        )
    }
}