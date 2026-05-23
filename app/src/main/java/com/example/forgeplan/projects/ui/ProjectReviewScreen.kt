package com.example.forgeplan.projects.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.model.ProjectEvaluationPayload
import com.example.forgeplan.projects.viewmodel.ProjectEvaluationViewModel
import com.example.forgeplan.projects.viewmodel.ProjectViewModel
import com.example.forgeplan.tasks.viewmodel.TaskViewModel

@Composable
fun ProjectReviewScreen(
    projectId: Long,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    projectViewModel: ProjectViewModel = viewModel(),
    taskViewModel: TaskViewModel = viewModel(),
    evaluationViewModel: ProjectEvaluationViewModel = viewModel()
) {
    val project by projectViewModel.selectedProject.collectAsState()
    val isLoading by projectViewModel.isLoading.collectAsState()
    val error by projectViewModel.error.collectAsState()
    val tasks by taskViewModel.tasks.collectAsState()
    val evaluationError by evaluationViewModel.error.collectAsState()

    var reviewText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(projectId) {
        projectViewModel.loadProjectById(projectId)
        taskViewModel.loadTasks(projectId)
        evaluationViewModel.loadEvaluations(projectId)
    }

    val averageProgress =
        if (tasks.isEmpty()) 0
        else tasks.map { it.completion_rate ?: 0 }.average().toInt()

    val completedTasks = tasks.count { it.status?.uppercase() == "DONE" }

    val completionRate =
        if (tasks.isEmpty()) 0
        else ((completedTasks.toFloat() / tasks.size.toFloat()) * 100).toInt()

    val deadlineAdherence =
        when {
            tasks.isEmpty() -> 0
            project?.status?.uppercase() == "DONE" -> 100
            else -> averageProgress.coerceIn(0, 100)
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ProjectReviewTopBar(
            title = "ForgePlan",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 22.dp)
        ) {
            when {
                isLoading && project == null -> CircularProgressIndicator()

                error != null -> {
                    Text(
                        text = error ?: "Erro desconhecido",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                project == null -> {
                    Text(
                        text = "Projeto não encontrado.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                else -> {
                    Text(
                        text = "Project Review",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = project!!.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    ReviewProgressMainCard(
                        title = "Progress",
                        progress = averageProgress
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    ReviewProgressSmallCard(
                        title = "Completion Rate",
                        progress = completionRate
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    ReviewProgressSmallCard(
                        title = "Deadline Adherence",
                        progress = deadlineAdherence
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    AddReviewCard(
                        value = reviewText,
                        onValueChange = {
                            reviewText = it
                            message = null
                        }
                    )

                    message?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    evaluationError?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        OutlinedButton(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            onClick = onBackClick,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = "Cancel",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Button(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            onClick = {
                                evaluationViewModel.createEvaluation(
                                    evaluation = ProjectEvaluationPayload(
                                        project_id = projectId,
                                        rating = 5,
                                        comment = reviewText.trim().ifBlank { null }
                                    ),
                                    onSuccess = {
                                        evaluationViewModel.loadEvaluations(projectId)
                                        message = "Review guardada com sucesso."
                                        reviewText = ""
                                    }
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                text = "▧",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(modifier = Modifier.size(8.dp))

                            Text(
                                text = "Save changes",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectReviewTopBar(
    title: String,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .background(MaterialTheme.colorScheme.tertiary)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = "Voltar",
            tint = MaterialTheme.colorScheme.onTertiary,
            modifier = Modifier
                .size(32.dp)
                .clickable { onBackClick() }
        )

        Spacer(modifier = Modifier.size(14.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiary,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiary,
            modifier = Modifier.size(34.dp)
        )
    }
}

@Composable
fun ReviewProgressMainCard(
    title: String,
    progress: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "◷",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )

                Spacer(modifier = Modifier.size(10.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f)
                )

                ProgressBadge(progress = progress)
            }

            Spacer(modifier = Modifier.height(42.dp))

            ReviewProgressBar(progress = progress, dark = true)
        }
    }
}

@Composable
fun ReviewProgressSmallCard(
    title: String,
    progress: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.tertiary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                ProgressBadge(progress = progress)
            }

            Spacer(modifier = Modifier.height(22.dp))

            ReviewProgressBar(progress = progress, dark = false)
        }
    }
}

@Composable
fun ProgressBadge(progress: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF3A347F))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${progress.coerceIn(0, 100)}%",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@Composable
fun ReviewProgressBar(
    progress: Int,
    dark: Boolean
) {
    val progressFraction = progress.coerceIn(0, 100) / 100f

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(RoundedCornerShape(50))
                .background(if (dark) Color.White.copy(alpha = 0.9f) else Color.LightGray)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progressFraction)
                    .height(9.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.tertiary)
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "0%",
                style = MaterialTheme.typography.labelSmall,
                color = if (dark) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "100%",
                style = MaterialTheme.typography.labelSmall,
                color = if (dark) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun AddReviewCard(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp),
        value = value,
        onValueChange = onValueChange,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "▤",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.size(8.dp))

                Text(
                    text = "Add review",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        shape = RoundedCornerShape(8.dp)
    )
}