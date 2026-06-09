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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.plus
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.model.Comment
import com.example.forgeplan.core.model.TaskLog
import com.example.forgeplan.core.session.SessionManager
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.tasks.viewmodel.TaskPublicDetailViewModel
import com.example.forgeplan.core.model.Task
@Composable
fun TaskPublicDetailScreen(
    taskId: Long,
    onBack: () -> Unit = {},
    onProjectsClick: () -> Unit = {},
    onTimelineClick: () -> Unit = {},
    onProgressClick: () -> Unit = {},
    onTeamClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    viewModel: TaskPublicDetailViewModel = viewModel()
) {
    val task        by viewModel.task.collectAsState()
    val logs        by viewModel.logs.collectAsState()
    val comments    by viewModel.comments.collectAsState()
    val isLoading   by viewModel.isLoading.collectAsState()
    val isSending   by viewModel.isSending.collectAsState()
    val result      by viewModel.commentResult.collectAsState()
    val userNames by viewModel.userNames.collectAsState()


    var commentText by remember { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(taskId) { viewModel.load(taskId) }

    LaunchedEffect(result) {
        result?.let {
            snackbar.showSnackbar(it)
            viewModel.clearResult()
            if (it.startsWith("✓")) commentText = ""
        }
    }

    Scaffold(
        topBar = {
            ForgePlanTopBar(
                title    = appText("Task details", "Detalhes da tarefa"),
                initials = SessionManager.userInitials
            )
        },
        bottomBar = {
            ForgePlanBottomBar(
                selectedItem    = "Projects",
                onProjectsClick = onProjectsClick,
                onTimelineClick = onTimelineClick,
                onProgressClick = onProgressClick,
                onTeamClick     = onTeamClick,
                onProfileClick  = onProfileClick
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            if (isLoading || task == null) {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            val t = task!!

            Text(
                text       = t.title,
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onBackground
            )
            t.description?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
            }

            Spacer(Modifier.height(16.dp))

            LinearProgressIndicator(
                progress   = { (t.completion_rate ?: 0) / 100f },
                modifier   = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color      = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
            )
            Text(
                text  = "${t.completion_rate ?: 0}% ${appText("complete", "concluído")}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(Modifier.height(20.dp))

            // ── Detalhes da tarefa ──────────────────────────────────
                       Text(
                               text       = appText("Task Details", "Detalhes da tarefa"),
                               style      = MaterialTheme.typography.titleSmall,
                               fontWeight = FontWeight.Bold,
                               color      = MaterialTheme.colorScheme.onBackground
                                   )
                       Spacer(Modifier.height(8.dp))

                       TaskInfoDetailsCard(task = t)

                       Spacer(Modifier.height(20.dp))

            // ── Histórico de Progresso (Dropdown) ─────────────────────────────

            var isLogsExpanded by remember { mutableStateOf(false) }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isLogsExpanded = !isLogsExpanded }
                        .padding(12.dp)
                ) {

                    // Header clicável
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Text(
                            text = appText("Progress History", "Histórico de Progresso"),
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

                    // Conteúdo expansível
                    AnimatedVisibility(visible = isLogsExpanded) {

                        Column(
                            modifier = Modifier.padding(top = 12.dp)
                        ) {

                            if (logs.isEmpty()) {
                                Text(
                                    text = appText(
                                        "No progress logs yet.",
                                        "Ainda não há registos de progresso."
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            } else {
                                logs.forEach {
                                    LogRow(it)
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Comentários ───────────────────────────────────────────
            Text(
                text       = appText("Comments", "Comentários"),
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))

            if (comments.isEmpty()) {
                Text(
                    text  = appText("No comments yet. Be the first!", "Ainda sem comentários. Sê o primeiro!"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            } else {
                comments.forEach {

                    val userName =
                        userNames[it.user_id]
                            ?: "Utilizador"

                    CommentRow(
                        comment = it,
                        userName = userName
                    )

                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    value         = commentText,
                    onValueChange = { commentText = it },
                    modifier      = Modifier.weight(1f),
                    placeholder   = { Text(appText("Write a comment…", "Escreve um comentário…")) },
                    shape         = RoundedCornerShape(8.dp),
                    maxLines      = 4
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick  = {
                        viewModel.sendComment(
                            taskId     = t.id,
                            content    = commentText,
                            successMsg = appText("✓ Comment sent.", "✓ Comentário enviado."),
                            errorPrefix= appText("Error", "Erro")
                        )
                    },
                    enabled  = commentText.isNotBlank() && !isSending,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (commentText.isNotBlank()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                        )
                ) {
                    if (isSending) {
                        CircularProgressIndicator(Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.Send,
                            contentDescription = appText("Send", "Enviar"),
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LogRow(log: TaskLog) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Badge de percentagem
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${log.completion_rate ?: 0}%",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Data
                Text(
                    text = log.log_date ?: "—",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f)
                )

                // Tempo gasto
                log.minutes_spent?.let { mins ->

                    val h = mins / 60
                    val m = mins % 60

                    val label = if (h > 0) {
                        "${h}h${if (m > 0) " ${m}min" else ""}"
                    } else {
                        "${m}min"
                    }

                    Text(
                        text = "⏱ $label",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Localização
                log.location?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.width(6.dp))

                    Text(
                        text = "📍 $it",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Notas
            log.notes?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(6.dp))

                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun CommentRow(
    comment: Comment,
    userName: String
) {
    var showUserDialog by remember { mutableStateOf(false) }

    val isOwn = comment.user_id == SessionManager.userId

    val initials = if (isOwn) {
        SessionManager.userInitials
    } else {
        userName
            .split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.toString() }
            .joinToString("")
            .uppercase()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {

        if (!isOwn) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { showUserDialog  = true },   // 👈 AQUI
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials.uppercase(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = if (isOwn) 12.dp else 2.dp,
                topEnd = if (isOwn) 2.dp else 12.dp,
                bottomStart = 12.dp,
                bottomEnd = 12.dp
            ),
            color = if (isOwn)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surface,
            border = if (isOwn) null
            else BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {

                Text(
                    text = comment.content ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isOwn)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurface
                )

                comment.created_at?.let { ts ->
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = ts.take(16).replace("T", " "),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOwn)
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f)
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }

        if (isOwn) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { showUserDialog  = true },   // 👈 AQUI
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials.uppercase(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
    if (showUserDialog) {
        AlertDialog(
            onDismissRequest = {
                showUserDialog = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUserDialog = false
                    }
                ) {
                    Text("OK")
                }
            },
            title = {
                Text("Autor do comentário")
            },
            text = {
                Text(userName)
            }
        )
    }
}

@Composable
private fun AvatarCircle(initials: String) {
    var showUserDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable {
                showUserDialog = true
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TaskInfoDetailsCard(task: Task) {
        Surface(
                shape  = RoundedCornerShape(8.dp),
                color  = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
                    ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        TaskInfoRow("Status",    readableStatus(task.status))
                        TaskInfoRow("Prioridade", task.priority ?: "—")
                        TaskInfoRow("Grupo",      task.task_group ?: "—")
                        TaskInfoRow("Início",     task.start_date ?: "—")
                        TaskInfoRow("Fim",        task.end_date ?: "—")
                    }
            }
    }

@Composable
private fun TaskInfoRow(label: String, value: String) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                Text(value,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                            )
            }
    }

private fun readableStatus(status: String?): String = when (status?.uppercase()) {
        "DONE"        -> "Feita"
        "IN_PROGRESS"  -> "Em progresso"
        "PENDING"      -> "Por fazer"
        else           -> "Por fazer"
    }