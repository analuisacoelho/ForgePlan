package com.example.forgeplan.tasks.viewmodel

import androidx.lifecycle.ViewModel
import com.example.forgeplan.core.model.TaskDependency
import com.example.forgeplan.core.repository.TaskDependencyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TaskDependencyViewModel : ViewModel() {

    private val repository = TaskDependencyRepository()

    private val _dependencies = MutableStateFlow<List<TaskDependency>>(emptyList())
    val dependencies: StateFlow<List<TaskDependency>> = _dependencies

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadDependencies(taskId: Long) {
        _isLoading.value = true
        _error.value = null

        repository.getDependencies(
            taskId = taskId,
            onSuccess = { result ->
                _dependencies.value = result
                _isLoading.value = false
            },
            onError = { message ->
                _error.value = message
                _isLoading.value = false
            }
        )
    }

    fun createDependency(
        taskId: Long,
        dependsOnTaskId: Long,
        onSuccess: () -> Unit
    ) {
        _isLoading.value = true
        _error.value = null

        val dependency = TaskDependency(
            task_id = taskId,
            depends_on_task_id = dependsOnTaskId
        )

        repository.createDependency(
            dependency = dependency,
            onSuccess = {
                loadDependencies(taskId)
                _isLoading.value = false
                onSuccess()
            },
            onError = { message ->
                _error.value = message
                _isLoading.value = false
            }
        )
    }
}