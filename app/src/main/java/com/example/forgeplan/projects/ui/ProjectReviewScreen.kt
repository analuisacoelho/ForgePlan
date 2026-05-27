package com.example.forgeplan.projects.ui

import android.content.res.Configuration
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.language.appText
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
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val project by projectViewModel.selectedProject.collectAsState()
    val isLoading by projectViewModel.isLoading.collectAsState()
    val error by projectViewModel.error.collectAsState()
    val tasks by taskViewModel.tasks.collectAsState()
    val evaluationError by evaluationViewModel.error.collectAsState()

    var reviewText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    fun saveReview() {
        evaluationViewModel.createEvaluation(
            evaluation = ProjectEvaluationPayload(
                project_id = projectId,
                rating = 5,
                comment = reviewText.trim().ifBlank { null }
            ),
            onSuccess = {
                reviewText = ""
                message = appText(
                    en = "Review saved successfully.",
                    pt = "Review guardada com sucesso."
                )
                evaluationViewModel.loadEvaluations(projectId)
                onSaveClick()
            }
        )
    }

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
            onBackClick = onBackClick,
            onSaveClick = { saveReview() }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (isLandscape) 32.dp else 22.dp,
                    vertical = if (isLandscape) 14.dp else 22.dp
                )
        ) {
            when {
                isLoading && project == null -> CircularProgressIndicator()

                error != null -> {
                    Text(
                        text = error ?: appText(en = "Unknown error", pt = "Erro desconhecido"),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                project == null -> {
                    Text(
                        text = appText(
                            en = "Project not found.",
                            pt = "Projeto não encontrado."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                else -> {
                    Text(
                        text = appText(
                            en = "Project Review",
                            pt = "Avaliação do Projeto"
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(if (isLandscape) 10.dp else 18.dp))

                    Text(
                        text = project!!.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(if (isLandscape) 12.dp else 18.dp))

                    if (isLandscape) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                ReviewProgressMainCard(
                                    title = appText(en = "Progress", pt = "Progresso"),
                                    progress = averageProgress
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                ReviewProgressSmallCard(
                                    title = appText(
                                        en = "Completion Rate",
                                        pt = "Taxa de Conclusão"
                                    ),
                                    progress = completionRate
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                ReviewProgressSmallCard(
                                    title = appText(
                                        en = "Deadline Adherence",
                                        pt = "Cumprimento de Prazos"
                                    ),
                                    progress = deadlineAdherence
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                AddReviewCard(
                                    value = reviewText,
                                    onValueChange = {
                                        reviewText = it
                                        message = null
                                    }
                                )

                                ReviewMessages(
                                    message = message,
                                    evaluationError = evaluationError
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                ProjectReviewButtons(
                                    onBackClick = onBackClick,
                                    onSaveClick = { saveReview() }
                                )
                            }
                        }
                    } else {
                        ReviewProgressMainCard(
                            title = appText(en = "Progress", pt = "Progresso"),
                            progress = averageProgress
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        ReviewProgressSmallCard(
                            title = appText(
                                en = "Completion Rate",
                                pt = "Taxa de Conclusão"
                            ),
                            progress = completionRate
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        ReviewProgressSmallCard(
                            title = appText(
                                en = "Deadline Adherence",
                                pt = "Cumprimento de Prazos"
                            ),
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

                        ReviewMessages(
                            message = message,
                            evaluationError = evaluationError
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        ProjectReviewButtons(
                            onBackClick = onBackClick,
                            onSaveClick = { saveReview() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewMessages(
    message: String?,
    evaluationError: String?
) {
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
}

@Composable
fun ProjectReviewButtons(
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit
) {
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
                text = appText(en = "Cancel", pt = "Cancelar"),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Button(
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            onClick = onSaveClick,
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
                text = appText(en = "Save changes", pt = "Guardar"),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ProjectReviewTopBar(
    title: String,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit
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
            contentDescription = appText(en = "Back", pt = "Voltar"),
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
            contentDescription = appText(en = "Save", pt = "Guardar"),
            tint = MaterialTheme.colorScheme.onTertiary,
            modifier = Modifier
                .size(34.dp)
                .clickable { onSaveClick() }
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

                ReviewProgressBadge(progress = progress)
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
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                ReviewProgressBadge(progress = progress)
            }

            Spacer(modifier = Modifier.height(22.dp))

            ReviewProgressBar(progress = progress, dark = false)
        }
    }
}

@Composable
fun ReviewProgressBadge(progress: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.tertiary)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${progress.coerceIn(0, 100)}%",
            color = MaterialTheme.colorScheme.onTertiary,
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

    val backgroundColor =
        if (dark) {
            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
        }

    val labelColor =
        if (dark) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(RoundedCornerShape(50))
                .background(backgroundColor)
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
                color = labelColor,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "100%",
                style = MaterialTheme.typography.labelSmall,
                color = labelColor
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
                    text = appText(en = "Add review", pt = "Adicionar review"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        placeholder = {
            Text(
                text = appText(
                    en = "Write your project review here.",
                    pt = "Escreve aqui a avaliação do projeto."
                )
            )
        },
        shape = RoundedCornerShape(8.dp)
    )
}