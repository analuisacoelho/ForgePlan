package com.example.forgeplan.tasks.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.forgeplan.core.model.Comment
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.model.TaskLog
import com.example.forgeplan.core.model.TaskPhoto
import com.example.forgeplan.core.network.SupabaseApi
import com.example.forgeplan.core.repository.ActivityLogRepository
import com.example.forgeplan.core.repository.CommentRepository
import com.example.forgeplan.core.repository.TaskLogRepository
import com.example.forgeplan.core.repository.TaskRepository
import com.example.forgeplan.core.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.example.forgeplan.core.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TaskPublicDetailViewModel : ViewModel() {


    private val _logPhotos = MutableStateFlow<Map<Long, String>>(emptyMap())
    val logPhotos: StateFlow<Map<Long, String>> = _logPhotos
    private val taskRepo    = TaskRepository()
    private val taskLogRepo = TaskLogRepository()
    private val commentRepo = CommentRepository()
    private val logRepository = ActivityLogRepository()

    private val _task = MutableStateFlow<Task?>(null)
    val task: StateFlow<Task?> = _task

    private val _logs = MutableStateFlow<List<TaskLog>>(emptyList())
    val logs: StateFlow<List<TaskLog>> = _logs

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    private val _commentResult = MutableStateFlow<String?>(null)
    val commentResult: StateFlow<String?> = _commentResult

    private val userRepo = UserRepository()

    private val _userNames = MutableStateFlow<Map<Long, String>>(emptyMap())
    val userNames: StateFlow<Map<Long, String>> = _userNames

    fun clearResult() { _commentResult.value = null }

    fun load(taskId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {

                val t: Task? = suspendCancellableCoroutine { cont ->
                    taskRepo.getTaskById(
                        taskId = taskId,
                        onSuccess = { cont.resume(it) },
                        onError = { cont.resume(null) }
                    )

                }

                _task.value = t

                loadLogs(taskId)
                loadComments(taskId)

                // NOVO
                loadUsers()


            } catch (e: Exception) {
                Log.e("TaskPublicDetailVM", "load error", e)
            }
            finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadPhotosForLogs(logs: List<TaskLog>) {
        val photoMap = mutableMapOf<Long, String>()
        logs.forEach { log ->
            try {
                val photos = withContext(Dispatchers.IO) {
                    suspendCancellableCoroutine { cont ->
                        SupabaseApi.service.getTaskPhotosByLogId(
                            taskLogId = "eq.${log.id}"
                        ).enqueue(object : retrofit2.Callback<List<TaskPhoto>> {
                            override fun onResponse(call: retrofit2.Call<List<TaskPhoto>>, response: retrofit2.Response<List<TaskPhoto>>) {
                                cont.resume(response.body() ?: emptyList())
                            }
                            override fun onFailure(call: retrofit2.Call<List<TaskPhoto>>, t: Throwable) {
                                cont.resume(emptyList())
                            }
                        })
                    }
                }
                photos.firstOrNull()?.photo_url?.let { url ->
                    photoMap[log.id] = url
                }
            } catch (e: Exception) {
                Log.e("TaskPublicDetailVM", "photo load error for log ${log.id}", e)
            }
        }
        _logPhotos.value = photoMap
    }
    private fun loadLogs(taskId: Long) {
        taskLogRepo.getLogsByTaskId(
            taskId = taskId,
            onSuccess = {
                _logs.value = it

                viewModelScope.launch {
                    loadPhotosForLogs(it)
                }
            },
            onError = {
                _logs.value = emptyList()
            }
        )
    }

    private fun loadUsers() {

        userRepo.getUsers(
            onSuccess = { users ->

                _userNames.value = users.associate {
                    it.id to it.name
                }
            },
            onError = {
                _userNames.value = emptyMap()
            }
        )
    }

    fun loadComments(taskId: Long) {
        commentRepo.getCommentsByTaskId(
            taskId    = taskId,
            onSuccess = { _comments.value = it },
            onError   = { _comments.value = emptyList() }
        )
    }

    fun sendComment(
        taskId: Long,
        content: String,
        successMsg: String,
        errorPrefix: String
    ) {
        val myId = SessionManager.userId
        if (myId == -1L || content.isBlank() || _isSending.value) return

        viewModelScope.launch {
            _isSending.value = true
            try {
                commentRepo.insertComment(
                    taskId = taskId,
                    userId = myId,
                    content = content.trim()
                )

                val tTitle = taskRepo.getTaskTitleById(taskId)
                logRepository.logActivity(
                    action = "Added comment",
                    entityType = "comment",
                    entityId = taskId,
                    detailsEn = "User: ${SessionManager.currentUser?.name} added a comment to task '$tTitle'",
                    detailsPt = "User: ${SessionManager.currentUser?.name} adicionou um comentário à tarefa '$tTitle'"
                )

                loadComments(taskId)
                _commentResult.value = successMsg

            } catch (e: Exception) {
                _commentResult.value = "$errorPrefix: ${e.message}"
            } finally {
                _isSending.value = false
            }
        }
    }
}