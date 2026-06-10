package com.example.forgeplan.core.notifications

import com.example.forgeplan.core.language.AppLanguage
import com.example.forgeplan.core.model.NotificationPayload
import com.example.forgeplan.core.repository.NotificationRepository
import com.example.forgeplan.core.repository.TaskAssignmentRepository

/**
 * NotificationHelper
 *
 * Utilitário para criar notificações in-app nos momentos certos.
 * Deve ser chamado depois de cada operação relevante no backend.
 *
 * Uso típico:
 *   // Após inserir comentário:
 *   NotificationHelper.onCommentAdded(taskId, projectId, authorName, commentText)
 *
 *   // Após mudar estado de tarefa:
 *   NotificationHelper.onTaskStatusChanged(taskId, projectId, newStatus)
 *
 *   // Após criar nova tarefa e atribuir ao utilizador:
 *   NotificationHelper.onTaskAssigned(taskId, projectId, assignedUserId, taskTitle)
 */
object NotificationHelper {

    private val repo = NotificationRepository()
    private val assignmentRepo = TaskAssignmentRepository()

    // ── Novo comentário ───────────────────────────────────────────────────────

    /**
     * Notifica todos os utilizadores atribuídos à tarefa (exceto o autor)
     * que foi adicionado um novo comentário.
     */
    suspend fun onCommentAdded(
        taskId: Long,
        projectId: Long,
        authorUserId: Long,
        authorName: String,
        commentText: String
    ) {
        val assignedUsers = assignmentRepo.getUserIdsForTask(taskId)
        val isPt = AppLanguage.isPortuguese()

        assignedUsers
            .filter { it != authorUserId }
            .forEach { userId ->
                repo.createNotification(
                    NotificationPayload(
                        user_id   = userId,
                        task_id   = taskId,
                        project_id = projectId,
                        type      = "comment",
                        title     = if (isPt)
                            "$authorName mencionou-te num comentário"
                        else
                            "$authorName mentioned you in a comment",
                        message   = commentText.take(120)
                    )
                )
            }
    }

    // ── Mudança de estado ─────────────────────────────────────────────────────

    /**
     * Notifica todos os utilizadores atribuídos à tarefa sobre
     * a alteração de estado (ex: TODO → IN_PROGRESS → DONE).
     */
    suspend fun onTaskStatusChanged(
        taskId: Long,
        projectId: Long,
        taskTitle: String,
        newStatus: String,
        changedByUserId: Long
    ) {
        val assignedUsers = assignmentRepo.getUserIdsForTask(taskId)
        val isPt = AppLanguage.isPortuguese()

        val statusLabel = when (newStatus.uppercase()) {
            "TODO"        -> if (isPt) "A fazer"        else "To Do"
            "IN_PROGRESS" -> if (isPt) "Em progresso"   else "In Progress"
            "DONE"        -> if (isPt) "Concluído"       else "Done"
            "BLOCKED"     -> if (isPt) "Bloqueado"       else "Blocked"
            else          -> newStatus
        }

        assignedUsers
            .filter { it != changedByUserId }
            .forEach { userId ->
                repo.createNotification(
                    NotificationPayload(
                        user_id    = userId,
                        task_id    = taskId,
                        project_id = projectId,
                        type       = "status_change",
                        title      = if (isPt)
                            "Estado de \"$taskTitle\" alterado para $statusLabel"
                        else
                            "\"$taskTitle\" status changed to $statusLabel",
                        message    = null
                    )
                )
            }
    }

    // ── Atualização de progresso / completion_rate ────────────────────────────

    /**
     * Notifica a alteração da taxa de conclusão de uma tarefa.
     */
    suspend fun onCompletionRateUpdated(
        taskId: Long,
        projectId: Long,
        taskTitle: String,
        newRate: Int,
        changedByUserId: Long
    ) {
        val assignedUsers = assignmentRepo.getUserIdsForTask(taskId)
        val isPt = AppLanguage.isPortuguese()

        assignedUsers
            .filter { it != changedByUserId }
            .forEach { userId ->
                repo.createNotification(
                    NotificationPayload(
                        user_id    = userId,
                        task_id    = taskId,
                        project_id = projectId,
                        type       = "completion",
                        title      = if (isPt)
                            "Progresso de \"$taskTitle\" atualizado para $newRate%"
                        else
                            "\"$taskTitle\" progress updated to $newRate%",
                        message    = null
                    )
                )
            }
    }

    // ── Nova tarefa atribuída ─────────────────────────────────────────────────

    /**
     * Notifica um utilizador que lhe foi atribuída uma nova tarefa.
     */
    suspend fun onTaskAssigned(
        taskId: Long,
        projectId: Long,
        assignedUserId: Long,
        taskTitle: String,
        priority: String?
    ) {
        val isPt = AppLanguage.isPortuguese()
        val priorityLabel = priority?.let {
            if (isPt) "Prioridade: $it" else "Priority: $it"
        } ?: ""

        repo.createNotification(
            NotificationPayload(
                user_id    = assignedUserId,
                task_id    = taskId,
                project_id = projectId,
                type       = "new_task",
                title      = if (isPt)
                    "Nova tarefa atribuída: $taskTitle"
                else
                    "New task assigned: $taskTitle",
                message    = priorityLabel.ifBlank { null }
            )
        )
    }

    // ── Prazo a aproximar-se ──────────────────────────────────────────────────

    /**
     * Deve ser chamado por um Worker/AlarmManager que verifique prazos diariamente.
     * Notifica utilizadores cujas tarefas têm prazo nas próximas 24h.
     */
    suspend fun onDeadlineApproaching(
        taskId: Long,
        projectId: Long,
        taskTitle: String,
        assignedUserId: Long,
        deadlineDate: String
    ) {
        val isPt = AppLanguage.isPortuguese()

        repo.createNotification(
            NotificationPayload(
                user_id    = assignedUserId,
                task_id    = taskId,
                project_id = projectId,
                type       = "deadline",
                title      = if (isPt)
                    "Prazo a aproximar-se: $taskTitle"
                else
                    "Deadline approaching: $taskTitle",
                message    = if (isPt)
                    "A tarefa deve ser concluída até $deadlineDate"
                else
                    "Task must be completed by $deadlineDate"
            )
        )
    }
}