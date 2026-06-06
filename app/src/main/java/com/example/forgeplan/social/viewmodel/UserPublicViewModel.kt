package com.example.forgeplan.social.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.forgeplan.core.model.Comment
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.model.TaskLog
import com.example.forgeplan.core.model.User
import com.example.forgeplan.core.repository.CommentRepository
import com.example.forgeplan.core.repository.TaskAssignmentRepository
import com.example.forgeplan.core.repository.TaskLogRepository
import com.example.forgeplan.core.repository.TaskRepository
import com.example.forgeplan.core.repository.UserRepository
import com.example.forgeplan.core.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class UserWithTasks(
    val user: User,
    val tasks: List<Task>
)

class UserPublicViewModel : ViewModel() {

    private val userRepo       = UserRepository()
    private val taskRepo       = TaskRepository()
    private val taskAssignRepo = TaskAssignmentRepository()
    private val taskLogRepo    = TaskLogRepository()
    private val commentRepo    = CommentRepository()

    // ── Utilizadores + tarefas ────────────────────────────────────────────────
    private val _usersWithTasks = MutableStateFlow<List<UserWithTasks>>(emptyList())
    val usersWithTasks: StateFlow<List<UserWithTasks>> = _usersWithTasks

    // ── Logs da tarefa selecionada ────────────────────────────────────────────
    private val _taskLogs = MutableStateFlow<List<TaskLog>>(emptyList())
    val taskLogs: StateFlow<List<TaskLog>> = _taskLogs

    // ── Comentários da tarefa selecionada ─────────────────────────────────────
    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments

    // ── Tarefa atualmente selecionada ─────────────────────────────────────────
    private val _selectedTask = MutableStateFlow<Task?>(null)
    val selectedTask: StateFlow<Task?> = _selectedTask

    // ── Estado da UI ──────────────────────────────────────────────────────────
    private val _isLoading    = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSendingComment = MutableStateFlow(false)
    val isSendingComment: StateFlow<Boolean> = _isSendingComment

    private val _commentResult = MutableStateFlow<String?>(null)
    val commentResult: StateFlow<String?> = _commentResult

    fun clearCommentResult() { _commentResult.value = null }

    // ── Carrega todos os utilizadores (exceto o próprio) com as tarefas ───────
    fun loadUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val myId    = SessionManager.userId
                val allUsers: List<User> = suspendCancellableCoroutine { cont ->
                    userRepo.getUsers(
                        onSuccess = { cont.resume(it) },
                        onError   = { cont.resume(emptyList()) }
                    )
                }
                // Exclui o próprio utilizador e utilizadores inactivos
                val others = allUsers.filter { it.id != myId && it.is_active == true }

                val result = mutableListOf<UserWithTasks>()
                for (user in others) {
                    val assignedTaskIds: List<Long> = suspendCancellableCoroutine { cont ->
                        taskAssignRepo.getTaskIdsByUserId(
                            userId    = user.id,
                            onSuccess = { cont.resume(it) },
                            onError   = { cont.resume(emptyList()) }
                        )
                    }
                    if (assignedTaskIds.isNotEmpty()) {
                        // Obtém as tarefas IN_PROGRESS ou Done para mostrar progresso
                        val tasks = mutableListOf<Task>()
                        for (taskId in assignedTaskIds) {
                            val task: Task? = suspendCancellableCoroutine { cont ->
                                taskRepo.getTaskById(
                                    taskId    = taskId,
                                    onSuccess = { cont.resume(it) },
                                    onError   = { cont.resume(null) }
                                )
                            }
                            task?.let { tasks.add(it) }
                        }
                        if (tasks.isNotEmpty()) result.add(UserWithTasks(user, tasks))
                    }
                }
                _usersWithTasks.value = result
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── Seleciona uma tarefa e carrega os seus logs e comentários ─────────────
    fun selectTask(task: Task) {
        _selectedTask.value = task
        loadTaskLogs(task.id)
        loadComments(task.id)
    }

    private fun loadTaskLogs(taskId: Long) {
        taskLogRepo.getLogsByTaskId(
            taskId    = taskId,
            onSuccess = { _taskLogs.value = it },
            onError   = { _taskLogs.value = emptyList() }
        )
    }

    fun loadComments(taskId: Long) {
        commentRepo.getCommentsByTaskId(
            taskId    = taskId,
            onSuccess = { _comments.value = it },
            onError   = { _comments.value = emptyList() }
        )
    }

    // ── Envia um comentário ───────────────────────────────────────────────────
    fun sendComment(
        taskId: Long,
        content: String,
        successMsg: String,
        errorPrefix: String
    ) {
        val myId = SessionManager.userId
        if (myId == -1L || content.isBlank()) return

        viewModelScope.launch {
            _isSendingComment.value = true
            try {
                commentRepo.insertComment(
                    taskId  = taskId,
                    userId  = myId,
                    content = content.trim()
                )
                loadComments(taskId)
                _commentResult.value = successMsg
            } catch (e: Exception) {
                _commentResult.value = "$errorPrefix: ${e.message}"
            } finally {
                _isSendingComment.value = false
            }
        }
    }
}