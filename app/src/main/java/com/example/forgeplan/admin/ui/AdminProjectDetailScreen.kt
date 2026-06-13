package com.example.forgeplan.admin.ui

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.model.TaskGroup
import com.example.forgeplan.core.model.User
import com.example.forgeplan.core.ui.components.ForgeCard
import com.example.forgeplan.core.ui.components.ForgeMiniChip
import com.example.forgeplan.core.ui.components.ForgePrimaryButton
import com.example.forgeplan.core.ui.components.ForgeSearchBar
import com.example.forgeplan.core.ui.components.UserAvatarChip
import com.example.forgeplan.projects.viewmodel.ProjectDetailViewModel
import com.example.forgeplan.projects.viewmodel.ProjectUserViewModel
import com.example.forgeplan.tasks.viewmodel.TaskGroupViewModel
import com.example.forgeplan.tasks.viewmodel.TaskViewModel
import com.example.forgeplan.tasks.viewmodel.UserViewModel

/**
 * Ecrã de detalhe de projeto para o Admin.
 * Leitura de tarefas e equipa, com FAB para editar, botão para associar manager
 * e botão para arquivar (soft-delete) o projeto.
 */
@Composable
fun AdminProjectDetailScreen(
    projectId: Long,
    onBackClick: () -> Unit = {},
    onUsersClick: () -> Unit = {},
    onActivityClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    onEditProjectClick: () -> Unit = {},
    viewModel: ProjectDetailViewModel = viewModel(),
    taskViewModel: TaskViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel(),
    projectUserViewModel: ProjectUserViewModel = viewModel(),
    taskGroupViewModel: TaskGroupViewModel = viewModel()
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val project by viewModel.project.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val tasks by taskViewModel.tasks.collectAsState()
    val users by userViewModel.users.collectAsState()
    val projectUsers by projectUserViewModel.projectUsers.collectAsState()
    val taskGroups by taskGroupViewModel.groups.collectAsState()

    var searchText by remember { mutableStateOf("") }
    var showAddManagerDialog by remember { mutableStateOf(false) }
    var showArchiveDialog by remember { mutableStateOf(false) }
    var archiveError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
        taskViewModel.loadTasks(projectId)
        userViewModel.loadUsers()
        projectUserViewModel.loadProjectUsers(projectId)
        taskGroupViewModel.loadGroups(projectId)
    }

    val assignedUserIds = projectUsers.map { it.user_id }
    val assignedUsers = users.filter { assignedUserIds.contains(it.id) }

    // Só managers disponíveis para associar ao projeto
    val availableManagers = users.filter { user ->
        user.role.uppercase() == "MANAGER" && !assignedUserIds.contains(user.id)
    }

    val progress = adminCalculateProgress(tasks)

    val filteredTasks = tasks.filter { task ->
        searchText.isBlank() ||
                task.title.contains(searchText, ignoreCase = true) ||
                (task.description ?: "").contains(searchText, ignoreCase = true)
    }

    AdminScaffold(
        selectedItem = "Projects",
        onProjectsClick = onBackClick,
        onUsersClick = onUsersClick,
        onActivityClick = onActivityClick,
        onProfileClick = onProfileClick,
        onLogout = onLogout
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = if (isLandscape) 28.dp else 18.dp,
                        vertical = if (isLandscape) 12.dp else 16.dp
                    )
                    .padding(bottom = 80.dp)
            ) {
                when {
                    isLoading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    error != null -> Text(text = error ?: "", color = MaterialTheme.colorScheme.error)
                    project == null -> Text(
                        text = appText(en = "Project not found.", pt = "Projeto não encontrado."),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    else -> {
                        val currentProject = project!!

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentProject.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = appText(
                                        en = "${tasks.size} tasks • ${assignedUsers.size} team members",
                                        pt = "${tasks.size} tarefas • ${assignedUsers.size} membros da equipa"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { progress.coerceIn(0, 100) / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(50)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            if (currentProject.priority?.uppercase() == "HIGH") {
                                ForgeMiniChip(
                                    text = appText(en = "Urgent", pt = "Urgente"),
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        currentProject.description?.let { desc ->
                            if (desc.isNotBlank()) {
                                ForgeCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = appText(en = "Description", pt = "Descrição"),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(18.dp))
                            }
                        }

                        if (isLandscape) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                Column(modifier = Modifier.weight(1.2f)) {
                                    AdminTasksSection(
                                        searchText = searchText,
                                        onSearchChange = { searchText = it },
                                        tasks = filteredTasks,
                                        totalTasks = tasks.size,
                                        taskGroups = taskGroups
                                    )
                                }
                                Column(modifier = Modifier.weight(0.8f)) {
                                    AdminTeamSection(
                                        users = assignedUsers,
                                        onAddManagerClick = { showAddManagerDialog = true }
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    AdminDangerZone(
                                        archiveError = archiveError,
                                        onArchiveClick = { showArchiveDialog = true }
                                    )
                                }
                            }
                        } else {
                            AdminTasksSection(
                                searchText = searchText,
                                onSearchChange = { searchText = it },
                                tasks = filteredTasks,
                                totalTasks = tasks.size,
                                taskGroups = taskGroups
                            )
                            Spacer(modifier = Modifier.height(22.dp))
                            AdminTeamSection(
                                users = assignedUsers,
                                onAddManagerClick = { showAddManagerDialog = true }
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            AdminDangerZone(
                                archiveError = archiveError,
                                onArchiveClick = { showArchiveDialog = true }
                            )
                        }
                    }
                }
            }

            // FAB com lápis para editar o projeto
            FloatingActionButton(
                onClick = onEditProjectClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 18.dp)
                    .size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = appText(en = "Edit project", pt = "Editar projeto")
                )
            }
        }
    }

    // Dialog para associar manager ao projeto - só mostra managers disponíveis
    if (showAddManagerDialog) {
        AlertDialog(
            onDismissRequest = { showAddManagerDialog = false },
            title = {
                Text(appText(en = "Add Manager", pt = "Adicionar Gestor"))
            },
            text = {
                Column {
                    if (availableManagers.isEmpty()) {
                        Text(appText(
                            en = "No managers available.",
                            pt = "Sem gestores disponíveis."
                        ))
                    } else {
                        availableManagers.forEach { manager ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        projectUserViewModel.assignUserToProject(
                                            userId = manager.id,
                                            projectId = projectId,
                                            onSuccess = {
                                                projectUserViewModel.loadProjectUsers(projectId)
                                            }
                                        )
                                        showAddManagerDialog = false
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(manager.name)
                                Text("+")
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                Text(
                    text = appText(en = "Close", pt = "Fechar"),
                    modifier = Modifier.clickable { showAddManagerDialog = false }
                )
            }
        )
    }

    // Dialog de confirmação para arquivar o projeto
    if (showArchiveDialog) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            title = {
                Text(appText(en = "Archive project?", pt = "Arquivar projeto?"))
            },
            text = {
                Text(
                    appText(
                        en = "This project will be hidden from all lists, but its data (tasks, comments, history) will be preserved.",
                        pt = "Este projeto deixará de aparecer nas listas, mas os seus dados (tarefas, comentários, histórico) serão preservados."
                    )
                )
            },
            confirmButton = {
                Text(
                    text = appText(en = "Archive", pt = "Arquivar"),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .clickable {
                            showArchiveDialog = false
                            viewModel.archiveProject(
                                projectId = projectId,
                                onSuccess = { onBackClick() },
                                onError = { message -> archiveError = message }
                            )
                        }
                        .padding(8.dp)
                )
            },
            dismissButton = {
                Text(
                    text = appText(en = "Cancel", pt = "Cancelar"),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .clickable { showArchiveDialog = false }
                        .padding(8.dp)
                )
            }
        )
    }
}

// Secção "zona de perigo" - arquivar projeto (admin apenas)
@Composable
private fun AdminDangerZone(
    archiveError: String?,
    onArchiveClick: () -> Unit
) {
    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = appText(en = "Danger Zone", pt = "Zona de Perigo"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = appText(
                    en = "Archive this project. It will be hidden from all lists but kept for history.",
                    pt = "Arquivar este projeto. Deixa de aparecer nas listas mas é mantido no histórico."
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )

            archiveError?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onArchiveClick,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.error),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = appText(en = "Archive Project", pt = "Arquivar Projeto"),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AdminTasksSection(
    searchText: String,
    onSearchChange: (String) -> Unit,
    tasks: List<Task>,
    totalTasks: Int,
    taskGroups: List<TaskGroup>
) {
    Text(
        text = appText(en = "Tasks", pt = "Tarefas"),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(10.dp))
    ForgeSearchBar(
        value = searchText,
        onValueChange = onSearchChange,
        placeholder = appText(en = "Search task", pt = "Pesquisar tarefa")
    )
    Spacer(modifier = Modifier.height(14.dp))

    val groupedTasks = tasks.groupBy { task ->
        task.task_group?.takeIf { it.isNotBlank() }
            ?: appText(en = "No group", pt = "Sem grupo")
    }
    val savedGroupNames = taskGroups.map { it.name }

    if (groupedTasks.isEmpty() && savedGroupNames.isEmpty()) {
        ForgeCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = appText(en = "No tasks in this project.", pt = "Sem tarefas neste projeto."),
                modifier = Modifier.padding(14.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    } else {
        savedGroupNames.forEach { groupName ->
            val groupTasks = groupedTasks[groupName].orEmpty()
            Text(
                text = "$groupName (${groupTasks.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (groupTasks.isEmpty()) {
                ForgeCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = appText(en = "No tasks in this group.", pt = "Sem tarefas neste grupo."),
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            } else {
                groupTasks.forEach { task ->
                    AdminTaskCard(task = task)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        groupedTasks.filterKeys { it !in savedGroupNames }.forEach { (groupName, groupTasks) ->
            Text(
                text = "$groupName (${groupTasks.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            groupTasks.forEach { task ->
                AdminTaskCard(task = task)
                Spacer(modifier = Modifier.height(10.dp))
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }

    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = appText(
            en = "$totalTasks tasks in this project",
            pt = "$totalTasks tarefas neste projeto"
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    )
}

@Composable
private fun AdminTaskCard(task: Task) {
    val isDone = task.status?.uppercase() == "DONE"
    val isInProgress = task.status?.uppercase() == "IN_PROGRESS"
    val progress = task.completion_rate ?: 0

    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.description ?: appText(en = "No description", pt = "Sem descrição"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        maxLines = 2
                    )
                }
                ForgeMiniChip(
                    text = when (task.status?.uppercase()) {
                        "DONE" -> appText(en = "Done", pt = "Feita")
                        "IN_PROGRESS" -> appText(en = "In Progress", pt = "Em progresso")
                        else -> appText(en = "To Do", pt = "Por fazer")
                    },
                    containerColor = when {
                        isDone -> Color(0xFFB7EBC0)
                        isInProgress -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    },
                    contentColor = when {
                        isDone -> Color(0xFF14532D)
                        isInProgress -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress.coerceIn(0, 100) / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.secondaryContainer
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = appText(en = "$progress% complete", pt = "$progress% concluída"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )
        }
    }
}

// Secção da equipa com botão para adicionar manager
@Composable
private fun AdminTeamSection(
    users: List<User>,
    onAddManagerClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = appText(en = "Project Team", pt = "Equipa do Projeto"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        ForgePrimaryButton(
            text = appText(en = "Add", pt = "Adicionar"),
            onClick = onAddManagerClick
        )
    }
    Spacer(modifier = Modifier.height(10.dp))
    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (users.isEmpty()) {
                Text(
                    text = appText(
                        en = "No members assigned to this project.",
                        pt = "Sem membros associados a este projeto."
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                users.forEach { user ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatarChip(
                            initials = user.name.split(" ")
                                .mapNotNull { it.firstOrNull()?.toString() }
                                .take(2).joinToString("").uppercase()
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = user.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                        }
                        ForgeMiniChip(
                            text = user.role.lowercase().replaceFirstChar { it.uppercase() }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

private fun adminCalculateProgress(tasks: List<Task>): Int {
    if (tasks.isEmpty()) return 0
    val avg = tasks.map { it.completion_rate ?: 0 }.average().toInt()
    val done = ((tasks.count { it.status?.uppercase() == "DONE" }.toFloat() / tasks.size) * 100).toInt()
    return maxOf(avg, done).coerceIn(0, 100)
}