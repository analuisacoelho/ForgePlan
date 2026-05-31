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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.input.KeyboardType
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

    val ratings = remember { mutableStateMapOf<Long, Int>() }
    val comments = remember { mutableStateMapOf<Long, String>() }

    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(projectId) {
        projectViewModel.loadProject(projectId)
        taskViewModel.loadTasks(projectId)
        userViewModel.loadUsers()
        projectUserViewModel.loadProjectUsers(projectId)
        evaluationViewModel.loadEvaluations(projectId)
    }

    val existingEvaluation = evaluations.firstOrNull()

    val assignedUserIds = projectUsers.map { it.user_id }
    val assignedUsers = users.filter { assignedUserIds.contains(it.id) }

    val reviewUsers =
        if (assignedUsers.isNotEmpty()) assignedUsers
        else users.take(4)

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

        val validRatings = ratings.values.filter { it in 1..5 }

        if (reviewUsers.isNotEmpty() && validRatings.size < reviewUsers.size) {
            message = appText(
                en = "Please rate all team members before saving.",
                pt = "Avalia todos os membros da equipa antes de guardar."
            )
            return
        }

        val averageRating =
            if (validRatings.isEmpty()) 5
            else validRatings.average().toInt().coerceIn(1, 5)

        val combinedComment = reviewUsers.joinToString("\n") { user ->
            val rating = ratings[user.id] ?: 0
            val comment = comments[user.id].orEmpty().ifBlank {
                appText(en = "No comment.", pt = "Sem comentário.")
            }

            "${user.name}: $rating/5 - $comment"
        }

        evaluationViewModel.createEvaluation(
            evaluation = ProjectEvaluationPayload(
                project_id = projectId,
                rating = averageRating,
                comment = combinedComment.ifBlank { null }
            ),
            onSuccess = {
                message = appText(
                    en = "Evaluation saved successfully.",
                    pt = "Avaliação guardada com sucesso."
                )
                onSaveClick()
            }
        )
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
                    vertical = if (isLandscape) 18.dp else 18.dp
                )
        ) {
            when {
                isLoading || evaluationIsLoading -> {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

                error != null -> {
                    Text(
                        text = error ?: appText(en = "Unknown error", pt = "Erro desconhecido"),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                project == null -> {
                    Text(
                        text = appText(en = "Project not found.", pt = "Projeto não encontrado."),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                existingEvaluation != null -> {
                    ProjectEvaluationReadOnlyScreen(
                        projectName = project!!.name,
                        evaluation = existingEvaluation,
                        onBackClick = onBackClick
                    )
                }

                else -> {
                    Text(
                        text = appText(
                            en = "Performance Evaluation",
                            pt = "Avaliação de Performance"
                        ),
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
                            en = "Please rate each team member's performance on this project and provide feedback.",
                            pt = "Avalia a performance de cada membro da equipa neste projeto e adiciona comentários."
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
                                        taskCount = tasks.size,
                                        hoursWorked = "${tasks.size * 3}h",
                                        rating = ratings[user.id] ?: 0,
                                        comment = comments[user.id].orEmpty(),
                                        onRatingChange = {
                                            ratings[user.id] = it.coerceIn(1, 5)
                                            message = null
                                        },
                                        onCommentChange = {
                                            comments[user.id] = it
                                            message = null
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                if (rowUsers.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    } else {
                        reviewUsers.forEach { user ->
                            UserEvaluationCard(
                                user = user,
                                taskCount = tasks.size,
                                hoursWorked = "${tasks.size * 3}h",
                                rating = ratings[user.id] ?: 0,
                                comment = comments[user.id].orEmpty(),
                                onRatingChange = {
                                    ratings[user.id] = it.coerceIn(1, 5)
                                    message = null
                                },
                                onCommentChange = {
                                    comments[user.id] = it
                                    message = null
                                }
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    message?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = it,
                            color = if (isProjectCompleted) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
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

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            onClick = onBackClick,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(appText(en = "Cancel", pt = "Cancelar"))
                        }

                        Button(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
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

@Composable
fun ProjectEvaluationReadOnlyScreen(
    projectName: String,
    evaluation: ProjectEvaluation,
    onBackClick: () -> Unit
) {
    Text(
        text = appText(
            en = "Saved Evaluation",
            pt = "Avaliação Guardada"
        ),
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = appText(en = "Final rating", pt = "Classificação final"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(5) { index ->
                    val starValue = index + 1

                    Text(
                        text = if (starValue <= evaluation.rating) "★" else "☆",
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (starValue <= evaluation.rating) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            ForgeMiniChip(
                text = "${evaluation.rating}/5",
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = appText(en = "Manager feedback", pt = "Feedback do gestor"),
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

            evaluation.created_at?.let {
                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = appText(
                        en = "Created at: $it",
                        pt = "Criada em: $it"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(22.dp))

    Button(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
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

@Composable
fun UserEvaluationCard(
    user: User,
    taskCount: Int,
    hoursWorked: String,
    rating: Int,
    comment: String,
    onRatingChange: (Int) -> Unit,
    onCommentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                ForgeAvatar(
                    initials = evaluationInitials(user),
                    size = 48,
                    backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Spacer(modifier = Modifier.size(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = user.role ?: appText(en = "Team member", pt = "Membro da equipa"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = appText(en = "$taskCount tasks", pt = "$taskCount tarefas"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = appText(en = "$hoursWorked worked", pt = "$hoursWorked trabalhadas"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = appText(en = "Performance Rating", pt = "Avaliação de performance"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(5) { index ->
                    val starValue = index + 1

                    Text(
                        text = if (starValue <= rating) "★" else "☆",
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (starValue <= rating) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        },
                        modifier = Modifier.clickable { onRatingChange(starValue) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = if (rating == 0) "" else rating.toString(),
                onValueChange = { value ->
                    val number = value.toIntOrNull()
                    if (number != null) onRatingChange(number.coerceIn(1, 5))
                },
                label = {
                    Text(appText(en = "Or enter rating (1-5)", pt = "Ou introduz avaliação (1-5)"))
                },
                placeholder = { Text("1-5") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                value = comment,
                onValueChange = onCommentChange,
                label = {
                    Text(appText(en = "Manager Comments", pt = "Comentários do gestor"))
                },
                placeholder = {
                    Text(
                        appText(
                            en = "Share your feedback about this team member's performance...",
                            pt = "Escreve o teu feedback sobre a performance deste membro..."
                        )
                    )
                },
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (rating > 0) {
                ForgeMiniChip(
                    text = appText(en = "$rating/5 selected", pt = "$rating/5 selecionado"),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
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