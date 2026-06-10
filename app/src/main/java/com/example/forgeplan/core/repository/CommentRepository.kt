package com.example.forgeplan.core.repository

import com.example.forgeplan.core.model.Comment
import com.example.forgeplan.core.network.SupabaseApi
import com.example.forgeplan.core.network.SupabaseService.CommentRequest
import com.example.forgeplan.core.notifications.NotificationHelper
import com.example.forgeplan.core.session.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CommentRepository {

    private val taskRepo = TaskRepository()

    /**
     * Insere um comentário e, em caso de sucesso, notifica todos os
     * utilizadores atribuídos à tarefa e o criador da tarefa (exceto o autor).
     */
    suspend fun insertComment(taskId: Long, userId: Long, content: String): Comment? {
        val result = SupabaseApi.service.insertComment(
            CommentRequest(task_id = taskId, user_id = userId, content = content)
        )
        val inserted = result.firstOrNull()

        // ── NOTIFICAÇÃO ──────────────────────────────────────────────────────
        if (inserted != null) {
            val authorName = SessionManager.currentUser?.name ?: "Alguém"

            // Vai buscar o project_id e created_by_id reais da tarefa
            val task = taskRepo.getTaskByIdSuspend(taskId)
            val realProjectId = task?.project_id ?: 0L
            val taskCreatorId = task?.created_by_id

            NotificationHelper.onCommentAdded(
                taskId        = taskId,
                projectId     = realProjectId,
                authorUserId  = userId,
                authorName    = authorName,
                commentText   = content,
                taskCreatorId = taskCreatorId
            )
        }
        // ─────────────────────────────────────────────────────────────────────

        return inserted
    }

    fun getCommentsByTaskId(
        taskId: Long,
        onSuccess: (List<Comment>) -> Unit,
        onError: (String) -> Unit
    ) {
        SupabaseApi.service.getCommentsByTaskId("eq.$taskId")
            .enqueue(object : Callback<List<Comment>> {
                override fun onResponse(
                    call: Call<List<Comment>>,
                    response: Response<List<Comment>>
                ) {
                    if (response.isSuccessful) onSuccess(response.body() ?: emptyList())
                    else onError("Erro ao carregar comentários: ${response.code()}")
                }
                override fun onFailure(call: Call<List<Comment>>, t: Throwable) {
                    onError(t.message ?: "Erro desconhecido")
                }
            })
    }
}