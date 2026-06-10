package com.example.forgeplan.core.model

/**
 * Modelo de Notificação.
 *
 * types usados:
 *  - "new_task"        → nova tarefa atribuída
 *  - "deadline"        → prazo a aproximar-se
 *  - "status_change"   → estado da tarefa alterado
 *  - "comment"         → novo comentário numa tarefa do utilizador
 *  - "completion"      → taxa de conclusão atualizada
 */
data class Notification(
    val id: Long,
    val user_id: Long?,
    val task_id: Long?,
    val project_id: Long?,
    val type: String?,       // new_task | deadline | status_change | comment | completion
    val title: String?,
    val message: String?,
    val is_read: Boolean,
    val created_at: String?
)

data class NotificationPayload(
    val user_id: Long,
    val task_id: Long?,
    val project_id: Long?,
    val type: String,
    val title: String,
    val message: String?,
    val is_read: Boolean = false
)