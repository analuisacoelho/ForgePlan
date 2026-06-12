package com.example.forgeplan.admin.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.admin.viewmodel.AdminViewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.model.ActivityLog
import com.example.forgeplan.projects.viewmodel.ProjectViewModel
import com.example.forgeplan.reports.ui.ReportsScreen
import com.example.forgeplan.tasks.viewmodel.UserDashboardViewModel
import com.example.forgeplan.timeline.ui.TimelineBoardWithSummary
import com.example.forgeplan.timeline.ui.TimelineToggle
import com.example.forgeplan.tasks.viewmodel.UserViewModel
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.remember
import com.example.forgeplan.timeline.ui.parseDate

@Composable
fun AdminActivityScreen(
    onProjectsClick: () -> Unit = {},
    onUsersClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    adminViewModel: AdminViewModel = viewModel(),
    dashboardViewModel: UserDashboardViewModel = viewModel(),
    projectViewModel: ProjectViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {
    val activityLogs by adminViewModel.activityLogs.collectAsState()
    val isLoading by adminViewModel.isLoading.collectAsState()
    val error by adminViewModel.error.collectAsState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    var selectedTab by rememberSaveable { mutableStateOf(0) }

    val tabs = listOf(
        appText(en = "Logs", pt = "Logs"),
        appText(en = "Timeline", pt = "Timeline"),
        appText(en = "Reports", pt = "Relatórios")
    )

    LaunchedEffect(Unit) {
        adminViewModel.loadActivityLogs()
        dashboardViewModel.loadDashboard()
    }

    AdminScaffold(
        selectedItem = "Activity",
        onProjectsClick = onProjectsClick,
        onUsersClick = onUsersClick,
        onActivityClick = {},
        onProfileClick = onProfileClick,
        onLogout = onLogout
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = if (isLandscape) 30.dp else 18.dp,
                    vertical = if (isLandscape) 10.dp else 14.dp
                )
            ) {
                Text(
                    text = appText(en = "Activity", pt = "Atividade"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.outline,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index)
                                    MaterialTheme.colorScheme.outline
                                else
                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> LogsTab(
                    logs = activityLogs,
                    isLoading = isLoading,
                    error = error,
                    isLandscape = isLandscape
                )
                1 -> TimelineTab(
                    dashboardViewModel = dashboardViewModel,
                    isLandscape = isLandscape
                )
                2 -> ReportsTab(
                    projectViewModel = projectViewModel,
                    userViewModel = userViewModel
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Tab 0 – Logs
// ─────────────────────────────────────────────────────────

@Composable
private fun LogsTab(
    logs: List<ActivityLog>,
    isLoading: Boolean,
    error: String?,
    isLandscape: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = if (isLandscape) 30.dp else 18.dp,
                vertical = 16.dp
            )
    ) {
        when {
            isLoading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            error != null -> Text(text = error, color = MaterialTheme.colorScheme.error)
            logs.isEmpty() -> Text(
                text = appText(en = "No activity yet.", pt = "Sem atividade ainda."),
                color = MaterialTheme.colorScheme.onBackground
            )
            else -> {
                if (isLandscape) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(logs.chunked(2)) { rowLogs ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowLogs.forEach { log ->
                                    ActivityLogCard(log = log, modifier = Modifier.weight(1f))
                                }
                                if (rowLogs.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(logs) { log ->
                            ActivityLogCard(log = log)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityLogCard(
    log: ActivityLog,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = log.action ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatLogDate(log.created_at),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${log.entity_type ?: ""} - ${log.details ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

private fun formatLogDate(dateString: String?): String {
    if (dateString == null) return ""
    return try {
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        val outputFormat = java.text.SimpleDateFormat("dd/MM/yyyy - HH:mm", java.util.Locale.getDefault())
        val date = inputFormat.parse(dateString.substringBefore("."))
        outputFormat.format(date ?: return dateString)
    } catch (e: Exception) {
        dateString
    }
}

// ─────────────────────────────────────────────────────────
// Tab 1 – Timeline (reutiliza exatamente os composables do TimelineScreen)
// ─────────────────────────────────────────────────────────

@Composable
private fun TimelineTab(
    dashboardViewModel: UserDashboardViewModel,
    isLandscape: Boolean
) {
    val projectsWithTasks by dashboardViewModel.projectsWithTasks.collectAsState()
    val isLoading by dashboardViewModel.isLoading.collectAsState()
    val error by dashboardViewModel.error.collectAsState()

    var selectedMode by rememberSaveable { mutableStateOf("Week") }

    val allTasks = remember(projectsWithTasks) { projectsWithTasks.values.flatten() }
    val sortedTasks = remember(allTasks) {
        allTasks.sortedWith(compareBy(nullsLast()) { parseDate(it.start_date) })
    }
    val projectNameById = remember(projectsWithTasks) {
        projectsWithTasks.keys.associate { it.id to it.name }
    }

    val finishedCount = sortedTasks.count { it.status?.uppercase() == "DONE" }
    val activeCount = sortedTasks.count { it.status?.uppercase() == "IN_PROGRESS" }
    val pendingCount = sortedTasks.size - finishedCount - activeCount

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = if (isLandscape) 28.dp else 14.dp,
                vertical = if (isLandscape) 12.dp else 16.dp
            )
            .padding(bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = appText("Timeline", "Linha Temporal"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            TimelineToggle(
                text = appText("Week", "Semana"),
                selected = selectedMode == "Week",
                onClick = { selectedMode = "Week" }
            )
            Spacer(modifier = Modifier.padding(2.dp))
            TimelineToggle(
                text = appText("Month", "Mês"),
                selected = selectedMode == "Month",
                onClick = { selectedMode = "Month" }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (isLoading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        } else if (sortedTasks.isEmpty()) {
            Text(
                text = appText("No tasks to show.", "Não existem tarefas para apresentar."),
                color = MaterialTheme.colorScheme.onBackground
            )
        } else {
            TimelineBoardWithSummary(
                tasks = sortedTasks,
                mode = selectedMode,
                projectNameById = projectNameById,
                finished = finishedCount,
                active = activeCount,
                pending = pendingCount,
                isLandscape = isLandscape
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
// Tab 2 – Reports (reutiliza exatamente o ReportsScreen existente)
// ─────────────────────────────────────────────────────────

@Composable
private fun ReportsTab(
    projectViewModel: ProjectViewModel,
    userViewModel: UserViewModel
) {
    // O ReportsScreen já tem toda a lógica. Passamos os ViewModels para não recriar estado.
    ReportsScreen(
        showScaffold = false,
        onProjectsClick = {},
        onTimelineClick = {},
        onTeamClick = {},
        projectViewModel = projectViewModel,
        userViewModel = userViewModel
    )
}