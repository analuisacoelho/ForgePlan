package com.example.forgeplan.social.ui

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.model.Comment
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.model.TaskLog
import com.example.forgeplan.core.model.User
import com.example.forgeplan.core.session.SessionManager
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.social.viewmodel.UserPublicViewModel
import com.example.forgeplan.social.viewmodel.UserWithTasks

// ─────────────────────────────────────────────────────────────────────────────
// UserPublicScreen — lista os outros utilizadores e as suas tarefas.
// Ao selecionar uma tarefa, mostra o histórico de progresso (task_logs)
// e permite escrever comentários (→ tabela comments).
// Suporta: PT/EN · light/dark · portrait/landscape
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun UserPublicScreen(
    onProjectsClick: () -> Unit = {},
    onTimelineClick: () -> Unit = {},
    onTeamClick: () -> Unit = {},
    viewModel: UserPublicViewModel = viewModel()
) {
    val configuration = LocalConfiguration.current
    val isLandscape   = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val usersWithTasks    by viewModel.usersWithTasks.collectAsState()
    val selectedTask      by viewModel.selectedTask.collectAsState()
    val taskLogs          by viewModel.taskLogs.collectAsState()
    val comments          by viewModel.comments.collectAsState()
    val isLoading         by viewModel.isLoading.collectAsState()
    val isSendingComment  by viewModel.isSendingComment.collectAsState()
    val commentResult     by viewModel.commentResult.collectAsState()

    var commentText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadUsers() }

    LaunchedEffect(commentResult) {
        commentResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearCommentResult()
            if (it.startsWith("✓")) commentText = ""
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            ForgePlanTopBar(title = "ForgePlan", initials = SessionManager.userInitials)

            if (isLandscape) {
                // ── Landscape: painel esquerdo (lista) + direito (detalhe) ──
                Row(Modifier.weight(1f)) {
                    // Painel esquerdo — lista de utilizadores/tarefas
                    Column(
                        modifier = Modifier
                            .width(300.dp)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        UserListPanel(
                            usersWithTasks = usersWithTasks,
                            selectedTask   = selectedTask,
                            isLoading      = isLoading,
                            onTaskClick    = { viewModel.selectTask(it) }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.fillMaxHeight().width(1.dp))

                    // Painel direito — detalhe da tarefa
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        if (selectedTask != null) {
                            TaskDetailPanel(
                                task             = selectedTask!!,
                                logs             = taskLogs,
                                comments         = comments,
                                commentText      = commentText,
                                onCommentChange  = { commentText = it },
                                isSending        = isSendingComment,
                                onSendComment    = {
                                    viewModel.sendComment(
                                        taskId     = selectedTask!!.id,
                                        content    = commentText,
                                        successMsg = appText("✓ Comment sent.", "✓ Comentário enviado."),
                                        errorPrefix= appText("Error", "Erro")
                                    )
                                }
                            )
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text  = appText("Select a task to see details.", "Seleciona uma tarefa para ver os detalhes."),
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            } else {
                // ── Portrait: coluna única com secções ────────────────────
                LazyColumn(
                    modifier            = Modifier.weight(1f),
                    contentPadding      = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Cabeçalho
                    item {
                        Text(
                            text       = appText("Team Progress", "Progresso da Equipa"),
                            style      = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    if (isLoading) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    } else if (usersWithTasks.isEmpty()) {
                        item {
                            Text(
                                text  = appText("No team members found.", "Nenhum membro da equipa encontrado."),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        // Lista de utilizadores e tarefas
                        usersWithTasks.forEach { userWithTasks ->
                            item {
                                UserCard(
                                    user         = userWithTasks.user,
                                    tasks        = userWithTasks.tasks,
                                    selectedTask = selectedTask,
                                    onTaskClick  = { viewModel.selectTask(it) }
                                )
                                Spacer(Modifier.height(10.dp))
                            }
                        }

                        // Detalhe da tarefa selecionada
                        if (selectedTask != null) {
                            item {
                                Spacer(Modifier.height(8.dp))
                                HorizontalDivider()
                                Spacer(Modifier.height(12.dp))
                                TaskDetailPanel(
                                    task            = selectedTask!!,
                                    logs            = taskLogs,
                                    comments        = comments,
                                    commentText     = commentText,
                                    onCommentChange = { commentText = it },
                                    isSending       = isSendingComment,
                                    onSendComment   = {
                                        viewModel.sendComment(
                                            taskId     = selectedTask!!.id,
                                            content    = commentText,
                                            successMsg = appText("✓ Comment sent.", "✓ Comentário enviado."),
                                            errorPrefix= appText("Error", "Erro")
                                        )
                                    }
                                )
                            }
                        }
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }

            ForgePlanBottomBar(
                selectedItem    = "Team",
                onProjectsClick = onProjectsClick,
                onTimelineClick = onTimelineClick,
                onTeamClick     = onTeamClick
            )
        }
    }
}

// ── Lista de utilizadores (painel esquerdo em landscape) ──────────────────────

@Composable
private fun UserListPanel(
    usersWithTasks: List<UserWithTasks>,
    selectedTask: Task?,
    isLoading: Boolean,
    onTaskClick: (Task) -> Unit
) {
    Text(
        text       = appText("Team Progress", "Progresso da Equipa"),
        style      = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color      = MaterialTheme.colorScheme.onBackground
    )
    Spacer(Modifier.height(14.dp))

    if (isLoading) {
        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        usersWithTasks.forEach { uwt ->
            UserCard(uwt.user, uwt.tasks, selectedTask, onTaskClick)
            Spacer(Modifier.height(10.dp))
        }
    }
}

// ── Card de utilizador com tarefas ────────────────────────────────────────────

@Composable
private fun UserCard(
    user: User,
    tasks: List<Task>,
    selectedTask: Task?,
    onTaskClick: (Task) -> Unit
) {
    Surface(
        shape  = RoundedCornerShape(10.dp),
        color  = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Cabeçalho do utilizador
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserInitialsAvatar(user)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text       = user.name ?: user.username ?: "—",
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text  = user.role ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Lista de tarefas do utilizador
            tasks.forEach { task ->
                val isSelected = task.id == selectedTask?.id
                TaskChip(task = task, isSelected = isSelected, onClick = { onTaskClick(task) })
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun UserInitialsAvatar(user: User) {
    val name     = user.name ?: user.username ?: "?"
    val initials = name.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }

    Box(
        modifier         = Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TaskChip(task: Task, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor    = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background
    val textColor  = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
    val statusColor = when (task.status) {
        "Done", "DONE" -> MaterialTheme.colorScheme.primary
        "IN_PROGRESS"  -> MaterialTheme.colorScheme.secondary
        else           -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
    }
    val statusLabel = when (task.status) {
        "Done", "DONE" -> appText("Done", "Concluída")
        "IN_PROGRESS"  -> appText("In Progress", "Em curso")
        else           -> appText("Pending", "Pendente")
    }

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape    = RoundedCornerShape(6.dp),
        color    = bgColor,
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.bodyMedium, color = textColor, fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(statusColor))
                    Spacer(Modifier.width(4.dp))
                    Text(statusLabel, style = MaterialTheme.typography.labelSmall, color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
            Spacer(Modifier.width(8.dp))
            // Barra de progresso circular
            ProgressRing(progress = task.completion_rate ?: 0, isSelected = isSelected)
        }
    }
}

@Composable
private fun ProgressRing(progress: Int, isSelected: Boolean) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
        CircularProgressIndicator(
            progress          = { progress / 100f },
            modifier          = Modifier.fillMaxSize(),
            strokeWidth       = 3.dp,
            color             = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            trackColor        = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
        )
        Text("${progress}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground)
    }
}

// ── Painel de detalhe da tarefa selecionada ───────────────────────────────────

@Composable
private fun TaskDetailPanel(
    task: Task,
    logs: List<TaskLog>,
    comments: List<Comment>,
    commentText: String,
    onCommentChange: (String) -> Unit,
    isSending: Boolean,
    onSendComment: () -> Unit
) {
    Column {
        // Título da tarefa
        Text(
            text       = task.title,
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onBackground
        )
        task.description?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
        }

        Spacer(Modifier.height(16.dp))

        // Barra de progresso
        LinearProgressIndicator(
            progress    = { (task.completion_rate ?: 0) / 100f },
            modifier    = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color       = MaterialTheme.colorScheme.primary,
            trackColor  = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
        )
        Text(
            text  = "${task.completion_rate ?: 0}% ${appText("complete", "concluído")}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(Modifier.height(20.dp))

        // ── Histórico de progresso (task_logs) ────────────────────────────
        Text(
            text       = appText("Progress History", "Histórico de Progresso"),
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))

        if (logs.isEmpty()) {
            Text(
                text  = appText("No progress logs yet.", "Ainda não há registos de progresso."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        } else {
            logs.forEach { log -> TaskLogItem(log); Spacer(Modifier.height(8.dp)) }
        }

        Spacer(Modifier.height(20.dp))

        // ── Comentários ────────────────────────────────────────────────────
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
            comments.forEach { comment -> CommentItem(comment); Spacer(Modifier.height(8.dp)) }
        }

        Spacer(Modifier.height(14.dp))

        // ── Campo de novo comentário ───────────────────────────────────────
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value         = commentText,
                onValueChange = onCommentChange,
                modifier      = Modifier.weight(1f),
                placeholder   = { Text(appText("Write a comment…", "Escreve um comentário…")) },
                shape         = RoundedCornerShape(8.dp),
                maxLines      = 4
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick  = onSendComment,
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
                    CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.Send, contentDescription = appText("Send", "Enviar"), tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ── Item de log de progresso ──────────────────────────────────────────────────

@Composable
private fun TaskLogItem(log: TaskLog) {
    Surface(
        shape  = RoundedCornerShape(8.dp),
        color  = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier         = Modifier.clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primary).padding(horizontal = 8.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${log.completion_rate ?: 0}%", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                Text(log.log_date ?: "—", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), modifier = Modifier.weight(1f))
                log.location?.takeIf { it.isNotBlank() }?.let {
                    Text("📍 $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
            log.minutes_spent?.takeIf { it > 0 }?.let { mins ->
                Spacer(Modifier.height(4.dp))
                val h = mins / 60; val m = mins % 60
                val timeStr = when {
                    h > 0 && m > 0 -> appText("${h}h ${m}m", "${h}h ${m}m")
                    h > 0           -> appText("${h}h", "${h}h")
                    else            -> appText("${m}m", "${m}m")
                }
                Text(
                    text  = "⏱ $timeStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
            log.notes?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

// ── Item de comentário ────────────────────────────────────────────────────────

@Composable
private fun CommentItem(comment: Comment) {
    val isOwn = comment.user_id == SessionManager.userId
    Row(
        modifier          = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape    = RoundedCornerShape(
                topStart    = if (isOwn) 12.dp else 2.dp,
                topEnd      = if (isOwn) 2.dp else 12.dp,
                bottomStart = 12.dp,
                bottomEnd   = 12.dp
            ),
            color    = if (isOwn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            border   = if (isOwn) null else BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text  = comment.content ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isOwn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
                comment.created_at?.let { ts ->
                    val displayTime = ts.take(16).replace("T", " ")
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text  = displayTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOwn) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// ── Divisor horizontal auxiliar ──────────────────────────────────────────────

@Composable
private fun HorizontalDivider(modifier: Modifier = Modifier) {
    Divider(modifier = modifier, color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f))
}