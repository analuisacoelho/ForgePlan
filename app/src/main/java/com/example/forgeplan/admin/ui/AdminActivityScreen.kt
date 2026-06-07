package com.example.forgeplan.admin.ui

import android.content.res.Configuration
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.admin.viewmodel.AdminViewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.model.ActivityLog

@Composable
fun AdminActivityScreen(
    onBackClick: () -> Unit = {},
    onProjectsClick: () -> Unit = {},
    onUsersClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: AdminViewModel = viewModel()
) {
    val activityLogs by viewModel.activityLogs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(Unit) { viewModel.loadActivityLogs() }

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
                .padding(
                    horizontal = if (isLandscape) 30.dp else 18.dp,
                    vertical = if (isLandscape) 12.dp else 16.dp
                )
        ) {
            Text(
                text = appText(en = "Activity Log", pt = "Historico de Atividades"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                isLoading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                error != null -> Text(text = error!!, color = MaterialTheme.colorScheme.error)
                activityLogs.isEmpty() -> Text(
                    text = appText(en = "No activity yet.", pt = "Sem atividade ainda."),
                    color = MaterialTheme.colorScheme.onBackground
                )
                else -> {
                    // Landscape: 2 colunas - Portrait: 1 coluna
                    if (isLandscape) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(activityLogs.chunked(2)) { rowLogs ->
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
                            items(activityLogs) { log ->
                                ActivityLogCard(log = log)
                            }
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
                // Data formatada da ação
                Text(
                    text = log.created_at ?: "",
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