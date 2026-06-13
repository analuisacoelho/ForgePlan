package com.example.forgeplan.core.repository

import com.example.forgeplan.ForgePlanApplication
import com.example.forgeplan.core.database.DatabaseProvider
import com.example.forgeplan.core.database.entity.CommentEntity
import com.example.forgeplan.core.model.Comment
import com.example.forgeplan.core.network.NetworkUtils
import com.example.forgeplan.core.network.SupabaseApi
import com.example.forgeplan.core.network.SupabaseService.CommentRequest
import com.example.forgeplan.core.notifications.NotificationHelper
import com.example.forgeplan.core.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CommentRepository {

    private val taskRepo = TaskRepository()
    private val context get() = ForgePlanApplication.instance
    private val db get() = DatabaseProvider.getDatabase(context)

    /**
     * Insere um comentário. Online: envia para o Supabase e notifica.
     * Offline: guarda em Room (is_synced=false), SyncManager envia depois.
     */
    suspend fun insertComment(taskId: Long, userId: Long, content: String): Comment? {
        if (NetworkUtils.isOnline(context)) {
            try {
                val result = SupabaseApi.service.insertComment(
                    CommentRequest(task_id = taskId, user_id = userId, content = content)
                )
                val inserted = result.firstOrNull()

                if (inserted != null) {
                    val authorName = SessionManager.currentUser?.name ?: "Alguém"
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

                return inserted
            } catch (e: Exception) {
                return saveCommentLocally(taskId, userId, content)
            }
        } else {
            return saveCommentLocally(taskId, userId, content)
        }
    }

    private suspend fun saveCommentLocally(taskId: Long, userId: Long, content: String): Comment {
        val entity = CommentEntity(
            task_id = taskId,
            user_id = userId,
            content = content,
            created_at = null,
            is_synced = false
        )
        val localId = db.commentDao().insert(entity)
        return Comment(
            id = localId,
            task_id = taskId,
            user_id = userId,
            content = content,
            created_at = null
        )
    }

    fun getCommentsByTaskId(
        taskId: Long,
        onSuccess: (List<Comment>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (NetworkUtils.isOnline(context)) {
            SupabaseApi.service.getCommentsByTaskId("eq.$taskId")
                .enqueue(object : Callback<List<Comment>> {
                    override fun onResponse(call: Call<List<Comment>>, response: Response<List<Comment>>) {
                        if (response.isSuccessful) onSuccess(response.body() ?: emptyList())
                        else loadLocalComments(taskId, onSuccess)
                    }
                    override fun onFailure(call: Call<List<Comment>>, t: Throwable) {
                        loadLocalComments(taskId, onSuccess)
                    }
                })
        } else {
            loadLocalComments(taskId, onSuccess)
        }
    }

    private fun loadLocalComments(taskId: Long, onSuccess: (List<Comment>) -> Unit) {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            val local = db.commentDao().getCommentsByTaskId(taskId).map {
                Comment(
                    id = it.id,
                    task_id = it.task_id ?: taskId,
                    user_id = it.user_id ?: -1L,
                    content = it.content ?: "",
                    created_at = it.created_at
                )
            }
            withContext(Dispatchers.Main) { onSuccess(local) }
        }
    }
}