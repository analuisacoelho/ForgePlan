package com.example.forgeplan.tasks.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.session.SessionManager
import com.example.forgeplan.core.ui.components.*
import com.example.forgeplan.notifications.viewmodel.NotificationViewModel
import com.example.forgeplan.tasks.viewmodel.UserDashboardViewModel

// ── TEXTOS ───────────────────────────────────────────────────────────────

private object Strings {
    val searchPlaceholder get() = appText("Search project", "Pesquisar projecto")
    val yourProjects      get() = appText("Your Projects", "Os Seus Projectos")
    val subtitle          get() = appText("Tap a project to open.", "Toque num projecto para abrir.")
    val filterLabel       get() = appText("Filter", "Filtrar")
    val filterByDate      get() = appText("By Date", "Por Data")
    val filterByRate      get() = appText("By Completion", "Por Conclusão")
    val allFilters        get() = appText("All", "Todos")

    val noProjects        get() = appText("No projects assigned yet.", "Ainda não tens projectos atribuídos.")
}

// ── FILTER TYPE ──────────────────────────────────────────────────────────

private enum class FilterType { ALL, DATE, COMPLETION }

// ── SCREEN ──────────────────────────────────────────────────────────────

@Composable
fun UserDashboardScreen(
    onTimelineClick: () -> Unit = {},
    onProgressClick: () -> Unit = {},
    onTeamClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onProjectClick: (Long) -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {
    val vm: UserDashboardViewModel = viewModel()
    val notifVm: NotificationViewModel = viewModel()

    // Carregar contagem de não lidas ao entrar no ecrã
    LaunchedEffect(Unit) {
        vm.loadDashboard()
        notifVm.load()
    }

    val unreadCount by notifVm.unreadCount.collectAsState()
    val projectsWithTasks by vm.projectsWithTasks.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()

    var searchText by rememberSaveable { mutableStateOf("") }
    var filterType by rememberSaveable { mutableStateOf(FilterType.ALL) }
    var showFilterMenu by remember { mutableStateOf(false) }

    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(Unit) { vm.loadDashboard() }

    val displayProjects = remember(projectsWithTasks, searchText, filterType) {
        var map =
            if (searchText.isBlank()) projectsWithTasks
            else projectsWithTasks.filter { (p, tasks) ->
                p.name.contains(searchText, ignoreCase = true) ||
                        tasks.any { it.title.contains(searchText, ignoreCase = true) }
            }

        when (filterType) {
            FilterType.DATE ->
                map.entries.sortedBy { it.key.start_date ?: "" }
                    .associate { it.key to it.value }

            FilterType.COMPLETION ->
                map.entries.sortedByDescending { (_, tasks) ->
                    val done = tasks.count { it.status?.uppercase() == "DONE" }
                    if (tasks.isEmpty()) 0 else done * 100 / tasks.size
                }.associate { it.key to it.value }

            FilterType.ALL -> map
        }
    }

    Scaffold(
        topBar = {
            ForgePlanTopBar(
                title = "ForgePlan",
                initials = SessionManager.userInitials,
                onNotificationClick = onNotificationClick,
                unreadCount = unreadCount,
                onAvatarClick = onProfileClick
            )
        },
        bottomBar = {
            ForgePlanBottomBar(
                selectedItem = "Projects",
                onProjectsClick = {},
                onTimelineClick = onTimelineClick,
                onProgressClick = onProgressClick,
                onTeamClick = onTeamClick,
                onProfileClick = onProfileClick
            )
        }
    ) { padding ->

        if (isLandscape) {
            LandscapeLayout(
                padding = padding,
                searchText = searchText,
                onSearchChange = { searchText = it },
                filterType = filterType,
                showFilterMenu = showFilterMenu,
                onToggleFilter = { showFilterMenu = !showFilterMenu },
                onDismiss = { showFilterMenu = false },
                onSelect = { filterType = it; showFilterMenu = false },
                displayProjects = displayProjects,
                isLoading = isLoading,
                error = error,
                onProjectClick = onProjectClick
            )
        } else {
            PortraitLayout(
                padding = padding,
                searchText = searchText,
                onSearchChange = { searchText = it },
                filterType = filterType,
                showFilterMenu = showFilterMenu,
                onToggleFilter = { showFilterMenu = !showFilterMenu },
                onDismiss = { showFilterMenu = false },
                onSelect = { filterType = it; showFilterMenu = false },
                displayProjects = displayProjects,
                isLoading = isLoading,
                error = error,
                onProjectClick = onProjectClick
            )
        }
    }
}

// ── PORTRAIT ─────────────────────────────────────────────────────────────

@Composable
private fun PortraitLayout(
    padding: PaddingValues,
    searchText: String,
    onSearchChange: (String) -> Unit,
    filterType: FilterType,
    showFilterMenu: Boolean,
    onToggleFilter: () -> Unit,
    onDismiss: () -> Unit,
    onSelect: (FilterType) -> Unit,
    displayProjects: Map<Project, List<Task>>,
    isLoading: Boolean,
    error: String?,
    onProjectClick: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            DashboardHeader(
                searchText,
                onSearchChange,
                filterType,
                showFilterMenu,
                onToggleFilter,
                onDismiss,
                onSelect
            )
        }

        if (isLoading) item { LoadingBox() }
        error?.let { item { ErrorCard(it) } }
        if (!isLoading && error == null && displayProjects.isEmpty())
            item { EmptyBox() }

        items(displayProjects.entries.toList()) { (project, tasks) ->
            ProjectCard(
                project = project,
                tasks = tasks,
                onClick = { onProjectClick(project.id) }
            )
        }
    }
}

// ── LANDSCAPE ───────────────────────────────────────────────────────────

@Composable
private fun LandscapeLayout(
    padding: PaddingValues,
    searchText: String,
    onSearchChange: (String) -> Unit,
    filterType: FilterType,
    showFilterMenu: Boolean,
    onToggleFilter: () -> Unit,
    onDismiss: () -> Unit,
    onSelect: (FilterType) -> Unit,
    displayProjects: Map<Project, List<Task>>,
    isLoading: Boolean,
    error: String?,
    onProjectClick: (Long) -> Unit
) {
    Row(
        Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        Column(
            Modifier
                .weight(0.35f)
                .padding(16.dp)
        ) {
            DashboardHeader(
                searchText,
                onSearchChange,
                filterType,
                showFilterMenu,
                onToggleFilter,
                onDismiss,
                onSelect
            )
        }

        HorizontalDivider(Modifier.width(1.dp))

        LazyColumn(
            Modifier
                .weight(0.65f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            if (isLoading) item { LoadingBox() }
            error?.let { item { ErrorCard(it) } }
            if (!isLoading && error == null && displayProjects.isEmpty())
                item { EmptyBox() }

            items(displayProjects.entries.toList()) { (project, tasks) ->
                ProjectCard(
                    project = project,
                    tasks = tasks,
                    onClick = { onProjectClick(project.id) }
                )
            }
        }
    }
}

// ── HEADER ──────────────────────────────────────────────────────────────

@Composable
private fun DashboardHeader(
    searchText: String,
    onSearchChange: (String) -> Unit,
    filterType: FilterType,
    showMenu: Boolean,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
    onSelect: (FilterType) -> Unit
) {
    ForgeSearchBar(
        value = searchText,
        onValueChange = onSearchChange,
        placeholder = Strings.searchPlaceholder
    )

    Spacer(Modifier.height(12.dp))

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(Strings.yourProjects, fontWeight = FontWeight.Bold)
            Text(Strings.subtitle, style = MaterialTheme.typography.bodySmall)
        }

        Box {
            Surface(
                modifier = Modifier.clickable { onToggle() },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    when (filterType) {
                        FilterType.ALL -> Strings.filterLabel
                        FilterType.DATE -> Strings.filterByDate
                        FilterType.COMPLETION -> Strings.filterByRate
                    },
                    modifier = Modifier.padding(8.dp)
                )
            }

            DropdownMenu(showMenu, onDismiss) {
                listOf(
                    FilterType.ALL to Strings.allFilters,
                    FilterType.DATE to Strings.filterByDate,
                    FilterType.COMPLETION to Strings.filterByRate
                ).forEach { (type, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = { onSelect(type) }
                    )
                }
            }
        }
    }
}

// ── PROJECT CARD ──────────────────────────────────────────

@Composable
fun ProjectCard(
    project: Project,
    tasks: List<Task>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val completed = tasks.count { it.status?.uppercase() == "DONE" }
    val pending   = tasks.count { it.status?.uppercase() != "DONE" }
    val total     = tasks.size
    val progress  = if (total == 0) 0 else completed * 100 / total
    val isCompleted = progress >= 100

    val progressColor = when {
        isCompleted   -> MaterialTheme.colorScheme.primary
        progress >= 50 -> MaterialTheme.colorScheme.tertiary
        else           -> MaterialTheme.colorScheme.error.copy(alpha = 0.75f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape  = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(18.dp)) {

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        project.name,
                        fontWeight = FontWeight.Bold,
                        style      = MaterialTheme.typography.titleLarge,
                        color      = MaterialTheme.colorScheme.onSurface,
                        maxLines   = 1
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        project.description ?: appText("No description", "Sem descrição"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        maxLines = 2
                    )
                }
                Spacer(Modifier.width(8.dp))
                // Priority/completion badge
                val chipText = when {
                    isCompleted -> appText("Done ✓", "Concluído ✓")
                    project.priority?.uppercase() == "HIGH" -> appText("Urgent", "Urgente")
                    project.priority?.uppercase() == "MEDIUM" -> appText("Medium", "Média")
                    else -> appText("Active", "Ativa")
                }
                val chipBg = when {
                    isCompleted -> androidx.compose.ui.graphics.Color(0xFFB7EBC0)
                    project.priority?.uppercase() == "HIGH" -> MaterialTheme.colorScheme.error
                    project.priority?.uppercase() == "MEDIUM" -> androidx.compose.ui.graphics.Color(0xFFFFF3CD)
                    else -> MaterialTheme.colorScheme.secondaryContainer
                }
                val chipFg = when {
                    isCompleted -> androidx.compose.ui.graphics.Color(0xFF14532D)
                    project.priority?.uppercase() == "HIGH" -> MaterialTheme.colorScheme.onError
                    project.priority?.uppercase() == "MEDIUM" -> androidx.compose.ui.graphics.Color(0xFF7B5200)
                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = chipBg
                ) {
                    Text(
                        chipText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style    = MaterialTheme.typography.labelSmall,
                        color    = chipFg,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Task summary chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        appText("$completed done", "$completed feitas"),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        appText("$pending pending", "$pending pendentes"),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    appText("Progress", "Progresso"),
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "$progress%",
                    style      = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color      = progressColor
                )
            }

            Spacer(Modifier.height(6.dp))

            LinearProgressIndicator(
                progress  = { progress / 100f },
                modifier  = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                color      = progressColor,
                trackColor = MaterialTheme.colorScheme.secondaryContainer
            )
        }
    }
}

// ── UTIL ────────────────────────────────────────────────────────────────

@Composable private fun LoadingBox() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable private fun ErrorCard(message: String) {
    Text(message, color = MaterialTheme.colorScheme.error)
}

@Composable private fun EmptyBox() {
    Text(Strings.noProjects)
}