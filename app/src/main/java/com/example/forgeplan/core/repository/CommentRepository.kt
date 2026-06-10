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

    /**
     * Insere um comentário e, em caso de sucesso, notifica todos os
     * utilizadores atribuídos à tarefa (excepto o autor).
     */
    suspend fun insertComment(taskId: Long, userId: Long, content: String): Comment? {
        val result = SupabaseApi.service.insertComment(
            CommentRequest(task_id = taskId, user_id = userId, content = content)
        )
        val inserted = result.firstOrNull()

        // ── NOTIFICAÇÃO ──────────────────────────────────────────────────────
        if (inserted != null) {
            val authorName = SessionManager.currentUser?.name ?: "Alguém"
            // projectId = 0 pois o Comment não tem project_id; a notificação
            // usa task_id para navegar, por isso project_id não é crítico aqui.
            // Se quiseres, faz uma query extra para obter o project_id da task.
            NotificationHelper.onCommentAdded(
                taskId       = taskId,
                projectId    = 0L,
                authorUserId = userId,
                authorName   = authorName,
                commentText  = content
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