package com.example.forgeplan.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.forgeplan.core.model.Comment
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.model.TaskLog
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

class TaskPublicDetailViewModel : ViewModel() {


    private val taskRepo    = TaskRepository()
    private val taskLogRepo = TaskLogRepository()
    private val commentRepo = CommentRepository()

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

            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadLogs(taskId: Long) {
        taskLogRepo.getLogsByTaskId(
            taskId    = taskId,
            onSuccess = { _logs.value = it },
            onError   = { _logs.value = emptyList() }
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
        if (myId == -1L || content.isBlank()) return

        viewModelScope.launch {
            _isSending.value = true
            try {
                commentRepo.insertComment(
                    taskId = taskId,
                    userId = myId,
                    content = content.trim()
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