package com.example.forgeplan.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.forgeplan.core.model.TaskAssignment
import com.example.forgeplan.core.notifications.NotificationHelper
import com.example.forgeplan.core.repository.TaskAssignmentRepository
import com.example.forgeplan.core.repository.TaskRepository
import com.example.forgeplan.core.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class TaskAssignmentViewModel : ViewModel() {

    private val repository  = TaskAssignmentRepository()
    private val taskRepo    = TaskRepository()

    private val _assignments = MutableStateFlow<List<TaskAssignment>>(emptyList())
    val assignments: StateFlow<List<TaskAssignment>> = _assignments

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadAssignments(taskId: Long) {
        _isLoading.value = true
        _error.value = null

        repository.getAssignmentsByTaskId(
            taskId = taskId,
            onSuccess = { assignmentList ->
                _assignments.value = assignmentList
                _isLoading.value = false
            },
            onError = { message ->
                _error.value = message
                _isLoading.value = false
            }
        )
    }

    /**
     * Atribui utilizador à tarefa e envia notificação in-app ao utilizador atribuído.
     *
     * @param taskTitle  título da tarefa (para a mensagem de notificação).
     *                   Se null, tenta carregar da API.
     * @param projectId  id do projecto (para a notificação).
     * @param priority   prioridade da tarefa (opcional, aparece na notificação).
     */
    fun assignUserToTask(
        taskId: Long,
        userId: Long,
        taskTitle: String? = null,
        projectId: Long? = null,
        priority: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        _isLoading.value = true
        _error.value = null

        val assignment = TaskAssignment(
            task_id     = taskId,
            user_id     = userId,
            assigned_at = null
        )

        repository.assignUserToTask(
            assignment = assignment,
            onSuccess = {
                loadAssignments(taskId)
                _isLoading.value = false

                // ── NOTIFICAÇÃO ───────────────────────────────────────────────
                // Só notifica se não for o próprio utilizador a atribuir-se
                if (userId != SessionManager.userId) {
                    viewModelScope.launch {
                        val title = taskTitle ?: fetchTaskTitle(taskId)
                        val pid   = projectId ?: fetchProjectId(taskId)
                        NotificationHelper.onTaskAssigned(
                            taskId         = taskId,
                            projectId      = pid,
                            assignedUserId = userId,
                            taskTitle      = title,
                            priority       = priority
                        )
                    }
                }
                // ─────────────────────────────────────────────────────────────

                onSuccess()
            },
            onError = { message ->
                _error.value = message
                _isLoading.value = false
            }
        )
    }

    fun removeUserFromTask(
        taskId: Long,
        userId: Long,
        onSuccess: () -> Unit = {}
    ) {
        _isLoading.value = true
        _error.value = null

        repository.removeUserFromTask(
            taskId = taskId,
            userId = userId,
            onSuccess = {
                loadAssignments(taskId)
                _isLoading.value = false
                onSuccess()
            },
            onError = { message ->
                _error.value = message
                _isLoading.value = false
            }
        )
    }

    fun toggleUserAssignment(
        taskId: Long,
        userId: Long,
        isCurrentlyAssigned: Boolean,
        taskTitle: String? = null,
        projectId: Long? = null,
        priority: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        if (isCurrentlyAssigned) {
            removeUserFromTask(taskId = taskId, userId = userId, onSuccess = onSuccess)
        } else {
            assignUserToTask(
                taskId    = taskId,
                userId    = userId,
                taskTitle = taskTitle,
                projectId = projectId,
                priority  = priority,
                onSuccess = onSuccess
            )
        }
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    /** Tenta obter o título da tarefa da API. Devolve "—" em caso de falha. */
    private suspend fun fetchTaskTitle(taskId: Long): String {
        return try {
            suspendCancellableCoroutine { cont ->
                taskRepo.getTaskById(
                    taskId    = taskId,
                    onSuccess = { task -> cont.resume(task?.title ?: "—") },
                    onError   = { cont.resume("—") }
                )
            }
        } catch (e: Exception) { "—" }
    }

    /** Tenta obter o project_id da tarefa da API. Devolve 0 em caso de falha. */
    private suspend fun fetchProjectId(taskId: Long): Long {
        return try {
            suspendCancellableCoroutine { cont ->
                taskRepo.getTaskById(
                    taskId    = taskId,
                    onSuccess = { task -> cont.resume(task?.project_id ?: 0L) },
                    onError   = { cont.resume(0L) }
                )
            }
        } catch (e: Exception) { 0L }
    }
}