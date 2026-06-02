package com.example.forgeplan.tasks.viewmodel

import androidx.lifecycle.ViewModel
import com.example.forgeplan.core.model.Task
import com.example.forgeplan.core.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TaskViewModel : ViewModel() {

    private val repository = TaskRepository()

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
            onSuccess = {
                _isLoading.value = false
                loadTasks(task.project_id)
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
                onSuccess(createdTask)
            },
            onError = { message ->
                _error.value = message
                _isLoading.value = false
            }
        )
    }

    fun updateTask(
        task: Task,
        onSuccess: () -> Unit
    ) {
        _isLoading.value = true
        _error.value = null

        repository.updateTask(
            task = task,
            onSuccess = {
                _isLoading.value = false
                loadTasks(task.project_id)
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
            onSuccess = onSuccess
        )
    }
}