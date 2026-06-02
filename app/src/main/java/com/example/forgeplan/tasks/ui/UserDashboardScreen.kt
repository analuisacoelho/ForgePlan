package com.example.forgeplan.tasks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.network.SupabaseApi
import com.example.forgeplan.core.repository.TaskRepository
import com.example.forgeplan.core.ui.components.*
import com.example.forgeplan.projects.viewmodel.ProjectViewModel
import com.example.forgeplan.tasks.viewmodel.TaskViewModel
import com.example.forgeplan.tasks.viewmodel.UserViewModel

// ── Cores de prioridade ─────────────────────────────────────────────────────

private val PriorityHigh   = Color(0xFFE57373)   // vermelho suave
private val PriorityMedium = Color(0xFFFFCC80)   // laranja suave
private val PriorityLow    = Color(0xFFA5D6A7)   // verde suave
private val CardDone       = Color(0xFFD0D0D0)   // cinzento para concluída

// ── Ecrã principal ──────────────────────────────────────────────────────────

@Composable
fun UserDashboardScreen(
    onTimelineClick: () -> Unit = {},
    onProgressClick: () -> Unit = {},
    onTeamClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val taskViewModel: TaskViewModel = viewModel()
    val projectViewModel: ProjectViewModel = viewModel()
    val userViewModel: UserViewModel = viewModel()

    val projects by projectViewModel.projects.collectAsState()
    val isLoading by projectViewModel.isLoading.collectAsState()

    // Carrega projetos do utilizador actual ao entrar no ecrã
    LaunchedEffect(Unit) {
        projectViewModel.loadProjects()
    }


    // Guarda as tarefas de cada projecto: projectId -> List<Task>
    val tasksByProject = remember { mutableStateMapOf<Long, List<Task>>() }

    // Quando os projectos chegarem, carrega as tarefas de cada um
    LaunchedEffect(projects) {
        projects.forEach { project ->
            taskViewModel.loadTasksForDashboard(project.id) { tasks ->
                tasksByProject[project.id] = tasks
            }
        }
    }

    Scaffold(
        topBar = {
            ForgePlanTopBar(
                title = "ForgePlan",
                initials = userViewModel.currentUserInitials
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
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Cabeçalho ────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                var searchText by remember { mutableStateOf("") }

                ForgeSearchBar(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = "Search tasks..."
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Your Tasks",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Check your tasks, the projects they belong to, the\nmaterials needed and their descriptions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(8.dp))
            }

            // ── Linha de membros + filtro ─────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatares dos membros do utilizador (exemplo estático)
                    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                        listOf("UC", "AL", "NI", "+5").forEachIndexed { index, initials ->
                            UserAvatarChip(
                                initials = initials,
                                modifier = Modifier.offset(x = (index * (-6)).dp)
                            )
                        }
                    }
                    ForgeDropdownCard(text = "Filter")
                }
                Spacer(Modifier.height(4.dp))
            }

            // ── Loading ───────────────────────────────────────────────────
            if (isLoading) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    }
                }
            }

            // ── Um card por projecto ──────────────────────────────────────
            items(projects) { project ->
                val tasks = tasksByProject[project.id] ?: emptyList()
                ProjectTaskCard(
                    project = project,
                    tasks = tasks,
                    onMarkAsDone = { task ->
                        taskViewModel.markTaskAsDone(task) {
                            // Recarrega as tarefas deste projecto
                            taskViewModel.loadTasksForDashboard(project.id) { updated ->
                                tasksByProject[project.id] = updated
                            }
                        }
                    }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ── Card de projecto com as suas tarefas ────────────────────────────────────

@Composable
fun ProjectTaskCard(
    project: Project,
    tasks: List<Task>,
    onMarkAsDone: (Task) -> Unit
) {
    val todoTasks = tasks.filter { it.status != "Done" }
    val doneTasks = tasks.filter { it.status == "Done" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // Nome do projecto
            Text(
                text = project.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(6.dp))

            // Etiqueta "To do" + contagem
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "To do",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = todoTasks.size.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Grelha de tarefas (2 colunas)
            if (todoTasks.isNotEmpty()) {
                TaskGrid(tasks = todoTasks, onMarkAsDone = onMarkAsDone)
            } else {
                Text(
                    text = "Sem tarefas pendentes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Ícone de comentário / chat
            Spacer(Modifier.height(4.dp))
            Icon(
                imageVector = Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            )
        }
    }
}

// ── Grelha 2 colunas de tarefas ─────────────────────────────────────────────

@Composable
fun TaskGrid(
    tasks: List<Task>,
    onMarkAsDone: (Task) -> Unit
) {
    // Divide as tarefas em linhas de 2
    val rows = tasks.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { task ->
                    TaskMiniCard(
                        task = task,
                        modifier = Modifier.weight(1f),
                        onMarkAsDone = { onMarkAsDone(task) }
                    )
                }
                // Preenche coluna vazia se a linha tiver apenas 1 item
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// ── Mini-card de tarefa ──────────────────────────────────────────────────────

@Composable
fun TaskMiniCard(
    task: Task,
    modifier: Modifier = Modifier,
    onMarkAsDone: () -> Unit
) {
    val isDone = task.status == "Done"
    val bgColor = if (isDone) CardDone else priorityColor(task.priority)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chip de prioridade
                Surface(
                    shape = RoundedCornerShape(50),
                    color = priorityLabelColor(task.priority)
                ) {
                    Text(
                        text = task.priority?.replaceFirstChar { it.uppercase() } ?: "—",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = Color.White
                    )
                }
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Opções",
                    modifier = Modifier.size(16.dp),
                    tint = Color.White.copy(alpha = 0.8f)
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = task.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 2
            )

            Spacer(Modifier.height(10.dp))

            // Botão "Mark as done" ou ícone de concluído
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isDone) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Done",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = "Concluída",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.clickable { onMarkAsDone() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Mark as done",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = "Marcar como concluída",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Helpers de cor ───────────────────────────────────────────────────────────

private fun priorityColor(priority: String?): Color = when (priority?.lowercase()) {
    "high"   -> Color(0xFFEF9A9A)
    "medium" -> Color(0xFFFFCC80)
    "low"    -> Color(0xFFA5D6A7)
    else     -> Color(0xFFBDBDBD)
}

private fun priorityLabelColor(priority: String?): Color = when (priority?.lowercase()) {
    "high"   -> Color(0xFFE53935)
    "medium" -> Color(0xFFFB8C00)
    "low"    -> Color(0xFF43A047)
    else     -> Color(0xFF757575)
}