package com.example.forgeplan.tasks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
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
import android.content.res.Configuration
import com.example.forgeplan.core.language.AppLanguage
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.session.SessionManager
import com.example.forgeplan.core.ui.components.*
import com.example.forgeplan.tasks.viewmodel.UserDashboardViewModel

// ── Textos bilingues ─────────────────────────────────────────────────────────

private object Strings {
    val searchPlaceholder   get() = appText("Search your task",               "Pesquisar tarefa")
    val yourTasks           get() = appText("Your Tasks",                     "As Suas Tarefas")
    val subtitle            get() = appText(
        "Check your tasks, the projects they belong to,\nthe materials needed and their descriptions.",
        "Consulte as suas tarefas, os projectos a que pertencem,\nos materiais necessários e as suas descrições."
    )
    val filterLabel         get() = appText("Filter",                         "Filtrar")
    val toDo                get() = appText("To do",                          "Por fazer")
    val allDone             get() = appText("All tasks completed ✓",          "Todas as tarefas concluídas ✓")
    val noProjects          get() = appText("No projects assigned yet.",       "Ainda não tens projectos atribuídos.")
    val markAsDone          get() = appText("Mark as done",                   "Marcar como feita")
    val done                get() = appText("Done",                           "Feita")
    val high                get() = appText("High",                           "Alta")
    val medium              get() = appText("Medium",                         "Média")
    val low                 get() = appText("Low",                            "Baixa")

    fun priorityLabel(p: String?): String = when (p?.lowercase()) {
        "high"   -> high
        "medium" -> medium
        "low"    -> low
        else     -> p?.replaceFirstChar { it.uppercase() } ?: "—"
    }
}

// ── Cores de prioridade (funcionam em light e dark) ──────────────────────────

private fun priorityBgColor(priority: String?): Color = when (priority?.lowercase()) {
    "high"   -> Color(0xFFEF9A9A)
    "medium" -> Color(0xFFFFCC80)
    "low"    -> Color(0xFFA5D6A7)
    else     -> Color(0xFF9E9E9E)
}

private fun priorityChipColor(priority: String?): Color = when (priority?.lowercase()) {
    "high"   -> Color(0xFFE53935)
    "medium" -> Color(0xFFFB8C00)
    "low"    -> Color(0xFF43A047)
    else     -> Color(0xFF616161)
}

private val CardDoneColor = Color(0xFF9E9E9E)

// ── Ecrã principal ───────────────────────────────────────────────────────────

@Composable
fun UserDashboardScreen(
    onTimelineClick: () -> Unit = {},
    onProgressClick: () -> Unit = {},
    onTeamClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val vm: UserDashboardViewModel = viewModel()

    val projectsWithTasks by vm.projectsWithTasks.collectAsState()
    val isLoading         by vm.isLoading.collectAsState()
    val error             by vm.error.collectAsState()

    // rememberSaveable mantém o texto de pesquisa em rotações de ecrã
    var searchText by rememberSaveable { mutableStateOf("") }

    // Detecta orientação para ajustar o layout
    val configuration  = LocalConfiguration.current
    val isLandscape    = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Carrega dados ao entrar
    LaunchedEffect(Unit) { vm.loadDashboard() }

    // Filtra projectos pelo texto de pesquisa (nome do projecto ou título da tarefa)
    val filteredProjects = remember(projectsWithTasks, searchText) {
        if (searchText.isBlank()) projectsWithTasks
        else projectsWithTasks.filter { (project, tasks) ->
            project.name.contains(searchText, ignoreCase = true) ||
                    tasks.any { it.title.contains(searchText, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            ForgePlanTopBar(
                title    = "ForgePlan",
                initials = SessionManager.userInitials
            )
        },
        bottomBar = {
            ForgePlanBottomBar(
                selectedItem    = "Tasks",
                onTimelineClick = onTimelineClick,
                onProgressClick = onProgressClick,
                onTeamClick     = onTeamClick,
                onProfileClick  = onProfileClick
            )
        },
        // containerColor herda do MaterialTheme — funciona automaticamente
        // em modo claro e escuro sem código extra
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        if (isLandscape) {
            // ── Layout horizontal: coluna esquerda fixa + lista de cards ──
            LandscapeLayout(
                paddingValues      = paddingValues,
                searchText         = searchText,
                onSearchChange     = { searchText = it },
                filteredProjects   = filteredProjects,
                isLoading          = isLoading,
                error              = error,
                onMarkAsDone       = { task -> vm.markTaskAsDone(task) }
            )
        } else {
            // ── Layout vertical: LazyColumn normal ────────────────────────
            PortraitLayout(
                paddingValues      = paddingValues,
                searchText         = searchText,
                onSearchChange     = { searchText = it },
                filteredProjects   = filteredProjects,
                isLoading          = isLoading,
                error              = error,
                onMarkAsDone       = { task -> vm.markTaskAsDone(task) }
            )
        }
    }
}

// ── Layout vertical (portrait) ───────────────────────────────────────────────

@Composable
private fun PortraitLayout(
    paddingValues: PaddingValues,
    searchText: String,
    onSearchChange: (String) -> Unit,
    filteredProjects: Map<Project, List<Task>>,
    isLoading: Boolean,
    error: String?,
    onMarkAsDone: (Task) -> Unit
) {
    LazyColumn(
        modifier              = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp),
        verticalArrangement   = Arrangement.spacedBy(16.dp)
    ) {
        item { DashboardHeader(searchText, onSearchChange) }
        item { MembersFilterRow() }

        if (isLoading) item { LoadingBox() }
        error?.let { msg -> item { ErrorCard(msg) } }

        if (!isLoading && error == null && filteredProjects.isEmpty()) {
            item { EmptyBox() }
        }

        items(filteredProjects.entries.toList()) { (project, tasks) ->
            ProjectTaskCard(
                project      = project,
                tasks        = tasks,
                // portrait → 2 colunas
                columns      = 2,
                onMarkAsDone = onMarkAsDone
            )
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ── Layout horizontal (landscape) ────────────────────────────────────────────

@Composable
private fun LandscapeLayout(
    paddingValues: PaddingValues,
    searchText: String,
    onSearchChange: (String) -> Unit,
    filteredProjects: Map<Project, List<Task>>,
    isLoading: Boolean,
    error: String?,
    onMarkAsDone: (Task) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // Coluna esquerda: header + filtros (30% da largura)
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.32f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardHeader(searchText, onSearchChange)
            MembersFilterRow()
        }

        Divider(
            modifier  = Modifier
                .fillMaxHeight()
                .width(1.dp),
            color     = MaterialTheme.colorScheme.outlineVariant
        )

        // Coluna direita: cards (70% da largura)
        LazyColumn(
            modifier              = Modifier
                .fillMaxHeight()
                .weight(0.68f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement   = Arrangement.spacedBy(16.dp)
        ) {
            if (isLoading) item { LoadingBox() }
            error?.let { msg -> item { ErrorCard(msg) } }

            if (!isLoading && error == null && filteredProjects.isEmpty()) {
                item { EmptyBox() }
            }

            items(filteredProjects.entries.toList()) { (project, tasks) ->
                ProjectTaskCard(
                    project      = project,
                    tasks        = tasks,
                    // landscape → 3 colunas (mais espaço horizontal)
                    columns      = 3,
                    onMarkAsDone = onMarkAsDone
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ── Blocos partilhados ───────────────────────────────────────────────────────

@Composable
private fun DashboardHeader(searchText: String, onSearchChange: (String) -> Unit) {
    Spacer(Modifier.height(4.dp))
    ForgeSearchBar(
        value         = searchText,
        onValueChange = onSearchChange,
        placeholder   = Strings.searchPlaceholder
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text       = Strings.yourTasks,
        style      = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color      = MaterialTheme.colorScheme.onBackground
    )
    Text(
        text       = Strings.subtitle,
        style      = MaterialTheme.typography.bodySmall,
        color      = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        lineHeight = 18.sp
    )
}

@Composable
private fun MembersFilterRow() {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
            listOf("UC", "AL", "NI", "+5").forEachIndexed { i, initials ->
                UserAvatarChip(
                    initials = initials,
                    modifier = Modifier.offset(x = (i * (-6)).dp)
                )
            }
        }
        ForgeDropdownCard(text = Strings.filterLabel)
    }
}

@Composable
private fun LoadingBox() {
    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            text     = message,
            modifier = Modifier.padding(12.dp),
            color    = MaterialTheme.colorScheme.onErrorContainer,
            style    = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun EmptyBox() {
    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text  = Strings.noProjects,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
        )
    }
}

// ── Card de projecto ─────────────────────────────────────────────────────────

@Composable
fun ProjectTaskCard(
    project: Project,
    tasks: List<Task>,
    columns: Int = 2,
    onMarkAsDone: (Task) -> Unit
) {
    val todoTasks = tasks.filter { it.status?.lowercase() != "done" }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(
            // surfaceVariant adapta-se automaticamente a dark/light
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            Text(
                text       = project.name,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(6.dp))

            // Barra "To do" + contador
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
                    text       = Strings.toDo,
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                ) {
                    Text(
                        text       = todoTasks.size.toString().padStart(2, '0'),
                        style      = MaterialTheme.typography.labelSmall,
                        modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (todoTasks.isNotEmpty()) {
                TaskGrid(tasks = todoTasks, columns = columns, onMarkAsDone = onMarkAsDone)
            } else {
                Text(
                    text     = Strings.allDone,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(4.dp))
            Icon(
                imageVector        = Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                modifier           = Modifier.size(20.dp),
                tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

// ── Grelha adaptável de tarefas ───────────────────────────────────────────────

@Composable
fun TaskGrid(
    tasks: List<Task>,
    columns: Int = 2,
    onMarkAsDone: (Task) -> Unit
) {
    val rows = tasks.chunked(columns)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { task ->
                    TaskMiniCard(
                        task         = task,
                        modifier     = Modifier.weight(1f),
                        onMarkAsDone = { onMarkAsDone(task) }
                    )
                }
                // Preenche espaços vazios na última linha
                repeat(columns - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// ── Mini-card de tarefa ───────────────────────────────────────────────────────

@Composable
fun TaskMiniCard(
    task: Task,
    modifier: Modifier = Modifier,
    onMarkAsDone: () -> Unit
) {
    val isDone  = task.status?.lowercase() == "done"
    val bgColor = if (isDone) CardDoneColor else priorityBgColor(task.priority)

    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(8.dp),
        colors    = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Chip de prioridade com texto traduzido
                Surface(
                    shape = RoundedCornerShape(50),
                    color = priorityChipColor(task.priority)
                ) {
                    Text(
                        text       = Strings.priorityLabel(task.priority),
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color      = Color.White
                    )
                }
                Icon(
                    imageVector        = Icons.Default.MoreVert,
                    contentDescription = appText("Options", "Opções"),
                    modifier           = Modifier.size(16.dp),
                    tint               = Color.White.copy(alpha = 0.85f)
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text       = task.title,
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color      = Color.White,
                maxLines   = 2
            )

            Spacer(Modifier.height(10.dp))

            // Botão / estado "Done"
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                if (isDone) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text       = Strings.done,
                            style      = MaterialTheme.typography.labelSmall,
                            color      = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector        = Icons.Outlined.CheckCircle,
                            contentDescription = Strings.done,
                            tint               = Color.White,
                            modifier           = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Row(
                        modifier              = Modifier.clickable { onMarkAsDone() },
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text       = Strings.markAsDone,
                            style      = MaterialTheme.typography.labelSmall,
                            color      = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector        = Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = Strings.markAsDone,
                            tint               = Color.White,
                            modifier           = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}