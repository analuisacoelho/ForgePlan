package com.example.forgeplan.tasks.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.model.Comment
import com.example.forgeplan.core.session.SessionManager
import com.example.forgeplan.core.ui.components.ForgePlanBottomBar
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.tasks.viewmodel.TaskPublicDetailViewModel

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

    // 👇 IMPORTANTE: map user_id -> name
    val userNames by viewModel.userNames.collectAsState()

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
                .padding(16.dp)
        ) {

            Text(t.title, style = MaterialTheme.typography.titleLarge)

            Spacer(Modifier.height(6.dp))

            Text(
                text = t.description ?: "",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { (t.completion_rate ?: 0) / 100f },
                modifier = Modifier.fillMaxWidth()
            )

            Text("${t.completion_rate ?: 0}% concluído")

            Spacer(Modifier.height(20.dp))

            // ───────── LOGS ─────────
            Text("Logs", fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(8.dp))

            logs.forEach { log ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("📅 ${log.log_date}")
                        Text("Progresso: ${log.completion_rate}%")

                        log.notes?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(it)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ───────── COMMENTS ─────────
            Text("Comentários", fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                items(comments) { comment ->

                    val userName =
                        userNames[comment.user_id] ?: "Utilizador"

                    CommentRow(
                        comment = comment,
                        userName = userName
                    )

                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // ───────── INPUT ─────────
            Row(Modifier.fillMaxWidth()) {

                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Escreve um comentário") }
                )

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = {
                        viewModel.sendComment(
                            taskId,
                            commentText,
                            "✓ enviado",
                            "erro"
                        )
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
        }
    }
}