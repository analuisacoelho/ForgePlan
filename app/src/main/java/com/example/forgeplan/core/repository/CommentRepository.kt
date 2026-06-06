package com.example.forgeplan.core.repository

import com.example.forgeplan.core.model.Comment
import com.example.forgeplan.core.network.SupabaseApi
import com.example.forgeplan.core.network.SupabaseService.CommentRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CommentRepository {

    suspend fun insertComment(taskId: Long, userId: Long, content: String): Comment? {
        val result = SupabaseApi.service.insertComment(
            CommentRequest(task_id = taskId, user_id = userId, content = content)
        )
        return result.firstOrNull()
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