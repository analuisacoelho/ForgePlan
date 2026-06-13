package com.example.forgeplan.projects.ui

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeplan.core.language.appText
import com.example.forgeplan.core.model.ProjectEvaluation
import com.example.forgeplan.core.model.ProjectEvaluationPayload
import com.example.forgeplan.core.model.User
import com.example.forgeplan.core.ui.components.ForgeAvatar
import com.example.forgeplan.core.ui.components.ForgeMiniChip
import com.example.forgeplan.core.ui.components.ForgePlanTopBar
import com.example.forgeplan.projects.viewmodel.ProjectDetailViewModel
import com.example.forgeplan.projects.viewmodel.ProjectEvaluationViewModel
import com.example.forgeplan.projects.viewmodel.ProjectUserViewModel
import com.example.forgeplan.tasks.viewmodel.TaskViewModel
import com.example.forgeplan.tasks.viewmodel.UserViewModel

// ── Critérios de avaliação ──────────────────────────────────────────────
data class UserRatings(
    val deadlines: Int = 0,      // Cumprimento de prazos
    val completion: Int = 0,     // Taxa de conclusão
    val timeSpent: Int = 0       // Tempo despendido / dedicação
) {
    val average: Int
        get() = if (deadlines > 0 && completion > 0 && timeSpent > 0)
            ((deadlines + completion + timeSpent) / 3.0).toInt().coerceIn(1, 5)
        else 0

    val allFilled: Boolean
        get() = deadlines in 1..5 && completion in 1..5 && timeSpent in 1..5
}

@Composable
fun ProjectReviewScreen(
    projectId: Long,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    projectViewModel: ProjectDetailViewModel = viewModel(),
    taskViewModel: TaskViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel(),
    projectUserViewModel: ProjectUserViewModel = viewModel(),
    evaluationViewModel: ProjectEvaluationViewModel = viewModel()
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val project by projectViewModel.project.collectAsState()
    val isLoading by projectViewModel.isLoading.collectAsState()
    val error by projectViewModel.error.collectAsState()

    val tasks by taskViewModel.tasks.collectAsState()
    val users by userViewModel.users.collectAsState()
    val projectUsers by projectUserViewModel.projectUsers.collectAsState()

    val evaluations by evaluationViewModel.evaluations.collectAsState()
    val evaluationIsLoading by evaluationViewModel.isLoading.collectAsState()
    val evaluationError by evaluationViewModel.error.collectAsState()

    // ratings[userId] = UserRatings com os 3 critérios
    val ratings = remember { mutableStateMapOf<Long, UserRatings>() }
    val comments = remember { mutableStateMapOf<Long, String>() }

    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(projectId) {
        projectViewModel.loadProject(projectId)
        taskViewModel.loadTasks(projectId)
        userViewModel.loadUsers()
        projectUserViewModel.loadProjectUsers(projectId)
        evaluationViewModel.loadEvaluations(projectId)
    }

    val assignedUserIds = projectUsers.map { it.user_id }
    val assignedUsers = users.filter { assignedUserIds.contains(it.id) }

    // ── FIX: só utilizadores com role USER (sem managers) ──────────────
    val reviewUsers = assignedUsers.filter { it.role.uppercase() == "USER" }

    val completedTasks = tasks.count { it.status?.uppercase() == "DONE" }
    val completionRate =
        if (tasks.isEmpty()) 0
        else ((completedTasks.toFloat() / tasks.size.toFloat()) * 100).toInt()

    val isProjectCompleted =
        project?.status?.uppercase() == "DONE" || completionRate >= 100

    fun saveReview() {
        if (!isProjectCompleted) {
            message = appText(
                en = "This project must be completed before evaluation.",
                pt = "O projeto tem de estar concluído antes da avaliação."
            )
            return
        }

        if (reviewUsers.isEmpty()) {
            message = appText(
                en = "There are no team members to evaluate.",
                pt = "Não existem membros da equipa para avaliar."
            )
            return
        }

        // Verifica se todos os critérios estão preenchidos para todos os users
        val allFilled = reviewUsers.all { user ->
            ratings[user.id]?.allFilled == true
        }

        if (!allFilled) {
            message = appText(
                en = "Please rate all criteria for all team members before saving.",
                pt = "Avalia todos os critérios de todos os membros antes de guardar."
            )
            return
        }

        val totalToSave = reviewUsers.size
        var savedCount = 0

        reviewUsers.forEach { user ->
            val userRatings = ratings[user.id] ?: UserRatings()
            val avg = userRatings.average

            // Guarda no comment os detalhes dos critérios + comentário do gestor
            val criteriaDetail = buildString {
                append("[${appText("Deadlines", "Prazos")}: ${userRatings.deadlines}/5] ")
                append("[${appText("Completion", "Conclusão")}: ${userRatings.completion}/5] ")
                append("[${appText("Dedication", "Dedicação")}: ${userRatings.timeSpent}/5]")
                val userComment = comments[user.id]?.trim()
                if (!userComment.isNullOrBlank()) {
                    append("\n${userComment}")
                }
            }

            evaluationViewModel.createEvaluation(
                evaluation = ProjectEvaluationPayload(
                    project_id = projectId,
                    user_id = user.id,
                    rating = avg,
                    comment = criteriaDetail
                ),
                onSuccess = {
                    savedCount++
                    if (savedCount == totalToSave) {
                        projectViewModel.archiveProject(
                            projectId = projectId,
                            onSuccess = {
                                message = appText(
                                    en = "Evaluation saved. Project marked as completed.",
                                    pt = "Avaliação guardada. Projeto marcado como concluído."
                                )
                                onSaveClick()
                            },
                            onError = {
                                message = appText(
                                    en = "Evaluation saved successfully.",
                                    pt = "Avaliação guardada com sucesso."
                                )
                                onSaveClick()
                            }
                        )
                    }
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ForgePlanTopBar(
            title = appText(en = "Evaluation", pt = "Avaliação"),
            initials = "FP"
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (isLandscape) 42.dp else 18.dp,
                    vertical = 18.dp
                )
        ) {
            when {
                isLoading || evaluationIsLoading -> {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

                error != null -> {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                project == null -> {
                    Text(
                        text = appText(en = "Project not found.", pt = "Projeto não encontrado."),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                evaluations.isNotEmpty() -> {
                    ProjectEvaluationReadOnlyScreen(
                        projectName = project!!.name,
                        evaluations = evaluations,
                        users = users,
                        onBackClick = onBackClick
                    )
                }

                else -> {
                    Text(
                        text = appText(en = "Performance Evaluation", pt = "Avaliação de Performance"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = project!!.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    if (!isProjectCompleted) {
                        ForgeEvaluationWarning()
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Text(
                        text = appText(
                            en = "Rate each team member across 3 criteria (1–5 stars each).",
                            pt = "Avalia cada membro da equipa nos 3 critérios (1–5 estrelas cada)."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    if (reviewUsers.isEmpty()) {
                        Text(
                            text = appText(
                                en = "There are no team members to evaluate.",
                                pt = "Não existem membros da equipa para avaliar."
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    } else if (isLandscape) {
                        reviewUsers.chunked(2).forEach { rowUsers ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                rowUsers.forEach { user ->
                                    UserEvaluationCard(
                                        user = user,
                                        ratings = ratings[user.id] ?: UserRatings(),
                                        comment = comments[user.id].orEmpty(),
                                        onRatingsChange = { ratings[user.id] = it; message = null },
                                        onCommentChange = { comments[user.id] = it; message = null },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowUsers.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    } else {
                        reviewUsers.forEach { user ->
                            UserEvaluationCard(
                                user = user,
                                ratings = ratings[user.id] ?: UserRatings(),
                                comment = comments[user.id].orEmpty(),
                                onRatingsChange = { ratings[user.id] = it; message = null },
                                onCommentChange = { comments[user.id] = it; message = null }
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    message?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = it,
                            color = if (it.contains("success", ignoreCase = true) || it.contains("guardad", ignoreCase = true))
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    evaluationError?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            modifier = Modifier.weight(1f).height(52.dp),
                            onClick = onBackClick,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Text(appText(en = "Cancel", pt = "Cancelar"))
                        }

                        Button(
                            modifier = Modifier.weight(1f).height(52.dp),
                            onClick = { saveReview() },
                            enabled = isProjectCompleted,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text(appText(en = "Submit Evaluation", pt = "Guardar avaliação"))
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

// ── Card de avaliação por utilizador com 3 critérios ────────────────────
@Composable
fun UserEvaluationCard(
    user: User,
    ratings: UserRatings,
    comment: String,
    onRatingsChange: (UserRatings) -> Unit,
    onCommentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header do utilizador ────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ForgeAvatar(
                    initials = evaluationInitials(user),
                    size = 44,
                    backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                // Média ao vivo
                if (ratings.allFilled) {
                    ForgeMiniChip(
                        text = "Ø ${ratings.average}/5",
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(14.dp))

            // ── Critério 1: Cumprimento de prazos ───────────────────────
            CriteriaStarRow(
                label = appText(en = "Deadline Compliance", pt = "Cumprimento de Prazos"),
                description = appText(
                    en = "Did the member complete tasks within the defined deadlines?",
                    pt = "O membro concluiu as tarefas dentro dos prazos definidos?"
                ),
                rating = ratings.deadlines,
                onRatingChange = { onRatingsChange(ratings.copy(deadlines = it)) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── Critério 2: Taxa de conclusão ────────────────────────────
            CriteriaStarRow(
                label = appText(en = "Completion Rate", pt = "Taxa de Conclusão"),
                description = appText(
                    en = "What was the overall quality and completion of assigned tasks?",
                    pt = "Qual foi a taxa de conclusão e qualidade das tarefas atribuídas?"
                ),
                rating = ratings.completion,
                onRatingChange = { onRatingsChange(ratings.copy(completion = it)) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── Critério 3: Tempo despendido / Dedicação ─────────────────
            CriteriaStarRow(
                label = appText(en = "Dedication & Time Spent", pt = "Dedicação e Tempo"),
                description = appText(
                    en = "Did the member invest adequate time and effort in the project?",
                    pt = "O membro investiu tempo e esforço adequados no projeto?"
                ),
                rating = ratings.timeSpent,
                onRatingChange = { onRatingsChange(ratings.copy(timeSpent = it)) }
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(14.dp))

            // ── Comentário livre ─────────────────────────────────────────
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth().height(96.dp),
                value = comment,
                onValueChange = onCommentChange,
                label = { Text(appText(en = "Additional Comments (optional)", pt = "Comentários adicionais (opcional)")) },
                placeholder = {
                    Text(
                        appText(
                            en = "Share any additional feedback...",
                            pt = "Adiciona qualquer feedback extra..."
                        )
                    )
                },
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

// ── Linha de estrelas para um critério ──────────────────────────────────
@Composable
private fun CriteriaStarRow(
    label: String,
    description: String,
    rating: Int,
    onRatingChange: (Int) -> Unit
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(5) { index ->
                val starValue = index + 1
                Text(
                    text = if (starValue <= rating) "★" else "☆",
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (starValue <= rating)
                        MaterialTheme.colorScheme.secondary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                    modifier = Modifier.clickable { onRatingChange(starValue) }
                )
            }
            if (rating > 0) {
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "$rating/5",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }
}

// ── Ecrã read-only quando já existe avaliação guardada ──────────────────
@Composable
fun ProjectEvaluationReadOnlyScreen(
    projectName: String,
    evaluations: List<ProjectEvaluation>,
    users: List<User>,
    onBackClick: () -> Unit
) {
    Text(
        text = appText(en = "Saved Evaluation", pt = "Avaliação Guardada"),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = projectName,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
    )

    Spacer(modifier = Modifier.height(18.dp))

    evaluations.forEach { evaluation ->
        val user = users.firstOrNull { it.id == evaluation.user_id }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = user?.name ?: appText(en = "Team member", pt = "Membro da equipa"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Estrelas da nota média
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(5) { index ->
                        val starValue = index + 1
                        Text(
                            text = if (starValue <= evaluation.rating) "★" else "☆",
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (starValue <= evaluation.rating)
                                MaterialTheme.colorScheme.secondary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                ForgeMiniChip(
                    text = appText(en = "Average: ${evaluation.rating}/5", pt = "Média: ${evaluation.rating}/5"),
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = appText(en = "Feedback", pt = "Feedback"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = evaluation.comment ?: appText(
                        en = "No comments were added.",
                        pt = "Não foram adicionados comentários."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
    }

    Button(
        modifier = Modifier.fillMaxWidth().height(52.dp),
        onClick = onBackClick,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(appText(en = "Back", pt = "Voltar"))
    }
}

@Composable
fun ForgeEvaluationWarning() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = appText(
                en = "This project is not completed yet. Evaluation is only available after completion.",
                pt = "Este projeto ainda não está concluído. A avaliação só fica disponível após a conclusão."
            ),
            modifier = Modifier.padding(14.dp),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun evaluationInitials(user: User): String {
    return user.name
        .split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()
        .ifBlank { user.username.take(2).uppercase() }
}