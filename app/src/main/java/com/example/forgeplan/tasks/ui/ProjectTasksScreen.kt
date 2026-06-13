package com.example.forgeplan.tasks.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
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
import com.example.forgeplan.timeline.ui.parseDate
import androidx.compose.ui.Alignment
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.forgeplan.core.repository.TaskDependencyRepository
import com.example.forgeplan.ui.theme.ForgeGold
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// ─────────────────────────────────────────────
// STRINGS (PT / EN)
// ─────────────────────────────────────────────

private object ProjectTasksStrings {
    val title get() = appText("Project Tasks", "Tarefas do Projeto")
    val noTasks get() = appText("No tasks available", "Sem tarefas disponíveis")
    val progress get() = appText("Progress", "Progresso")
    val done get() = appText("Done", "Feita")
    val pending get() = appText("Pending", "Pendente")
    val inProgress get() = appText("In Progress", "Em Progresso")
    val sortLabel get() = appText("Sort", "Ordenar")
    val sortDefault get() = appText("Default", "Padrão")
    val sortByDate get() = appText("By Date", "Por Data")
    val sortByCompletion get() = appText("By Completion", "Por Conclusão")
}

private enum class TaskSortType { DEFAULT, DATE, COMPLETION }

// ─────────────────────────────────────────────
// SCREEN
// ─────────────────────────────────────────────

@Composable
fun ProjectTasksScreen(
    projectId: Long,
    onBack: () -> Unit,
    onMyTaskClick: (Long) -> Unit = {},
    onOtherTaskClick: (Long) -> Unit = {},
    onTimelineClick: () -> Unit = {},
    onProgressClick: () -> Unit = {},
    onTeamClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {
    val vm: ProjectTasksViewModel = viewModel()

    val tasks     by vm.tasks.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error     by vm.error.collectAsState()

    val myUserId = SessionManager.userId
    var myTaskIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    // Map: taskId -> list of task IDs it depends on
    var dependencyMap by remember { mutableStateOf<Map<Long, List<Long>>>(emptyMap()) }

    val notifVm: com.example.forgeplan.notifications.viewmodel.NotificationViewModel = viewModel()
    val unreadCount by notifVm.unreadCount.collectAsState()

    LaunchedEffect(projectId) {
        vm.loadProjectTasks(projectId)
        notifVm.load()
        val repo = com.example.forgeplan.core.repository.TaskAssignmentRepository()
        repo.getTaskIdsByUserId(
            userId    = myUserId,
            onSuccess = { myTaskIds = it.toSet() },
            onError   = { myTaskIds = emptySet() }
        )
    }

    // Load dependencies for all tasks once loaded — sequencial para evitar race condition
    LaunchedEffect(tasks) {
        if (tasks.isNotEmpty()) {
            val depRepo = TaskDependencyRepository()
            val map = mutableMapOf<Long, List<Long>>()
            tasks.forEach { task ->
                val deps = suspendCancellableCoroutine<List<Long>> { cont ->
                    depRepo.getDependencies(
                        taskId    = task.id,
                        onSuccess = { result -> cont.resume(result.map { it.depends_on_task_id }) },
                        onError   = { cont.resume(emptyList()) }
                    )
                }
                map[task.id] = deps
            }
            dependencyMap = map
        }
    }

    Scaffold(
        topBar = {
            ForgePlanTopBar(
                title    = ProjectTasksStrings.title,
                initials = SessionManager.userInitials,
                onNotificationClick = onNotificationClick,
                unreadCount = unreadCount,
                onAvatarClick = onProfileClick
            )
        },
        bottomBar = {
            ForgePlanBottomBar(
                selectedItem    = "Projects",
                onProjectsClick = onBack,
                onTimelineClick = onTimelineClick,
                onProgressClick = onProgressClick,
                onTeamClick     = onTeamClick,
                onProfileClick  = onProfileClick
            )
        }
    ) { padding ->
        TaskList(
            padding   = padding,
            tasks     = tasks,
            isLoading = isLoading,
            error     = error,
            onTaskClick = { task ->
                if (task.id in myTaskIds) onMyTaskClick(task.id)
                else                      onOtherTaskClick(task.id)
            },
            onToggleDone = { task ->
                if (task.id in myTaskIds) {
                    if (task.status?.uppercase() == "DONE") {
                        // un-complete: volta para IN_PROGRESS se tinha progresso, senão PENDING
                        val newStatus = if ((task.completion_rate ?: 0) > 0) "IN_PROGRESS" else "PENDING"
                        vm.updateTask(task.copy(status = newStatus))
                    } else {
                        // só conclui se tiver progresso > 0
                        if ((task.completion_rate ?: 0) > 0) {
                            vm.updateTask(task.copy(status = "DONE", completion_rate = 100))
                        }
                    }
                }
            },
            myTaskIds     = myTaskIds,
            dependencyMap = dependencyMap,
            allTasks      = tasks
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
    onTaskClick: (Task) -> Unit,
    onToggleDone: (Task) -> Unit,
    myTaskIds: Set<Long>,
    dependencyMap: Map<Long, List<Long>>,
    allTasks: List<Task>
) {
    var selectedTab    by remember { mutableStateOf(0) }
    var selectedFilter by remember { mutableStateOf(0) }
    var sortType       by remember { mutableStateOf(TaskSortType.DEFAULT) }
    var showSortMenu   by remember { mutableStateOf(false) }

    val activeTasks    = tasks.filter { it.status?.uppercase() != "DONE" }
    val completedTasks = tasks.filter { it.status?.uppercase() == "DONE" }

    Column(Modifier.fillMaxSize().padding(padding)) {

        // ── Tab Ativas / Concluídas ──────────────────────────────
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor   = MaterialTheme.colorScheme.surface,
            contentColor     = MaterialTheme.colorScheme.primary,
            indicator        = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color    = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick  = { selectedTab = 0 },
                text     = {
                    Text(
                        appText("Active (${activeTasks.size})", "Ativas (${activeTasks.size})"),
                        color = if (selectedTab == 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick  = { selectedTab = 1 },
                text     = {
                    Text(
                        appText("Completed (${completedTasks.size})", "Concluídas (${completedTasks.size})"),
                        color = if (selectedTab == 1) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }

        // ── Filtro Todas / Minhas / Equipa ───────────────────────
        val filterLabels = listOf(
            appText("All", "Todas"),
            appText("Mine", "Minhas"),
            appText("Team", "Equipa")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            filterLabels.forEachIndexed { index, label ->
                val isSelected = selectedFilter == index
                FilterChip(
                    selected = isSelected,
                    onClick  = { selectedFilter = index },
                    label    = { Text(label, style = MaterialTheme.typography.labelMedium) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor    = MaterialTheme.colorScheme.primary,
                        selectedLabelColor        = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }

            Spacer(Modifier.weight(1f))

            Box {
                AssistChip(
                    onClick = { showSortMenu = true },
                    label = {
                        Text(
                            when (sortType) {
                                TaskSortType.DEFAULT -> ProjectTasksStrings.sortLabel
                                TaskSortType.DATE -> ProjectTasksStrings.sortByDate
                                TaskSortType.COMPLETION -> ProjectTasksStrings.sortByCompletion
                            },
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(ProjectTasksStrings.sortDefault) },
                        onClick = { sortType = TaskSortType.DEFAULT; showSortMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(ProjectTasksStrings.sortByDate) },
                        onClick = { sortType = TaskSortType.DATE; showSortMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(ProjectTasksStrings.sortByCompletion) },
                        onClick = { sortType = TaskSortType.COMPLETION; showSortMenu = false }
                    )
                }
            }
        }

        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding      = PaddingValues(bottom = 16.dp)
        ) {
            if (isLoading) item { LoadingBox() }
            error?.let { item { ErrorBox(it) } }

            val byTab = if (selectedTab == 0) activeTasks else completedTasks

            val filteredTasks = when (selectedFilter) {
                1    -> byTab.filter { it.id in myTaskIds }
                2    -> byTab.filter { it.id !in myTaskIds }
                else -> byTab
            }

            val displayedTasks = when (sortType) {
                TaskSortType.DEFAULT -> filteredTasks
                TaskSortType.DATE -> filteredTasks.sortedWith(
                    compareBy(nullsLast()) { parseDate(it.start_date) }
                )
                TaskSortType.COMPLETION -> filteredTasks.sortedByDescending {
                    it.completion_rate ?: 0
                }
            }

            if (!isLoading && displayedTasks.isEmpty()) {
                item {
                    Text(
                        text = when {
                            selectedFilter == 1 -> appText("No tasks assigned to you", "Sem tarefas atribuídas a ti")
                            selectedFilter == 2 -> appText("No team tasks", "Sem tarefas da equipa")
                            selectedTab == 0    -> appText("No active tasks", "Sem tarefas ativas")
                            else                -> appText("No completed tasks", "Sem tarefas concluídas")
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            items(displayedTasks) { task ->
                val deps = dependencyMap[task.id] ?: emptyList()
                val blockingTasks = deps
                    .mapNotNull { depId -> allTasks.find { it.id == depId } }
                    .filter { it.status?.uppercase() != "DONE" }
                val blockedByUnfinished = blockingTasks.isNotEmpty()
                TaskCard(
                    task               = task,
                    isMine             = task.id in myTaskIds,
                    onClick            = { onTaskClick(task) },
                    onToggleDone       = { onToggleDone(task) },
                    isBlockedByDep     = blockedByUnfinished && deps.isNotEmpty(),
                    hasDependencies    = deps.isNotEmpty(),
                    blockingTaskNames  = blockingTasks.map { it.title }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// TASK CARD
// ─────────────────────────────────────────────

@Composable
fun TaskCard(
    task: Task,
    isMine: Boolean,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
    isBlockedByDep: Boolean = false,
    hasDependencies: Boolean = false,
    blockingTaskNames: List<String> = emptyList()
) {
    val isDone   = task.status?.uppercase() == "DONE"
    val progress = task.completion_rate ?: 0
    val hasNoProgress = progress == 0 && !isDone

    // Cores bem distintas: verde para concluída, azul para ativa, cinzento para bloqueada
    val containerColor = when {
        isDone         -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        isBlockedByDep -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        !isMine        -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        else           -> MaterialTheme.colorScheme.surface
    }

    val progressColor = when {
        isDone  -> MaterialTheme.colorScheme.tertiary
        progress >= 75 -> MaterialTheme.colorScheme.primary
        progress >= 40 -> MaterialTheme.colorScheme.secondary
        else    -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
    }

    var showBlockedDialog by remember { mutableStateOf(false) }
    var showNoProgressDialog by remember { mutableStateOf(false) }

    if (showBlockedDialog) {
        AlertDialog(
            onDismissRequest = { showBlockedDialog = false },
            title = { Text(appText("Blocked Task", "Tarefa Bloqueada")) },
            text  = {
                Column {
                    Text(appText(
                        "This task is waiting for the following tasks to be completed first:",
                        "Esta tarefa está à espera que as seguintes tarefas sejam concluídas primeiro:"
                    ))
                    if (blockingTaskNames.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        blockingTaskNames.forEach { name ->
                            Text(
                                text = "• $name",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBlockedDialog = false }) { Text("OK") }
            }
        )
    }

    if (showNoProgressDialog) {
        AlertDialog(
            onDismissRequest = { showNoProgressDialog = false },
            title = { Text(appText("No Progress", "Sem Progresso")) },
            text  = { Text(appText(
                "You need to log some progress before marking this task as done.",
                "Tens de registar progresso antes de marcar esta tarefa como concluída."
            )) },
            confirmButton = {
                TextButton(onClick = { showNoProgressDialog = false }) { Text("OK") }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape  = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(Modifier.padding(18.dp)) {

            // ── Título + badges + toggle ──────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text       = task.title,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.weight(1f),
                    color      = if (isDone)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurface
                )

                if (!isMine) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text  = appText("Team", "Equipa"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Badge de dependência
                if (hasDependencies) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isBlockedByDep) MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
                                else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text  = if (isBlockedByDep) appText("⛔ Blocked", "⛔ Bloqueada")
                            else appText("✓ Dep. ok", "✓ Dep. ok"),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isBlockedByDep) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // ── Círculo toggle de concluído (só nas minhas tarefas) ──
                if (isMine) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDone) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                width = 2.dp,
                                color = if (isDone) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .clickable {
                                if (!isDone) {
                                    when {
                                        isBlockedByDep -> showBlockedDialog = true
                                        hasNoProgress  -> showNoProgressDialog = true
                                        else           -> onToggleDone()
                                    }
                                } else {
                                    onToggleDone()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDone) {
                            Icon(
                                imageVector        = Icons.Default.Check,
                                contentDescription = "Done",
                                tint               = MaterialTheme.colorScheme.onPrimary,
                                modifier           = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            if (isBlockedByDep && blockingTaskNames.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = appText("Depends on: ", "Depende de: ") + blockingTaskNames.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1
                )
            }

            Spacer(Modifier.height(6.dp))

            // Status text with color
            val statusText = when (task.status?.uppercase()) {
                "DONE"        -> ProjectTasksStrings.done
                "IN_PROGRESS" -> ProjectTasksStrings.inProgress
                else          -> ProjectTasksStrings.pending
            }
            val statusColor = when (task.status?.uppercase()) {
                "DONE"        -> MaterialTheme.colorScheme.primary
                "IN_PROGRESS" -> MaterialTheme.colorScheme.tertiary
                else          -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            }
            Text(
                text  = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = statusColor,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(ProjectTasksStrings.progress)
                Text("$progress%", fontWeight = FontWeight.Bold, color = progressColor)
            }

            Spacer(Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color     = progressColor,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
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