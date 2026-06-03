package com.example.forgeplan.timeline.ui

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.session.SessionManager
import com.example.forgeplan.core.ui.components.ForgeCard
import com.example.forgeplan.core.ui.components.ForgeOutlinedCard
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.core.ui.components.ForgeSearchBar
import com.example.forgeplan.tasks.viewmodel.UserDashboardViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Localização: app/src/main/java/com/example/forgeplan/timeline/TimelineScreen.kt
// SUBSTITUI completamente o ficheiro existente.
//
// Alterações principais:
//  1. Usa UserDashboardViewModel — mostra apenas as tarefas do utilizador logado
//  2. As tarefas são ordenadas por start_date (asc); sem data ficam no fim
//  3. O cabeçalho do Gantt mostra as semanas/meses reais com base nas datas
//  4. As barras são posicionadas proporcionalmente ao intervalo start→end
//  5. Modo Week: janela de 7 dias a partir da tarefa mais cedo
//     Modo Month: 5 semanas a partir do início do mês actual
// ─────────────────────────────────────────────────────────────────────────────

private val DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE   // "yyyy-MM-dd"

private fun parseDate(s: String?): LocalDate? =
    s?.takeIf { it.isNotBlank() }?.let {
        try { LocalDate.parse(it, DATE_FMT) } catch (e: Exception) { null }
    }

// Largura de cada coluna de data no Gantt
private val COL_W = 98.dp
private val TASK_COL_W = 120.dp

@Composable
fun TimelineScreen(
    onProjectsClick: () -> Unit = {},
    onProgressClick: () -> Unit = {},
    onTeamClick: () -> Unit = {},
    dashboardViewModel: UserDashboardViewModel = viewModel()
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val projectsWithTasks by dashboardViewModel.projectsWithTasks.collectAsState()
    val isLoading         by dashboardViewModel.isLoading.collectAsState()
    val error             by dashboardViewModel.error.collectAsState()

    // rememberSaveable → sobrevive a rotações
    var searchText   by rememberSaveable { mutableStateOf("") }
    var selectedMode by rememberSaveable { mutableStateOf("Week") }

    LaunchedEffect(Unit) { dashboardViewModel.loadDashboard() }

    // Junta todas as tarefas do utilizador de todos os projectos
    val allTasks: List<Task> = remember(projectsWithTasks) {
        projectsWithTasks.values.flatten()
    }

    // Ordena por start_date (tarefas sem data vão para o fim)
    val sortedTasks: List<Task> = remember(allTasks) {
        allTasks.sortedWith(compareBy(nullsLast()) { parseDate(it.start_date) })
    }

    // Filtra por texto de pesquisa
    val filteredTasks = remember(sortedTasks, searchText) {
        if (searchText.isBlank()) sortedTasks
        else sortedTasks.filter {
            it.title.contains(searchText, ignoreCase = true) ||
                    (it.description ?: "").contains(searchText, ignoreCase = true)
        }
    }

    // Mapa projectId → projectName para mostrar no label
    val projectNameById = remember(projectsWithTasks) {
        projectsWithTasks.keys.associate { it.id to it.name }
    }

    val finishedCount = filteredTasks.count { it.status?.uppercase() == "DONE" }
    val activeCount   = filteredTasks.count { it.status?.uppercase() == "IN_PROGRESS" }
    val pendingCount  = filteredTasks.size - finishedCount - activeCount

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ForgePlanTopBar(title = "ForgePlan", initials = SessionManager.userInitials)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (isLandscape) 28.dp else 14.dp,
                    vertical   = if (isLandscape) 12.dp else 16.dp
                )
                .padding(bottom = 90.dp)
        ) {
            ForgeSearchBar(
                value         = searchText,
                onValueChange = { searchText = it },
                placeholder   = appText("Search your task", "Pesquisar tarefa")
            )

            Spacer(Modifier.height(if (isLandscape) 14.dp else 24.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = appText("Timeline", "Cronologia"),
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onBackground,
                    modifier   = Modifier.weight(1f)
                )
                TimelineToggle(appText("Week", "Semana"), selectedMode == "Week") { selectedMode = "Week" }
                Spacer(Modifier.width(4.dp))
                TimelineToggle(appText("Month", "Mês"),  selectedMode == "Month") { selectedMode = "Month" }
            }

            Spacer(Modifier.height(if (isLandscape) 14.dp else 22.dp))

            // Erros
            error?.let { Text(it, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(8.dp)) }

            if (isLoading) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (filteredTasks.isEmpty()) {
                ForgeCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text  = appText("No tasks to show.", "Não existem tarefas para apresentar."),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                TimelineBoardWithSummary(
                    tasks       = filteredTasks,
                    mode        = selectedMode,
                    projectNameById = projectNameById,
                    finished    = finishedCount,
                    active      = activeCount,
                    pending     = pendingCount,
                    isLandscape = isLandscape
                )
            }
        }

        ForgePlanBottomBar(
            selectedItem    = "Timeline",
            onProjectsClick = onProjectsClick,
            onProgressClick = onProgressClick,
            onTeamClick     = onTeamClick
        )
    }
}

// ── Toggle ────────────────────────────────────────────────────────────────────

@Composable
fun TimelineToggle(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(88.dp).height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondaryContainer)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text  = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

// ── Board + Summary ───────────────────────────────────────────────────────────

@Composable
fun TimelineBoardWithSummary(
    tasks: List<Task>,
    mode: String,
    projectNameById: Map<Long, String>,
    finished: Int,
    active: Int,
    pending: Int,
    isLandscape: Boolean
) {
    if (isLandscape) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.weight(1f).height(315.dp).horizontalScroll(rememberScrollState())) {
                TimelineBoard(tasks, mode, projectNameById, isLandscape = true)
            }
            TimelineSummary(finished, active, pending, modifier = Modifier.width(240.dp))
        }
    } else {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(390.dp).horizontalScroll(rememberScrollState())) {
                TimelineBoard(tasks, mode, projectNameById, isLandscape = false)
            }
            Spacer(Modifier.height(12.dp))
            TimelineSummary(finished, active, pending, modifier = Modifier.fillMaxWidth())
        }
    }
}

// ── Board principal com datas reais ──────────────────────────────────────────

@Composable
fun TimelineBoard(
    tasks: List<Task>,
    mode: String,
    projectNameById: Map<Long, String>,
    isLandscape: Boolean
) {
    val visibleTasks = tasks.take(if (isLandscape) 5 else 6)
    val today        = LocalDate.now()
    val locale       = if (com.example.forgeplan.core.language.AppLanguage.isPortuguese())
        Locale("pt") else Locale("en")

    // ── Calcula as colunas de data ────────────────────────────────────────────
    // "âncora" = data mais cedo nas tarefas visíveis, ou hoje se não houver datas
    val anchor: LocalDate = visibleTasks
        .mapNotNull { parseDate(it.start_date) }
        .minOrNull() ?: today

    val columns: List<Pair<String, LocalDate>> = if (mode == "Week") {
        // 7 dias a partir da âncora (limitamos a 5 para caber no ecrã)
        (0 until 5).map { i ->
            val day = anchor.plusDays(i.toLong())
            val label = buildString {
                append(day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale))
                append("\n")
                append(day.dayOfMonth)
                append(" ")
                append(day.month.getDisplayName(TextStyle.SHORT, locale))
            }
            label to day
        }
    } else {
        // Modo Month: 5 semanas a partir do início do mês da âncora
        val monthStart = anchor.withDayOfMonth(1)
        (0 until 5).map { i ->
            val weekStart = monthStart.plusWeeks(i.toLong())
            val weekEnd   = weekStart.plusDays(6)
            val label = buildString {
                append(appText("Week", "Sem."))
                append(" ${i + 1}\n")
                append(weekStart.dayOfMonth)
                append("–")
                append(weekEnd.dayOfMonth)
                append(" ")
                append(weekStart.month.getDisplayName(TextStyle.SHORT, locale))
            }
            label to weekStart
        }
    }

    val taskColumnWidth = if (isLandscape) 122.dp else TASK_COL_W
    val columnWidth     = if (isLandscape) 108.dp else COL_W
    val boardWidth      = taskColumnWidth + columnWidth * 5
    val boardHeight     = if (isLandscape) 315.dp else 390.dp
    val headerHeight    = 52.dp
    val gridHeight      = boardHeight - headerHeight

    val boardColor     = MaterialTheme.colorScheme.secondaryContainer
    val taskColumnColor= MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
    val gridLineColor  = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.28f)

    ForgeCard(modifier = Modifier.width(boardWidth)) {
        Row(modifier = Modifier.width(boardWidth).height(boardHeight).background(boardColor)) {

            // ── Coluna esquerda: nomes das tarefas ────────────────────────
            Column(
                modifier = Modifier
                    .width(taskColumnWidth).height(boardHeight)
                    .background(taskColumnColor)
                    .padding(horizontal = 10.dp, vertical = 12.dp)
            ) {
                Text(
                    text       = appText("Tasks", "Tarefas"),
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(14.dp))
                visibleTasks.forEach { task ->
                    TimelineTaskLabel(task, projectNameById[task.project_id] ?: "—")
                    Spacer(Modifier.height(if (isLandscape) 8.dp else 12.dp))
                }
            }

            // ── Área de datas + barras ────────────────────────────────────
            Column(modifier = Modifier.width(columnWidth * 5)) {

                // Cabeçalho de datas
                Row {
                    columns.forEachIndexed { i, (label, date) ->
                        val isToday = date == today || (mode == "Month" && today >= date && today < date.plusWeeks(1))
                        Text(
                            text      = label,
                            style     = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            modifier  = Modifier
                                .width(columnWidth).height(headerHeight)
                                .background(if (isToday) MaterialTheme.colorScheme.tertiary else boardColor)
                                .padding(top = 6.dp, start = 4.dp, end = 4.dp),
                            color = if (isToday) MaterialTheme.colorScheme.onTertiary
                            else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // Grelha + barras
                Box(modifier = Modifier.width(columnWidth * 5).height(gridHeight).background(boardColor)) {
                    TimelineGrid(columnWidth * 5, columnWidth, gridHeight, gridLineColor)

                    visibleTasks.forEachIndexed { index, task ->
                        val barData = computeBar(task, columns, mode, columnWidth)
                        val rowTop  = (index * if (isLandscape) 39 else 46).dp + 10.dp

                        TimelineProgressBar(
                            text       = barData.label,
                            startX     = barData.startX,
                            barWidth   = barData.width,
                            top        = rowTop,
                            isDone     = task.status?.uppercase() == "DONE"
                        )
                    }
                }
            }
        }
    }
}

// ── Calcula posição e largura da barra com base nas datas reais ──────────────

private data class BarData(val startX: Dp, val width: Dp, val label: String)

private fun computeBar(
    task: Task,
    columns: List<Pair<String, LocalDate>>,
    mode: String,
    columnWidth: Dp
): BarData {
    val start = parseDate(task.start_date)
    val end = parseDate(task.end_date)

    val colStart = columns.first().second
    val colEnd = columns.last().second.plusDays(if (mode == "Week") 1 else 7)

    val totalWidth = columnWidth * 5

    val progress = task.completion_rate ?: 0
    val status = task.status?.uppercase()

    val labelText = when (status) {
        "DONE" -> appText("100% Finished", "100% Concluída")
        "IN_PROGRESS" -> appText("$progress% Active", "$progress% Ativa")
        else -> appText("$progress% Pending", "$progress% Pendente")
    }

    if (start == null) {
        return BarData(
            startX = 8.dp,
            width = totalWidth * 0.55f,
            label = labelText
        )
    }

    if (start >= colEnd) {
        return BarData(
            startX = totalWidth - 90.dp,
            width = 82.dp,
            label = labelText
        )
    }

    val safeEnd = end ?: start.plusDays(1)

    if (safeEnd <= colStart) {
        return BarData(
            startX = 8.dp,
            width = 82.dp,
            label = labelText
        )
    }

    val clippedStart = maxOf(start, colStart)
    val clippedEnd = minOf(maxOf(safeEnd, clippedStart.plusDays(1)), colEnd)

    val offsetDays = ChronoUnit.DAYS.between(colStart, clippedStart)
        .coerceAtLeast(0)
        .toFloat()

    val durationDays = ChronoUnit.DAYS.between(clippedStart, clippedEnd)
        .coerceAtLeast(1)
        .toFloat()

    val totalDays = ChronoUnit.DAYS.between(colStart, colEnd)
        .coerceAtLeast(1)
        .toFloat()

    val widthPerDay = totalWidth / totalDays

    val startX = (widthPerDay * offsetDays + 6.dp)
        .coerceAtMost(totalWidth - 60.dp)

    val barWidth = (widthPerDay * durationDays)
        .coerceAtLeast(58.dp)
        .coerceAtMost(totalWidth - startX - 6.dp)

    return BarData(
        startX = startX,
        width = barWidth,
        label = labelText
    )
}

// ── Barra de progresso posicionada ───────────────────────────────────────────

@Composable
fun TimelineProgressBar(
    text: String,
    startX: Dp,
    barWidth: Dp,
    top: Dp,
    isDone: Boolean
) {
    val barColor = if (isDone) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .padding(start = startX, top = top)
            .width(barWidth).height(22.dp)
            .clip(RoundedCornerShape(50))
            .background(barColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = text,
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color      = if (isDone) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary,
            maxLines   = 1
        )
    }
}

// ── Grelha de fundo ───────────────────────────────────────────────────────────

@Composable
fun TimelineGrid(gridWidth: Dp, columnWidth: Dp, gridHeight: Dp, gridLineColor: Color) {
    Box(modifier = Modifier.width(gridWidth).height(gridHeight)) {
        Row { repeat(5) { Box(Modifier.width(columnWidth).height(gridHeight)) { Box(Modifier.width(1.dp).height(gridHeight).background(gridLineColor)) } } }
        Column { repeat(7) { Box(Modifier.width(gridWidth).height(1.dp).background(gridLineColor)); Spacer(Modifier.height(45.dp)) } }
    }
}

// ── Label da tarefa ───────────────────────────────────────────────────────────

@Composable
fun TimelineTaskLabel(task: Task, projectName: String) {
    val dateStr = buildString {
        val s = parseDate(task.start_date)
        val e = parseDate(task.end_date)
        if (s != null) {
            append(s.dayOfMonth); append("/"); append(s.monthValue)
            if (e != null && e != s) { append("→"); append(e.dayOfMonth); append("/"); append(e.monthValue) }
        } else {
            append(appText("No date", "Sem data"))
        }
    }

    Column(modifier = Modifier.height(42.dp)) {
        Text(task.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, maxLines = 1)
        Text(dateStr,    style = MaterialTheme.typography.labelSmall,  color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f), maxLines = 1)
    }
}

// ── Resumo ────────────────────────────────────────────────────────────────────

@Composable
fun TimelineSummary(finished: Int, active: Int, pending: Int, modifier: Modifier = Modifier) {
    ForgeOutlinedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
            Text(appText("Summary", "Resumo"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                TimelineSummaryItem(finished.toString(), appText("Finished", "Concluídas"), "finished")
                TimelineDivider()
                TimelineSummaryItem(active.toString(),   appText("Active", "Ativas"),      "active")
                TimelineDivider()
                TimelineSummaryItem(pending.toString(),  appText("Pending", "Pendentes"),  "pending")
            }
        }
    }
}

@Composable
fun TimelineDivider() {
    Box(modifier = Modifier.width(1.dp).height(52.dp).background(MaterialTheme.colorScheme.tertiary))
}

@Composable
fun TimelineSummaryItem(number: String, label: String, variant: String) {
    val numberColor = when (variant) {
        "finished" -> MaterialTheme.colorScheme.secondary
        "active"   -> MaterialTheme.colorScheme.primary
        else       -> MaterialTheme.colorScheme.onSurface
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(number, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = numberColor)
        Text(label,  style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}