package com.example.forgeplan.tasks.ui

import android.R.attr.fontWeight
import android.R.attr.text
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.forgeplan.core.model.Comment
import com.example.forgeplan.core.session.SessionManager
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.tasks.viewmodel.TaskPublicDetailViewModel
import com.example.forgeplan.core.model.TaskLog

@Composable
fun TaskOwnerDetailScreen(
    taskId: Long,
    onBack: () -> Unit,
    onAddProgress: (Long) -> Unit,
    onProjectsClick: () -> Unit,
    onTimelineClick: () -> Unit,
    onProgressClick: () -> Unit,
    onTeamClick: () -> Unit,
    onProfileClick: () -> Unit,
    viewModel: TaskPublicDetailViewModel = viewModel()
) {

    val task by viewModel.task.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val userNames by viewModel.userNames.collectAsState()
    val logPhotos by viewModel.logPhotos.collectAsState()

    var commentText by remember { mutableStateOf("") }

    LaunchedEffect(taskId) {
        viewModel.load(taskId)
    }

    Scaffold(
        topBar = {
            ForgePlanTopBar(
                title = "Minha Task",
                initials = SessionManager.userInitials
            )
        },
        bottomBar = {
            ForgePlanBottomBar(
                selectedItem = "Projects",
                onProjectsClick = onProjectsClick,
                onTimelineClick = onTimelineClick,
                onProgressClick = onProgressClick,
                onTeamClick = onTeamClick,
                onProfileClick = onProfileClick
            )
        }
    ) { padding ->

        if (isLoading || task == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val t = task!!

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

            Text(t.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(6.dp))

            Text(
                text = t.description ?: "",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { (t.completion_rate ?: 0) / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
            )

            Text(
                text = "${t.completion_rate ?: 0}% concluído",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(Modifier.height(20.dp))

            // ── Detalhes da tarefa ──────────────────────────────────
            Text(
                text = "Detalhes",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            TaskInfoDetailsCard(task = t)
            Spacer(Modifier.height(20.dp))

            // ── Histórico de Progresso (Dropdown) ──────────────────
            var isLogsExpanded by remember { mutableStateOf(false) }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isLogsExpanded = !isLogsExpanded }
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Histórico de Progresso",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (isLogsExpanded) "▲" else "▼",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    AnimatedVisibility(visible = isLogsExpanded) {
                        Column(
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .heightIn(max = 340.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (logs.isEmpty()) {
                                Text(
                                    text = "Ainda não há registos de progresso.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            } else {
                                logs.forEach {
                                    OwnerLogRow(log = it, photoUrl = logPhotos[it.id])
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Comentários ────────────────────────────────────────
            Text(
                text = "Comentários",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(8.dp))

            if (comments.isEmpty()) {
                Text(
                    text = "Ainda sem comentários. Sê o primeiro!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    comments.forEach { comment ->
                        val userName = userNames[comment.user_id] ?: "Utilizador"
                        CommentRow(comment = comment, userName = userName)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Input comentário ───────────────────────────────────
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Escreve um comentário") },
                    shape = RoundedCornerShape(8.dp),
                    maxLines = 4
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        viewModel.sendComment(taskId, commentText, "✓ enviado", "erro")
                        commentText = ""
                    },
                    enabled = commentText.isNotBlank()
                ) {
                    Text("Enviar")
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { onAddProgress(taskId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Adicionar progresso")
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OwnerLogRow(log: TaskLog, photoUrl: String?) {

    var showPhotoDialog by remember { mutableStateOf(false) }

    if (showPhotoDialog && photoUrl != null) {
        AlertDialog(
            onDismissRequest = { showPhotoDialog = false },
            confirmButton = {
                TextButton(onClick = { showPhotoDialog = false }) { Text("Fechar") }
            },
            text = {
                AsyncImage(
                    model              = photoUrl,
                    contentDescription = "Foto do progresso",
                    modifier           = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale       = ContentScale.FillWidth
                )
            }
        )
    }

    Surface(
        shape    = RoundedCornerShape(8.dp),
        color    = MaterialTheme.colorScheme.surface,
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        "${log.completion_rate ?: 0}%",
                        color      = MaterialTheme.colorScheme.onPrimary,
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    log.log_date ?: "—",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f)
                )
                log.minutes_spent?.let { mins ->
                    val h = mins / 60
                    val m = mins % 60
                    val label = if (h > 0) "${h}h${if (m > 0) " ${m}min" else ""}" else "${m}min"
                    Text(
                        "⏱ $label",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                log.location?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "📍 $it",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            log.notes?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface)
            }

            if (photoUrl != null) {
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    model              = photoUrl,
                    contentDescription = "Foto do progresso",
                    modifier           = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showPhotoDialog = true },
                    contentScale       = ContentScale.Crop
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Toca para expandir",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}