package com.example.forgeplan.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.notifications.NotificationHelper
import com.example.forgeplan.core.repository.ActivityLogRepository
import com.example.forgeplan.core.repository.TaskRepository
import com.example.forgeplan.core.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TaskViewModel : ViewModel() {

    private val repository = TaskRepository()
    private val logRepository = ActivityLogRepository()

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks

    private val _selectedTask = MutableStateFlow<Task?>(null)
    val selectedTask: StateFlow<Task?> = _selectedTask

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadTasks(projectId: Long) {
        _isLoading.value = true
        _error.value = null

        repository.getTasksByProjectId(
            projectId = projectId,
            onSuccess = { taskList ->
                _tasks.value = taskList
                _isLoading.value = false
            },
            onError = { message ->
                _error.value = message
                _isLoading.value = false
            }
        )
    }

    fun loadTaskById(taskId: Long) {
        _isLoading.value = true
        _error.value = null

        repository.getTaskById(
            taskId = taskId,
            onSuccess = { task ->
                _selectedTask.value = task
                _isLoading.value = false
            },
            onError = { message ->
                _error.value = message
                _isLoading.value = false
            }
        )
    }

    fun clearSelectedTask() {
        _selectedTask.value = null
    }

    fun createTask(
        task: Task,
        onSuccess: () -> Unit
    ) {
        _isLoading.value = true
        _error.value = null

        repository.createTask(
            task = task,
            onSuccess = { created ->
                _isLoading.value = false
                loadTasks(task.project_id)
                
                created?.let {
                    logRepository.logActivity(
                        action = "Created task",
                        entityType = "task",
                        entityId = it.id,
                        detailsEn = "Manager: ${SessionManager.currentUser?.name} created task '${it.title}'",
                        detailsPt = "Manager: ${SessionManager.currentUser?.name} criou a tarefa '${it.title}'"
                    )
                }
                
                onSuccess()
            },
            onError = { message ->
                _error.value = message
                _isLoading.value = false
            }
        )
    }

    fun createTaskReturning(
        task: Task,
        onSuccess: (Task?) -> Unit
    ) {
        _isLoading.value = true
        _error.value = null

        repository.createTask(
            task = task,
            onSuccess = { createdTask ->
                _isLoading.value = false
                loadTasks(task.project_id)
                
                createdTask?.let {
                    logRepository.logActivity(
                        action = "Created task",
                        entityType = "task",
                        entityId = it.id,
                        detailsEn = "Manager: ${SessionManager.currentUser?.name} created task '${it.title}'",
                        detailsPt = "Manager: ${SessionManager.currentUser?.name} criou a tarefa '${it.title}'"
                    )
                }
                
                onSuccess(createdTask)
            },
            onError = { message ->
                _error.value = message
                _isLoading.value = false
            }
        )
    }

    /**
     * Atualiza a tarefa e dispara notificações de estado/progresso
     * se o status ou completion_rate mudarem.
     *
     * @param previousTask  tarefa com os valores ANTES da edição (para comparar).
     *                      Se null, não dispara notificações de mudança.
     */
    fun updateTask(
        task: Task,
        onSuccess: () -> Unit,
        previousTask: Task? = null
    ) {
        _isLoading.value = true
        _error.value = null

        repository.updateTask(
            task = task,
            onSuccess = { updated ->
                _isLoading.value = false
                loadTasks(task.project_id)

                logRepository.logActivity(
                    action = "Updated task",
                    entityType = "task",
                    entityId = task.id,
                    detailsEn = "Manager: ${SessionManager.currentUser?.name} updated task '${task.title}'",
                    detailsPt = "Manager: ${SessionManager.currentUser?.name} atualizou a tarefa '${task.title}'"
                )

                // ── NOTIFICAÇÕES ─────────────────────────────────────────────
                if (previousTask != null) {
                    val userId = SessionManager.userId
                    viewModelScope.launch {
                        // Mudança de estado
                        val oldStatus = previousTask.status?.uppercase() ?: "PENDING"
                        val newStatus = task.status?.uppercase() ?: "PENDING"
                        if (oldStatus != newStatus) {
                            NotificationHelper.onTaskStatusChanged(
                                taskId          = task.id,
                                projectId       = task.project_id,
                                taskTitle       = task.title,
                                newStatus       = newStatus,
                                changedByUserId = userId
                            )
                        }

                        // Mudança de taxa de conclusão
                        val oldRate = previousTask.completion_rate ?: 0
                        val newRate = task.completion_rate ?: 0
                        if (oldRate != newRate) {
                            NotificationHelper.onCompletionRateUpdated(
                                taskId          = task.id,
                                projectId       = task.project_id,
                                taskTitle       = task.title,
                                newRate         = newRate,
                                changedByUserId = userId
                            )
                        }

                        // Log if completed
                        if ((oldStatus != "DONE") && (newStatus == "DONE")) {
                            logRepository.logActivity(
                                action = "Completed task",
                                entityType = "task",
                                entityId = task.id,
                                detailsEn = "User: ${SessionManager.currentUser?.name} marked task '${task.title}' as completed",
                                detailsPt = "User: ${SessionManager.currentUser?.name} marcou a tarefa '${task.title}' como concluída"
                            )
                        }
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

    fun loadTasksForDashboard(
        projectId: Long,
        onResult: (List<Task>) -> Unit
    ) {
        repository.getTasksByProjectId(
            projectId = projectId,
            onSuccess = { taskList -> onResult(taskList) },
            onError = { onResult(emptyList()) }
        )
    }

    fun markTaskAsDone(
        task: Task,
        onSuccess: () -> Unit
    ) {
        val updatedTask = task.copy(
            status = "DONE",
            completion_rate = 100
        )
        updateTask(
            task = updatedTask,
            onSuccess = onSuccess,
            previousTask = task  // passa a tarefa original para comparar
        )
    }

    fun deleteTask(
        task: Task,
        onSuccess: () -> Unit
    ) {
        if ((task.completion_rate ?: 0) > 0) {
            _error.value = "Não é possível eliminar uma tarefa com progresso."
            return
        }

        _isLoading.value = true
        _error.value = null

        repository.deleteTask(
            taskId = task.id,
            onSuccess = {
                _isLoading.value = false
                loadTasks(task.project_id)

                logRepository.logActivity(
                    action = "Deleted task",
                    entityType = "task",
                    entityId = task.id,
                    detailsEn = "Manager: ${SessionManager.currentUser?.name} deleted task '${task.title}'",
                    detailsPt = "Manager: ${SessionManager.currentUser?.name} eliminou a tarefa '${task.title}'"
                )

                onSuccess()
            },
            onError = { message ->
                _error.value = message
                _isLoading.value = false
            }
        )
    }
}
